from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
import unicodedata

from fastapi import HTTPException, status
from sqlalchemy import and_, delete, insert, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.family import FamilyMember, FamilyMemberRole
from app.models.family_task import (
    FamilyTask,
    FamilyTaskAssignee,
    FamilyTaskOccurrence,
    FamilyTaskOccurrenceStatus,
    FamilyTaskRecurrence,
)
from app.models.user import User, UserRole


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


def ensure_family_member(db: Session, *, family_id: int, user: User) -> FamilyMember:
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
    return occurrence, task, membership


def is_task_scheduled_for_date(task: FamilyTask, target_date: date) -> bool:
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


def get_or_create_occurrence(db: Session, *, task: FamilyTask, scheduled_date: date) -> FamilyTaskOccurrence:
    occurrence = _find_occurrence(db, task_id=task.id, scheduled_date=scheduled_date)
    if occurrence is not None:
        return occurrence

    try:
        with db.begin_nested():
            db.execute(
                insert(FamilyTaskOccurrence).values(
                    task_id=task.id,
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
    return [
        get_or_create_occurrence(db, task=task, scheduled_date=scheduled_date)
        for task in tasks
        if is_task_scheduled_for_date(task, scheduled_date)
    ]


def complete_occurrence(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    membership: FamilyMember,
    user: User,
) -> FamilyTaskOccurrence:
    _ensure_can_complete(db, task=task, membership=membership, user=user)

    if occurrence.status in {
        FamilyTaskOccurrenceStatus.COMPLETED,
        FamilyTaskOccurrenceStatus.PENDING_VALIDATION,
        FamilyTaskOccurrenceStatus.VALIDATED,
    }:
        return occurrence

    now = datetime.now(timezone.utc)
    occurrence.completed_at = now
    occurrence.completed_by_user_id = user.id
    occurrence.validated_at = None
    occurrence.validated_by_user_id = None
    occurrence.status = (
        FamilyTaskOccurrenceStatus.PENDING_VALIDATION
        if task.validation_required
        else FamilyTaskOccurrenceStatus.COMPLETED
    )
    db.add(occurrence)
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

    occurrence.status = FamilyTaskOccurrenceStatus.VALIDATED
    occurrence.validated_at = datetime.now(timezone.utc)
    occurrence.validated_by_user_id = user.id
    db.add(occurrence)
    return occurrence


def reopen_occurrence(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    user: User,
) -> FamilyTaskOccurrence:
    ensure_family_parent(db, family_id=task.family_id, user=user)
    occurrence.status = FamilyTaskOccurrenceStatus.TODO
    occurrence.completed_at = None
    occurrence.completed_by_user_id = None
    occurrence.validated_at = None
    occurrence.validated_by_user_id = None
    db.add(occurrence)
    return occurrence


def task_payload(db: Session, task: FamilyTask) -> dict:
    return {
        "id": task.id,
        "family_id": task.family_id,
        "title": task.title,
        "description": task.description,
        "creator_user_id": task.creator_user_id,
        "category": task.category,
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
        "validation_required": task.validation_required,
        "gamification_enabled": task.gamification_enabled,
        "priority": _enum_value(task.priority),
        "completed_at": occurrence.completed_at,
        "completed_by": occurrence.completed_by_user_id,
        "validated_at": occurrence.validated_at,
        "validated_by": occurrence.validated_by_user_id,
    }


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
            "display_name": profile.display_name if profile else user.email.split("@")[0],
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
