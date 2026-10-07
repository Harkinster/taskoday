from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
import json
import unicodedata

from fastapi import HTTPException, status
from sqlalchemy import and_, case, delete, insert, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_task import (
    FamilyTask,
    FamilyTaskAssignee,
    FamilyTaskOccurrence,
    FamilyTaskOccurrenceContributor,
    FamilyTaskOccurrenceEvent,
    FamilyTaskOccurrenceEventParticipant,
    FamilyTaskOccurrenceStatus,
    FamilyTaskRecurrence,
)
from app.models.user import User, UserRole
from app.services.user_identity_service import display_name_for_user, user_reference_payload
from app.services.reward_engine import grant_for_finalization, revoke_cycle_rewards


WEEKDAY_ALIASES = {
    "1": 1,
    "monday": 1,
    "mon": 1,
    "lundi": 1,
    "2": 2,
    "tuesday": 2,
    "tue": 2,
    "mardi": 2,
    "3": 3,
    "wednesday": 3,
    "wed": 3,
    "mercredi": 3,
    "4": 4,
    "thursday": 4,
    "thu": 4,
    "jeudi": 4,
    "5": 5,
    "friday": 5,
    "fri": 5,
    "vendredi": 5,
    "6": 6,
    "saturday": 6,
    "sat": 6,
    "samedi": 6,
    "7": 7,
    "sunday": 7,
    "sun": 7,
    "dimanche": 7,
}

HOUSE_QUEST_CATEGORY = "TASKODAY_HOUSE_QUEST"
from app.services.action_identity import IDENTITY_TO_CATEGORY, identity_from_category


def occurrence_category(category: str | None) -> str:
    """Resolve the documented legacy house category once, when an occurrence is born."""
    normalized = category.strip().upper() if category is not None else ""
    if normalized in {"", "MAISON"}:
        return HOUSE_QUEST_CATEGORY
    if normalized in IDENTITY_TO_CATEGORY.values():
        return normalized
    raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Categorie d'action inconnue.")


def ensure_family_member(db: Session, *, family_id: int, user: User) -> FamilyMember:
    # Serialize task writes with archival. Reads share this guard so callers of
    # get_task_for_member/get_occurrence_for_member cannot mutate after archive.
    family = db.scalar(select(Family).where(Family.id == family_id).with_for_update())
    if family is None or family.archived_at is not None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    membership = db.scalar(
        select(FamilyMember).where(FamilyMember.family_id == family_id, FamilyMember.user_id == user.id)
    )
    if membership is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    return membership


def ensure_family_parent(db: Session, *, family_id: int, user: User) -> FamilyMember:
    membership = ensure_family_member(db, family_id=family_id, user=user)
    if user.role != UserRole.PARENT or membership.role != FamilyMemberRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    return membership


def normalize_weekdays(values: list[int | str] | None) -> list[int]:
    if not values:
        return []

    weekdays: list[int] = []
    for value in values:
        day: int | None
        if isinstance(value, int):
            day = value
        else:
            normalized = _normalize_weekday(value)
            day = WEEKDAY_ALIASES.get(normalized)

        if day is None or day < 1 or day > 7:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Jour de recurrence invalide.",
            )
        if day not in weekdays:
            weekdays.append(day)

    return sorted(weekdays)


def weekdays_to_storage(values: list[int]) -> str | None:
    return ",".join(str(value) for value in values) if values else None


def parse_weekdays(value: str | None) -> list[int]:
    if not value:
        return []
    return [int(part) for part in value.split(",") if part]


def normalize_due_fields(
    *,
    due_at: datetime | None,
    due_date: date | None,
    due_time: time | None,
) -> tuple[datetime | None, date | None, time | None]:
    if due_at is not None:
        return due_at, due_at.date(), _storage_time(due_at.time())
    if due_date is not None and due_time is not None:
        due_at = datetime.combine(due_date, due_time)
        if due_at.tzinfo is None:
            due_at = due_at.replace(tzinfo=timezone.utc)
        return due_at, due_date, _storage_time(due_time)
    if due_date is not None:
        return None, due_date, None
    return None, None, None


def normalize_due_update(task: FamilyTask, data: dict) -> tuple[datetime | None, date | None, time | None]:
    if "due_at" in data:
        due_at = data["due_at"]
        return normalize_due_fields(due_at=due_at, due_date=None, due_time=None)

    due_date = data["due_date"] if "due_date" in data else task.due_date
    due_time = data["due_time"] if "due_time" in data else task.due_time
    if due_date is None:
        if due_time is not None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="due_date est requis quand due_time est fourni.",
            )
        return None, None, None

    return normalize_due_fields(due_at=None, due_date=due_date, due_time=due_time)


def validate_assignee_user_ids(db: Session, *, family_id: int, assignee_user_ids: list[int]) -> list[int]:
    user_ids = _dedupe_user_ids(assignee_user_ids)
    if not user_ids:
        return []

    existing_ids = set(
        db.scalars(
            select(FamilyMember.user_id).where(
                FamilyMember.family_id == family_id,
                FamilyMember.user_id.in_(user_ids),
            )
        ).all()
    )
    if existing_ids != set(user_ids):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Tous les assignes doivent appartenir a la famille.",
        )
    return user_ids


def replace_task_assignees(db: Session, *, task: FamilyTask, assignee_user_ids: list[int]) -> None:
    db.execute(delete(FamilyTaskAssignee).where(FamilyTaskAssignee.task_id == task.id))
    db.flush()
    for user_id in assignee_user_ids:
        db.add(FamilyTaskAssignee(task_id=task.id, user_id=user_id))


def get_task_for_member(db: Session, *, task_id: int, user: User) -> tuple[FamilyTask, FamilyMember]:
    task = db.get(FamilyTask, task_id)
    if task is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tache familiale introuvable.")
    membership = ensure_family_member(db, family_id=task.family_id, user=user)
    ensure_task_visible(task=task, membership=membership, user=user)
    return task, membership


def get_occurrence_for_member(
    db: Session,
    *,
    occurrence_id: int,
    user: User,
) -> tuple[FamilyTaskOccurrence, FamilyTask, FamilyMember]:
    occurrence = db.get(FamilyTaskOccurrence, occurrence_id)
    if occurrence is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Occurrence introuvable.")
    task = occurrence.task
    membership = ensure_family_member(db, family_id=task.family_id, user=user)
    ensure_task_visible(task=task, membership=membership, user=user)
    return occurrence, task, membership


def task_visible_to_member(*, task: FamilyTask, membership: FamilyMember, user: User) -> bool:
    if user.role == UserRole.PARENT and membership.role == FamilyMemberRole.PARENT:
        return True
    if user.role != UserRole.CHILD or membership.role != FamilyMemberRole.CHILD:
        return False
    if task.scope == "HOUSE":
        return True
    return any(assignee.user_id == user.id for assignee in task.assignees)


def ensure_task_visible(*, task: FamilyTask, membership: FamilyMember, user: User) -> None:
    if not task_visible_to_member(task=task, membership=membership, user=user):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tache familiale introuvable.")


def is_task_scheduled_for_date(task: FamilyTask, target_date: date) -> bool:
    if task.end_date is not None and target_date > task.end_date:
        return False
    if task.recurrence == FamilyTaskRecurrence.NONE and task.due_date is None and task.due_at is None:
        return False

    start_date = _task_start_date(task)
    if target_date < start_date:
        return False

    if task.recurrence == FamilyTaskRecurrence.NONE:
        return target_date == start_date
    if task.recurrence == FamilyTaskRecurrence.DAILY:
        return (target_date - start_date).days % task.recurrence_interval == 0
    if task.recurrence == FamilyTaskRecurrence.WEEKLY:
        return (
            target_date.isoweekday() == start_date.isoweekday()
            and ((target_date - start_date).days // 7) % task.recurrence_interval == 0
        )
    if task.recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS:
        anchor_week_start = start_date - timedelta(days=start_date.weekday())
        target_week_start = target_date - timedelta(days=target_date.weekday())
        weeks_since_anchor = (target_week_start - anchor_week_start).days // 7
        return (
            target_date.isoweekday() in set(parse_weekdays(task.selected_weekdays))
            and weeks_since_anchor % task.recurrence_interval == 0
        )
    return False


def get_or_create_undated_occurrence(db: Session, *, task: FamilyTask) -> FamilyTaskOccurrence:
    occurrence = db.scalar(select(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.task_id == task.id,
        FamilyTaskOccurrence.scheduled_date.is_(None),
    ))
    if occurrence is not None:
        return occurrence
    try:
        with db.begin_nested():
            db.execute(insert(FamilyTaskOccurrence).values(
                task_id=task.id, category=occurrence_category(task.category),
                scope=task.scope, kind=task.kind, scheduled_date=None,
                status=FamilyTaskOccurrenceStatus.TODO,
            ))
    except IntegrityError:
        occurrence = db.scalar(select(FamilyTaskOccurrence).where(
            FamilyTaskOccurrence.task_id == task.id,
            FamilyTaskOccurrence.scheduled_date.is_(None),
        ))
        if occurrence is not None:
            return occurrence
        raise
    occurrence = db.scalar(select(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.task_id == task.id,
        FamilyTaskOccurrence.scheduled_date.is_(None),
    ))
    if occurrence is None:
        raise RuntimeError("Occurrence ouverte creee mais introuvable.")
    return occurrence


def get_or_create_occurrence(db: Session, *, task: FamilyTask, scheduled_date: date) -> FamilyTaskOccurrence:
    occurrence = _find_occurrence(db, task_id=task.id, scheduled_date=scheduled_date)
    if occurrence is not None:
        return occurrence

    try:
        with db.begin_nested():
            db.execute(
                insert(FamilyTaskOccurrence).values(
                    task_id=task.id,
                    category=occurrence_category(task.category),
                    scope=task.scope,
                    kind=task.kind,
                    scheduled_date=scheduled_date,
                    status=FamilyTaskOccurrenceStatus.TODO,
                )
            )
    except IntegrityError:
        occurrence = _find_occurrence(db, task_id=task.id, scheduled_date=scheduled_date)
        if occurrence is not None:
            return occurrence
        raise

    occurrence = _find_occurrence(db, task_id=task.id, scheduled_date=scheduled_date)
    if occurrence is None:
        raise RuntimeError("Occurrence familiale creee mais introuvable.")
    return occurrence


def _find_occurrence(db: Session, *, task_id: int, scheduled_date: date) -> FamilyTaskOccurrence | None:
    return db.scalar(
        select(FamilyTaskOccurrence).where(
            FamilyTaskOccurrence.task_id == task_id,
            FamilyTaskOccurrence.scheduled_date == scheduled_date,
        )
    )


def get_or_create_occurrences_for_date(
    db: Session,
    *,
    tasks: list[FamilyTask],
    scheduled_date: date,
) -> list[FamilyTaskOccurrence]:
    scheduled = [
        get_or_create_occurrence(db, task=task, scheduled_date=scheduled_date)
        for task in tasks
        if is_task_scheduled_for_date(task, scheduled_date)
    ]
    if scheduled_date == date.today():
        scheduled += [
            get_or_create_undated_occurrence(db, task=task)
            for task in tasks
            if task.kind == "MISSION" and task.recurrence == FamilyTaskRecurrence.NONE
            and task.due_date is None and task.due_at is None
        ]
    return scheduled


def complete_occurrence(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    membership: FamilyMember,
    user: User,
) -> FamilyTaskOccurrence:
    _ensure_can_complete(db, task=task, membership=membership, user=user)

    if task.scope == "HOUSE":
        if occurrence.status != FamilyTaskOccurrenceStatus.IN_PROGRESS:
            raise HTTPException(status_code=409, detail="Commencez ou rejoignez cette action avant de la terminer.")
        if db.scalar(select(FamilyTaskOccurrenceContributor.id).where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == occurrence.cycle_number,
            FamilyTaskOccurrenceContributor.user_id == user.id,
        )) is None:
            raise HTTPException(status_code=403, detail="Participez a cette action avant de la terminer.")

    if occurrence.status in {
        FamilyTaskOccurrenceStatus.COMPLETED,
        FamilyTaskOccurrenceStatus.PENDING_VALIDATION,
        FamilyTaskOccurrenceStatus.VALIDATED,
    }:
        return occurrence

    now = datetime.now(timezone.utc)
    old_status = occurrence.status
    new_status = (
        FamilyTaskOccurrenceStatus.PENDING_VALIDATION
        if task.validation_required
        else FamilyTaskOccurrenceStatus.COMPLETED
    )
    # A status-and-cycle compare-and-set guarantees that concurrent taps can
    # create only one completion transition for the shared occurrence.
    result = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == old_status,
        FamilyTaskOccurrence.cycle_number == occurrence.cycle_number,
    ).values(
        status=new_status,
        cycle_number=case((FamilyTaskOccurrence.cycle_number == 0, 1), else_=FamilyTaskOccurrence.cycle_number),
        completed_at=now,
        completed_by_user_id=user.id,
        validated_at=None,
        validated_by_user_id=None,
    ))
    if result.rowcount != 1:
        db.refresh(occurrence)
        if occurrence.status in {
            FamilyTaskOccurrenceStatus.COMPLETED,
            FamilyTaskOccurrenceStatus.PENDING_VALIDATION,
            FamilyTaskOccurrenceStatus.VALIDATED,
        }:
            return occurrence
        raise HTTPException(status_code=409, detail="Cette action a deja ete modifiee.")
    db.refresh(occurrence)
    event = record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="COMPLETE", old_status=old_status,
        actor_user_id=user.id, completed_by_user_id=user.id, occurred_at=now,
    )
    occurrence._reward_points_awarded_to_me = grant_for_finalization(
        db, occurrence=occurrence, task=task, trigger_event=event, actor_user_id=user.id,
    )
    return occurrence


def start_occurrence(
    db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask,
    membership: FamilyMember, user: User,
) -> FamilyTaskOccurrence:
    if task.scope != "HOUSE":
        raise HTTPException(status_code=409, detail="La participation concerne uniquement les actions Maison.")
    if occurrence.status == FamilyTaskOccurrenceStatus.IN_PROGRESS:
        existing = db.scalar(select(FamilyTaskOccurrenceContributor.id).where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == occurrence.cycle_number,
            FamilyTaskOccurrenceContributor.user_id == user.id,
        ))
        if existing is not None:
            return occurrence
        raise HTTPException(status_code=409, detail="Cette action est deja en cours. Rejoignez-la pour participer.")
    if occurrence.status != FamilyTaskOccurrenceStatus.TODO:
        raise HTTPException(status_code=409, detail="Cette action n'est pas ouverte a la participation.")

    result = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == FamilyTaskOccurrenceStatus.TODO,
    ).values(
        status=FamilyTaskOccurrenceStatus.IN_PROGRESS,
        cycle_number=case((FamilyTaskOccurrence.cycle_number == 0, 1), else_=FamilyTaskOccurrence.cycle_number),
    ))
    if result.rowcount != 1:
        db.refresh(occurrence)
        if occurrence.status == FamilyTaskOccurrenceStatus.IN_PROGRESS and db.scalar(select(FamilyTaskOccurrenceContributor.id).where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == occurrence.cycle_number,
            FamilyTaskOccurrenceContributor.user_id == user.id,
        )) is not None:
            return occurrence
        raise HTTPException(status_code=409, detail="Cette action a deja ete modifiee.")

    db.refresh(occurrence)
    now = datetime.now(timezone.utc)
    db.add(FamilyTaskOccurrenceContributor(
        occurrence_id=occurrence.id, cycle_number=occurrence.cycle_number,
        user_id=user.id, joined_at=now,
    ))
    db.flush()
    record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="START",
        old_status=FamilyTaskOccurrenceStatus.TODO, actor_user_id=user.id,
        completed_by_user_id=None, occurred_at=now,
    )
    return occurrence


def join_occurrence(
    db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask,
    membership: FamilyMember, user: User,
) -> FamilyTaskOccurrence:
    if task.scope != "HOUSE":
        raise HTTPException(status_code=409, detail="La participation concerne uniquement les actions Maison.")
    if occurrence.status != FamilyTaskOccurrenceStatus.IN_PROGRESS:
        raise HTTPException(status_code=409, detail="Cette action n'est plus ouverte a la participation.")
    exists = db.scalar(select(FamilyTaskOccurrenceContributor.id).where(
        FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
        FamilyTaskOccurrenceContributor.cycle_number == occurrence.cycle_number,
        FamilyTaskOccurrenceContributor.user_id == user.id,
    ))
    if exists is not None:
        return occurrence
    # Serialize the final participation check against completion. The no-op
    # update acquires the occurrence write lock and fails if it was closed.
    guarded = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == FamilyTaskOccurrenceStatus.IN_PROGRESS,
        FamilyTaskOccurrence.cycle_number == occurrence.cycle_number,
    ).values(cycle_number=occurrence.cycle_number))
    if guarded.rowcount != 1:
        raise HTTPException(status_code=409, detail="Cette action n'est plus ouverte a la participation.")
    now = datetime.now(timezone.utc)
    try:
        with db.begin_nested():
            db.add(FamilyTaskOccurrenceContributor(
                occurrence_id=occurrence.id, cycle_number=occurrence.cycle_number,
                user_id=user.id, joined_at=now,
            ))
            db.flush()
    except IntegrityError:
        # A simultaneous duplicate join is an idempotent success.
        return occurrence
    record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="JOIN",
        old_status=FamilyTaskOccurrenceStatus.IN_PROGRESS, actor_user_id=user.id,
        completed_by_user_id=None, occurred_at=now,
    )
    return occurrence


def validate_occurrence(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    user: User,
) -> FamilyTaskOccurrence:
    ensure_family_parent(db, family_id=task.family_id, user=user)
    if occurrence.status == FamilyTaskOccurrenceStatus.VALIDATED:
        return occurrence
    if occurrence.status != FamilyTaskOccurrenceStatus.PENDING_VALIDATION:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Occurrence non en attente de validation.",
        )

    old_status = occurrence.status
    validated_at = datetime.now(timezone.utc)
    result = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == FamilyTaskOccurrenceStatus.PENDING_VALIDATION,
        FamilyTaskOccurrence.cycle_number == occurrence.cycle_number,
    ).values(
        status=FamilyTaskOccurrenceStatus.VALIDATED,
        validated_at=validated_at,
        validated_by_user_id=user.id,
    ))
    if result.rowcount != 1:
        db.refresh(occurrence)
        if occurrence.status == FamilyTaskOccurrenceStatus.VALIDATED:
            return occurrence
        raise HTTPException(status_code=409, detail="Cette occurrence a deja ete modifiee.")
    db.refresh(occurrence)
    event = record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="VALIDATE", old_status=old_status,
        actor_user_id=user.id, completed_by_user_id=occurrence.completed_by_user_id,
        occurred_at=validated_at,
    )
    occurrence._reward_points_awarded_to_me = grant_for_finalization(
        db, occurrence=occurrence, task=task, trigger_event=event, actor_user_id=user.id,
    )
    return occurrence


def reopen_occurrence(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    user: User,
) -> FamilyTaskOccurrence:
    ensure_family_parent(db, family_id=task.family_id, user=user)
    if occurrence.status == FamilyTaskOccurrenceStatus.TODO:
        return occurrence
    if occurrence.status == FamilyTaskOccurrenceStatus.FAILED:
        raise HTTPException(status_code=409, detail="Une Mission marquee comme ratee ne peut pas etre rouverte.")
    old_status = occurrence.status
    completed_by_user_id = occurrence.completed_by_user_id
    old_cycle_number = occurrence.cycle_number
    now = datetime.now(timezone.utc)
    result = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == old_status,
        FamilyTaskOccurrence.cycle_number == old_cycle_number,
    ).values(
        status=FamilyTaskOccurrenceStatus.TODO,
        completed_at=None,
        completed_by_user_id=None,
        validated_at=None,
        validated_by_user_id=None,
        cycle_number=old_cycle_number + 1 if old_cycle_number > 0 else 1,
    ))
    if result.rowcount != 1:
        db.refresh(occurrence)
        if occurrence.status == FamilyTaskOccurrenceStatus.TODO:
            return occurrence
        raise HTTPException(status_code=409, detail="Cette occurrence a deja ete modifiee.")
    db.refresh(occurrence)
    event = record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="REOPEN", old_status=old_status,
        actor_user_id=user.id, completed_by_user_id=completed_by_user_id, occurred_at=now,
        cycle_number=old_cycle_number,
    )
    db.flush()
    revoke_cycle_rewards(db, occurrence=occurrence, cycle_number=old_cycle_number, reopen_event=event)
    return occurrence


def reschedule_mission(
    db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask,
    user: User, due_date: date,
) -> FamilyTaskOccurrence:
    ensure_family_parent(db, family_id=task.family_id, user=user)
    if task.kind != "MISSION" or task.recurrence != FamilyTaskRecurrence.NONE:
        raise HTTPException(status_code=409, detail="Seules les Missions peuvent etre reportees.")
    if occurrence.status != FamilyTaskOccurrenceStatus.TODO or task.due_date is None or task.due_date >= date.today():
        raise HTTPException(status_code=409, detail="Seule une Mission en retard peut etre reportee.")
    if due_date < date.today():
        raise HTTPException(status_code=422, detail="La nouvelle echeance doit etre aujourd'hui ou plus tard.")
    old_due_date = task.due_date
    old_status = occurrence.status
    guarded = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == FamilyTaskOccurrenceStatus.TODO,
        select(FamilyTask.id).where(FamilyTask.id == task.id, FamilyTask.due_date == old_due_date).exists(),
    ).values(cycle_number=occurrence.cycle_number))
    if guarded.rowcount != 1:
        raise HTTPException(status_code=409, detail="Cette Mission a deja ete modifiee.")
    due_at, normalized_date, due_time = normalize_due_fields(due_at=None, due_date=due_date, due_time=task.due_time)
    task.due_date, task.due_at, task.due_time = normalized_date, due_at, due_time
    occurrence.scheduled_date = due_date
    now = datetime.now(timezone.utc)
    record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="RESCHEDULE", old_status=old_status,
        actor_user_id=user.id, completed_by_user_id=None, occurred_at=now,
        metadata={"old_due_date": old_due_date.isoformat(), "new_due_date": due_date.isoformat()},
    )
    return occurrence


def fail_mission(
    db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask, user: User,
) -> FamilyTaskOccurrence:
    ensure_family_parent(db, family_id=task.family_id, user=user)
    if task.kind != "MISSION" or task.recurrence != FamilyTaskRecurrence.NONE:
        raise HTTPException(status_code=409, detail="Seules les Missions ponctuelles peuvent etre marquees comme ratees.")
    if occurrence.status != FamilyTaskOccurrenceStatus.TODO or task.due_date is None or task.due_date >= date.today():
        raise HTTPException(status_code=409, detail="Seule une Mission en retard peut etre marquee comme ratee.")
    old_status = occurrence.status
    guarded = db.execute(update(FamilyTaskOccurrence).where(
        FamilyTaskOccurrence.id == occurrence.id,
        FamilyTaskOccurrence.status == FamilyTaskOccurrenceStatus.TODO,
        select(FamilyTask.id).where(
            FamilyTask.id == task.id,
            FamilyTask.due_date == task.due_date,
            FamilyTask.due_date < date.today(),
        ).exists(),
    ).values(status=FamilyTaskOccurrenceStatus.FAILED))
    if guarded.rowcount != 1:
        raise HTTPException(status_code=409, detail="Cette Mission a deja ete modifiee.")
    db.refresh(occurrence)
    now = datetime.now(timezone.utc)
    record_occurrence_transition(
        db, occurrence=occurrence, task=task, event_type="FAIL", old_status=old_status,
        actor_user_id=user.id, completed_by_user_id=None, occurred_at=now,
    )
    return occurrence


def record_occurrence_transition(
    db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask, event_type: str,
    old_status: FamilyTaskOccurrenceStatus, actor_user_id: int,
    completed_by_user_id: int | None, occurred_at: datetime, metadata: dict | None = None,
    cycle_number: int | None = None,
) -> FamilyTaskOccurrenceEvent:
    event = FamilyTaskOccurrenceEvent(
        family_id=task.family_id, task_id=task.id, occurrence_id=occurrence.id,
        category=occurrence.category, title=task.title, scheduled_date=occurrence.scheduled_date,
        scope=occurrence.scope, kind=occurrence.kind,
        event_type=event_type, status_from=old_status, status_to=occurrence.status,
        actor_user_id=actor_user_id, completed_by_user_id=completed_by_user_id,
        occurred_at=occurred_at, legacy_inferred=False,
        cycle_number=occurrence.cycle_number if cycle_number is None else cycle_number,
        metadata_json=json.dumps(metadata, separators=(",", ":")) if metadata is not None else None,
    )
    event.participants = [
        FamilyTaskOccurrenceEventParticipant(user_id=assignee.user_id)
        for assignee in task.assignees
    ]
    db.add(event)
    db.flush()
    return event


def task_payload(db: Session, task: FamilyTask) -> dict:
    return {
        "id": task.id,
        "family_id": task.family_id,
        "title": task.title,
        "description": task.description,
        "creator_user_id": task.creator_user_id,
        "category": task.category,
        "scope": task.scope,
        "kind": task.kind,
        "end_date": task.end_date,
        "priority": _enum_value(task.priority),
        "due_at": task.due_at,
        "due_date": task.due_date,
        "has_due_time": task.due_time is not None,
        "due_time": task.due_time,
        "recurrence": _enum_value(task.recurrence),
        "recurrence_interval": task.recurrence_interval,
        "selected_weekdays": parse_weekdays(task.selected_weekdays),
        "assignees": assignees_payload(db, task),
        "validation_required": task.validation_required,
        "gamification_enabled": task.gamification_enabled,
        "active": task.active,
        "created_at": task.created_at,
        "updated_at": task.updated_at,
    }


def occurrence_payload(db: Session, occurrence: FamilyTaskOccurrence) -> dict:
    task = occurrence.task
    return {
        "task_id": task.id,
        "occurrence_id": occurrence.id,
        "category": occurrence.category,
        "scope": occurrence.scope,
        "kind": occurrence.kind,
        "end_date": task.end_date,
        "title": task.title,
        "description": task.description,
        "assignees": assignees_payload(db, task),
        "scheduled_date": occurrence.scheduled_date,
        "due_at": task.due_at,
        "due_date": task.due_date,
        "has_due_time": task.due_time is not None,
        "due_time": task.due_time,
        "recurrence": _enum_value(task.recurrence),
        "recurrence_interval": task.recurrence_interval,
        "status": _enum_value(occurrence.status),
        "cycle_number": occurrence.cycle_number,
        "contributors": occurrence_contributors_payload(db, occurrence),
        "validation_required": task.validation_required,
        "gamification_enabled": task.gamification_enabled,
        "priority": _enum_value(task.priority),
        "completed_at": occurrence.completed_at,
        "completed_by": occurrence.completed_by_user_id,
        "completed_by_user": user_reference_payload(db, occurrence.completed_by_user_id),
        "validated_at": occurrence.validated_at,
        "validated_by": occurrence.validated_by_user_id,
        "validated_by_user": user_reference_payload(db, occurrence.validated_by_user_id),
        "reward_points_awarded_to_me": getattr(occurrence, "_reward_points_awarded_to_me", None),
        "reward_bundle_awarded_to_me": getattr(occurrence, "_reward_bundle_awarded_to_me", None),
    }


def occurrence_event_payload(db: Session, event: FamilyTaskOccurrenceEvent) -> dict:
    contributor_ids = occurrence_cycle_contributor_ids(db, event.occurrence_id, event.cycle_number)
    return {
        "id": event.id,
        "family_id": event.family_id,
        "task_id": event.task_id,
        "occurrence_id": event.occurrence_id,
        "category": event.category,
        "scope": event.scope,
        "kind": event.kind,
        "title": event.title,
        "scheduled_date": event.scheduled_date,
        "event_type": event.event_type,
        "cycle_number": event.cycle_number,
        "metadata": json.loads(event.metadata_json) if event.metadata_json else None,
        "contributor_user_ids": contributor_ids,
        "contributors": [
            {"user_id": user_id, "display_name": (user_reference_payload(db, user_id) or {}).get("display_name", "Membre de la famille")}
            for user_id in contributor_ids
        ],
        "status_from": _enum_value(event.status_from),
        "status_to": _enum_value(event.status_to),
        "actor_user_id": event.actor_user_id,
        "actor_user": user_reference_payload(db, event.actor_user_id),
        "completed_by_user_id": event.completed_by_user_id,
        "participant_user_ids": [participant.user_id for participant in event.participants],
        "occurred_at": event.occurred_at,
        "legacy_inferred": event.legacy_inferred,
    }


def occurrence_cycle_contributor_ids(db: Session, occurrence_id: int, cycle_number: int | None) -> list[int]:
    if cycle_number is None:
        return []
    return db.scalars(select(FamilyTaskOccurrenceContributor.user_id).where(
        FamilyTaskOccurrenceContributor.occurrence_id == occurrence_id,
        FamilyTaskOccurrenceContributor.cycle_number == cycle_number,
    ).order_by(FamilyTaskOccurrenceContributor.joined_at, FamilyTaskOccurrenceContributor.user_id)).all()


def occurrence_contributors_payload(db: Session, occurrence: FamilyTaskOccurrence) -> list[dict]:
    if occurrence.cycle_number <= 0:
        return []
    rows = db.execute(
        select(User, ChildProfile, FamilyMember)
        .join(FamilyTaskOccurrenceContributor, FamilyTaskOccurrenceContributor.user_id == User.id)
        .join(FamilyMember, and_(FamilyMember.family_id == occurrence.task.family_id, FamilyMember.user_id == User.id))
        .join(ChildProfile, ChildProfile.user_id == User.id, isouter=True)
        .where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == occurrence.cycle_number,
        ).order_by(FamilyTaskOccurrenceContributor.joined_at, User.id)
    ).all()
    return [{"user_id": user.id, "display_name": display_name_for_user(user, profile)} for user, profile, _ in rows]


def assignees_payload(db: Session, task: FamilyTask) -> list[dict]:
    rows = db.execute(
        select(User, ChildProfile, FamilyMember)
        .join(FamilyTaskAssignee, FamilyTaskAssignee.user_id == User.id)
        .join(
            FamilyMember,
            and_(FamilyMember.family_id == task.family_id, FamilyMember.user_id == User.id),
        )
        .join(ChildProfile, ChildProfile.user_id == User.id, isouter=True)
        .where(FamilyTaskAssignee.task_id == task.id)
        .order_by(User.id.asc())
    ).all()

    return [
        {
            "user_id": user.id,
            "role": _enum_name(membership.role),
            "email": user.email,
            "display_name": display_name_for_user(user, profile),
        }
        for user, profile, membership in rows
    ]


def group_items_by_member(items: list[dict]) -> list[dict]:
    groups: dict[int | None, dict] = {}
    for item in items:
        assignees = item["assignees"]
        if not assignees:
            group = groups.setdefault(None, {"assignee": None, "items": []})
            group["items"].append(item)
            continue

        for assignee in assignees:
            group = groups.setdefault(assignee["user_id"], {"assignee": assignee, "items": []})
            group["items"].append(item)

    return [groups[key] for key in sorted(groups.keys(), key=lambda value: (value is not None, value or 0))]


def _ensure_can_complete(db: Session, *, task: FamilyTask, membership: FamilyMember, user: User) -> None:
    if user.role == UserRole.PARENT and membership.role == FamilyMemberRole.PARENT:
        return
    if task.scope == "HOUSE":
        # Every family member may contribute; completion still requires a
        # contributor record for the current cycle (checked by the caller).
        return

    assignee_ids = set(
        db.scalars(select(FamilyTaskAssignee.user_id).where(FamilyTaskAssignee.task_id == task.id)).all()
    )
    if not assignee_ids or user.id in assignee_ids:
        return

    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")


def _task_start_date(task: FamilyTask) -> date:
    if task.due_date is not None:
        return task.due_date
    if task.due_at is not None:
        return task.due_at.date()
    if task.created_at is not None:
        return task.created_at.date()
    return date.today()


def _dedupe_user_ids(values: list[int]) -> list[int]:
    user_ids: list[int] = []
    for value in values:
        if value <= 0:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Assigne invalide.",
            )
        if value not in user_ids:
            user_ids.append(value)
    return user_ids


def _normalize_weekday(value: str) -> str:
    text = unicodedata.normalize("NFKD", value.strip().lower())
    return "".join(char for char in text if not unicodedata.combining(char))


def _storage_time(value: time) -> time:
    return value.replace(tzinfo=None)


def _enum_value(value: object) -> object:
    return value.value if hasattr(value, "value") else value


def _enum_name(value: object) -> str:
    return value.name if hasattr(value, "name") else str(value)
