from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, or_, select
from sqlalchemy.orm import Session, selectinload

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.family import FamilyMemberRole
from app.models.family_task import (
    FamilyTask, FamilyTaskOccurrenceContributor, FamilyTaskOccurrenceEvent, FamilyTaskOccurrenceEventParticipant,
    FamilyTaskOccurrenceStatus, FamilyTaskPriority, FamilyTaskRecurrence,
)
from app.models.user import User, UserRole
from app.schemas.common import SuccessResponse
from app.schemas.family_task import (
    FamilyTaskCreateRequest,
    FamilyTaskDefinitionResponse,
    FamilyTaskOccurrenceResponse,
    FamilyTaskOccurrenceEventsResponse,
    FamilyTaskOccurrencesRangeResponse,
    FamilyTaskMissionRescheduleRequest,
    FamilyTaskUpdateRequest,
    FamilyTasksTodayResponse,
)
from app.schemas.reward_engine import RewardSummaryResponse
from app.services.family_task_service import (
    complete_occurrence,
    ensure_family_member,
    ensure_family_parent,
    get_occurrence_for_member,
    get_or_create_occurrences_for_date,
    get_task_for_member,
    group_items_by_member,
    normalize_weekdays,
    normalize_due_fields,
    normalize_due_update,
    occurrence_payload,
    occurrence_event_payload,
    occurrence_category,
    start_occurrence,
    join_occurrence,
    reschedule_mission,
    fail_mission,
    parse_weekdays,
    reopen_occurrence,
    replace_task_assignees,
    task_payload,
    validate_assignee_user_ids,
    validate_occurrence,
    task_visible_to_member,
    weekdays_to_storage,
)
from app.services.reward_engine import reward_summary
from app.services.action_identity import identity_from_category, resolve_action_identity, validate_identity_recurrence

router = APIRouter(tags=["family-tasks"])

MAX_OCCURRENCE_RANGE_DAYS = 31
OVERDUE_LOOKBACK_DAYS = 30


@router.get("/families/{family_id}/rewards/me", response_model=SuccessResponse[RewardSummaryResponse])
def get_my_family_rewards(
    family_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_family_member(db, family_id=family_id, user=current_user)
    summary = reward_summary(db, family_id=family_id, user_id=current_user.id)
    summary["grants"] = [
        {
            "id": grant.id,
            "action_id": grant.action_id,
            "action_title": grant.action_title,
            "occurrence_id": grant.occurrence_id,
            "cycle_number": grant.cycle_number,
            "scope": grant.scope,
            "kind": grant.kind,
            "points": grant.points,
            "trigger_event_id": grant.trigger_event_id,
            "created_at": grant.created_at,
            "revoked_at": grant.revoked_at,
        }
        for grant in summary["grants"]
    ]
    return success_response(summary)


@router.get("/families/{family_id}/tasks", response_model=SuccessResponse[list[FamilyTaskDefinitionResponse]])
def list_family_tasks(
    family_id: int,
    include_inactive: bool = False,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)

    stmt = select(FamilyTask).options(selectinload(FamilyTask.assignees)).where(FamilyTask.family_id == family_id).order_by(FamilyTask.id.asc())
    if not include_inactive:
        stmt = stmt.where(FamilyTask.active.is_(True))
    tasks = db.scalars(stmt).all()

    return success_response([
        task_payload(db, task) for task in tasks
        if task_visible_to_member(task=task, membership=membership, user=current_user)
    ])


@router.post("/families/{family_id}/tasks", status_code=status.HTTP_201_CREATED, response_model=SuccessResponse[FamilyTaskDefinitionResponse])
def create_family_task(
    family_id: int,
    payload: FamilyTaskCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    action_scope, action_kind, action_category = resolve_action_identity(
        category=payload.category, scope=payload.scope, kind=payload.kind,
    )
    recurrence = FamilyTaskRecurrence(payload.recurrence)
    validate_identity_recurrence(kind=action_kind, recurrence=recurrence.value)
    selected_weekdays = _selected_weekdays_for_recurrence(recurrence, payload.selected_weekdays)
    due_at, due_date, due_time = normalize_due_fields(
        due_at=payload.due_at,
        due_date=payload.due_date,
        due_time=payload.due_time,
    )
    if payload.end_date is not None and action_kind != "ROUTINE":
        raise HTTPException(status_code=422, detail="Une date de fin est reservee aux routines.")
    if payload.end_date is not None and payload.end_date < (due_date or date.today()):
        raise HTTPException(status_code=422, detail="La date de fin doit suivre le debut de la routine.")
    assignee_user_ids = validate_assignee_user_ids(
        db,
        family_id=family_id,
        assignee_user_ids=payload.assignee_user_ids,
    )
    if action_scope == "PERSONAL" and len(assignee_user_ids) != 1:
        raise HTTPException(status_code=422, detail="Une action personnelle doit concerner un seul membre.")

    task = FamilyTask(
        family_id=family_id,
        title=payload.title,
        description=payload.description,
        creator_user_id=current_user.id,
        category=action_category,
        scope=action_scope,
        kind=action_kind,
        priority=FamilyTaskPriority(payload.priority),
        due_at=due_at,
        due_date=due_date,
        due_time=due_time,
        recurrence=recurrence,
        end_date=payload.end_date,
        recurrence_interval=payload.recurrence_interval,
        selected_weekdays=selected_weekdays,
        validation_required=payload.validation_required,
        gamification_enabled=payload.gamification_enabled,
        active=True,
    )
    db.add(task)
    db.flush()
    replace_task_assignees(db, task=task, assignee_user_ids=assignee_user_ids)
    db.commit()
    db.refresh(task)

    return success_response(task_payload(db, task), message="Tache familiale creee.")


@router.get("/families/{family_id}/tasks/today", response_model=SuccessResponse[FamilyTasksTodayResponse])
def family_tasks_today(
    family_id: int,
    target_date: date | None = Query(default=None, alias="date"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    scheduled_date = target_date or date.today()

    occurrences = get_or_create_occurrences_for_date(
        db,
        tasks=_active_family_tasks(db, family_id, current_user, membership),
        scheduled_date=scheduled_date,
    )
    db.commit()

    items = [occurrence_payload(db, occurrence) for occurrence in occurrences]
    return success_response(
        {
            "family_id": family_id,
            "date": scheduled_date,
            "items": items,
            "by_member": group_items_by_member(items),
        }
    )


@router.get("/families/{family_id}/task-occurrences", response_model=SuccessResponse[FamilyTaskOccurrencesRangeResponse])
def family_task_occurrences_range(
    family_id: int,
    start_date: date,
    end_date: date,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    dates = _validated_date_range(start_date, end_date)
    tasks = _active_family_tasks(db, family_id, current_user, membership)

    occurrences = []
    for scheduled_date in dates:
        occurrences.extend(
            get_or_create_occurrences_for_date(
                db,
                tasks=tasks,
                scheduled_date=scheduled_date,
            )
        )
    db.commit()

    return success_response(
        {
            "family_id": family_id,
            "start_date": start_date,
            "end_date": end_date,
            "items": [occurrence_payload(db, occurrence) for occurrence in occurrences],
        }
    )


@router.get("/families/{family_id}/task-occurrences/overdue", response_model=SuccessResponse[FamilyTaskOccurrencesRangeResponse])
def family_task_occurrences_overdue(
    family_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    today = date.today()
    start_date = today - timedelta(days=OVERDUE_LOOKBACK_DAYS)
    end_date = today - timedelta(days=1)
    tasks = _active_family_tasks(db, family_id, current_user, membership)

    occurrences = []
    for scheduled_date in _date_range(start_date, end_date):
        occurrences.extend(
            get_or_create_occurrences_for_date(
                db,
                tasks=tasks,
                scheduled_date=scheduled_date,
            )
        )
    db.commit()

    overdue = [
        occurrence
        for occurrence in occurrences
        if occurrence.scheduled_date < today and occurrence.status == FamilyTaskOccurrenceStatus.TODO
    ]

    return success_response(
        {
            "family_id": family_id,
            "start_date": start_date,
            "end_date": end_date,
            "items": [occurrence_payload(db, occurrence) for occurrence in overdue],
        }
    )


@router.get("/families/{family_id}/task-events", response_model=SuccessResponse[FamilyTaskOccurrenceEventsResponse])
def list_family_task_events(
    family_id: int,
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    actor_user_id: int | None = None,
    start_at: datetime | None = None,
    end_at: datetime | None = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    if start_at is not None and end_at is not None and start_at > end_at:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="start_at doit preceder end_at.")
    stmt = select(FamilyTaskOccurrenceEvent).options(selectinload(FamilyTaskOccurrenceEvent.participants)).where(
        FamilyTaskOccurrenceEvent.family_id == family_id
    )
    is_parent = current_user.role == UserRole.PARENT and membership.role == FamilyMemberRole.PARENT
    if not is_parent:
        own_personal = and_(
            FamilyTaskOccurrenceEvent.scope == "PERSONAL",
            select(FamilyTaskOccurrenceEventParticipant.id).where(
                FamilyTaskOccurrenceEventParticipant.event_id == FamilyTaskOccurrenceEvent.id,
                FamilyTaskOccurrenceEventParticipant.user_id == current_user.id,
            ).exists(),
        )
        own_house = and_(
            FamilyTaskOccurrenceEvent.scope == "HOUSE",
            or_(
                FamilyTaskOccurrenceEvent.actor_user_id == current_user.id,
                FamilyTaskOccurrenceEvent.completed_by_user_id == current_user.id,
                select(FamilyTaskOccurrenceContributor.id).where(
                    FamilyTaskOccurrenceContributor.occurrence_id == FamilyTaskOccurrenceEvent.occurrence_id,
                    FamilyTaskOccurrenceContributor.cycle_number == FamilyTaskOccurrenceEvent.cycle_number,
                    FamilyTaskOccurrenceContributor.user_id == current_user.id,
                ).exists(),
            ),
        )
        stmt = stmt.where(or_(own_personal, own_house))
    if actor_user_id is not None:
        stmt = stmt.where(FamilyTaskOccurrenceEvent.actor_user_id == actor_user_id)
    if start_at is not None:
        stmt = stmt.where(FamilyTaskOccurrenceEvent.occurred_at >= start_at)
    if end_at is not None:
        stmt = stmt.where(FamilyTaskOccurrenceEvent.occurred_at <= end_at)
    events = db.scalars(stmt.order_by(FamilyTaskOccurrenceEvent.occurred_at.desc(), FamilyTaskOccurrenceEvent.id.desc()).limit(limit).offset(offset)).all()
    return success_response({
        "family_id": family_id,
        "items": [occurrence_event_payload(db, event) for event in events],
        "limit": limit,
        "offset": offset,
    })


@router.patch("/family-tasks/{task_id}", response_model=SuccessResponse[FamilyTaskDefinitionResponse])
def update_family_task(
    task_id: int,
    payload: FamilyTaskUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    task, _ = get_task_for_member(db, task_id=task_id, user=current_user)
    ensure_family_parent(db, family_id=task.family_id, user=current_user)

    data = payload.model_dump(exclude_unset=True)
    if "category" in data and identity_from_category(data["category"]) != (task.scope, task.kind):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="La categorie d'une action ne peut pas etre modifiee apres sa creation.",
        )
    if ("scope" in data and data["scope"] != task.scope) or ("kind" in data and data["kind"] != task.kind):
        raise HTTPException(status_code=409, detail="Le scope et le kind d'une action sont immuables.")
    if "recurrence" in data and data["recurrence"] != task.recurrence.value:
        validate_identity_recurrence(kind=task.kind, recurrence=data["recurrence"])
    if "end_date" in data and task.kind != "ROUTINE" and data["end_date"] is not None:
        raise HTTPException(status_code=422, detail="Une date de fin est reservee aux routines.")
    assignee_user_ids = data.pop("assignee_user_ids", None)
    if assignee_user_ids is not None and task.scope == "PERSONAL" and len(set(assignee_user_ids)) != 1:
        raise HTTPException(status_code=422, detail="Une action personnelle doit concerner un seul membre.")
    recurrence_was_set = "recurrence" in data
    selected_weekdays_was_set = "selected_weekdays" in data
    selected_weekdays_payload = data.pop("selected_weekdays", None)
    due_was_set = "due_at" in data or "due_date" in data or "due_time" in data

    if "title" in data:
        task.title = data["title"]
    if "description" in data:
        task.description = data["description"]
    if "priority" in data:
        task.priority = FamilyTaskPriority(data["priority"])
    if due_was_set:
        due_at, due_date, due_time = normalize_due_update(task, data)
        task.due_at = due_at
        task.due_date = due_date
        task.due_time = due_time
    if "end_date" in data:
        task.end_date = data["end_date"]
    if task.end_date is not None and task.end_date < (task.due_date or task.created_at.date()):
        raise HTTPException(status_code=422, detail="La date de fin doit suivre le debut de la routine.")
    if "validation_required" in data:
        task.validation_required = data["validation_required"]
    if "gamification_enabled" in data:
        task.gamification_enabled = data["gamification_enabled"]
    if "recurrence_interval" in data:
        task.recurrence_interval = data["recurrence_interval"]
    if "active" in data:
        task.active = data["active"]

    if recurrence_was_set or selected_weekdays_was_set:
        recurrence = FamilyTaskRecurrence(data["recurrence"]) if recurrence_was_set else task.recurrence
        if recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS:
            days = (
                normalize_weekdays(selected_weekdays_payload)
                if selected_weekdays_was_set
                else parse_weekdays(task.selected_weekdays)
            )
            if not days:
                raise HTTPException(
                    status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                    detail="selected_weekdays est requis pour SELECTED_WEEKDAYS.",
                )
            task.selected_weekdays = weekdays_to_storage(days)
        else:
            task.selected_weekdays = None
        task.recurrence = recurrence

    if assignee_user_ids is not None:
        validated_assignees = validate_assignee_user_ids(
            db,
            family_id=task.family_id,
            assignee_user_ids=assignee_user_ids,
        )
        replace_task_assignees(db, task=task, assignee_user_ids=validated_assignees)

    db.commit()
    db.refresh(task)

    return success_response(task_payload(db, task), message="Tache familiale mise a jour.")


@router.delete("/family-tasks/{task_id}", response_model=SuccessResponse[FamilyTaskDefinitionResponse])
def deactivate_family_task(
    task_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    task, _ = get_task_for_member(db, task_id=task_id, user=current_user)
    ensure_family_parent(db, family_id=task.family_id, user=current_user)

    task.active = False
    db.commit()
    db.refresh(task)

    return success_response(task_payload(db, task), message="Tache familiale desactivee.")


@router.post("/task-occurrences/{occurrence_id}/complete", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def complete_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, membership = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    if not task.active:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tache familiale introuvable.")

    complete_occurrence(db, occurrence=occurrence, task=task, membership=membership, user=current_user)
    db.commit()
    db.refresh(occurrence)

    return success_response(occurrence_payload(db, occurrence), message="Occurrence completee.")


@router.post("/task-occurrences/{occurrence_id}/start", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def start_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, membership = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    if not task.active:
        raise HTTPException(status_code=404, detail="Tache familiale introuvable.")
    start_occurrence(db, occurrence=occurrence, task=task, membership=membership, user=current_user)
    db.commit()
    db.refresh(occurrence)
    return success_response(occurrence_payload(db, occurrence), message="Tache commencee.")


@router.post("/task-occurrences/{occurrence_id}/join", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def join_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, membership = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    if not task.active:
        raise HTTPException(status_code=404, detail="Tache familiale introuvable.")
    join_occurrence(db, occurrence=occurrence, task=task, membership=membership, user=current_user)
    db.commit()
    db.refresh(occurrence)
    return success_response(occurrence_payload(db, occurrence), message="Vous participez a cette tache.")


@router.post("/task-occurrences/{occurrence_id}/reschedule", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def reschedule_family_task_occurrence(
    occurrence_id: int,
    payload: FamilyTaskMissionRescheduleRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, _ = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    reschedule_mission(db, occurrence=occurrence, task=task, user=current_user, due_date=payload.due_date)
    db.commit()
    db.refresh(occurrence)
    return success_response(occurrence_payload(db, occurrence), message="Mission reportee.")


@router.post("/task-occurrences/{occurrence_id}/fail", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def fail_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, _ = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    fail_mission(db, occurrence=occurrence, task=task, user=current_user)
    db.commit()
    db.refresh(occurrence)
    return success_response(occurrence_payload(db, occurrence), message="Mission marquee comme ratee.")


@router.post("/task-occurrences/{occurrence_id}/validate", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def validate_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, _ = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    validate_occurrence(db, occurrence=occurrence, task=task, user=current_user)
    db.commit()
    db.refresh(occurrence)

    return success_response(occurrence_payload(db, occurrence), message="Occurrence validee.")


@router.post("/task-occurrences/{occurrence_id}/reopen", response_model=SuccessResponse[FamilyTaskOccurrenceResponse])
def reopen_family_task_occurrence(
    occurrence_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    occurrence, task, _ = get_occurrence_for_member(db, occurrence_id=occurrence_id, user=current_user)
    reopen_occurrence(db, occurrence=occurrence, task=task, user=current_user)
    db.commit()
    db.refresh(occurrence)

    return success_response(occurrence_payload(db, occurrence), message="Occurrence rouverte.")


def _selected_weekdays_for_recurrence(
    recurrence: FamilyTaskRecurrence,
    selected_weekdays: list[int | str] | None,
) -> str | None:
    if recurrence != FamilyTaskRecurrence.SELECTED_WEEKDAYS:
        return None

    days = normalize_weekdays(selected_weekdays)
    if not days:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="selected_weekdays est requis pour SELECTED_WEEKDAYS.",
        )
    return weekdays_to_storage(days)


def _active_family_tasks(db: Session, family_id: int, user: User, membership) -> list[FamilyTask]:
    tasks = db.scalars(
        select(FamilyTask)
        .options(selectinload(FamilyTask.assignees))
        .where(FamilyTask.family_id == family_id, FamilyTask.active.is_(True))
        .order_by(FamilyTask.id.asc())
    ).all()
    return [task for task in tasks if task_visible_to_member(task=task, membership=membership, user=user)]


def _validated_date_range(start_date: date, end_date: date) -> list[date]:
    if start_date > end_date:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="start_date doit etre avant ou egal a end_date.",
        )

    days = (end_date - start_date).days + 1
    if days > MAX_OCCURRENCE_RANGE_DAYS:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"La plage ne peut pas depasser {MAX_OCCURRENCE_RANGE_DAYS} jours.",
        )

    return _date_range(start_date, end_date)


def _date_range(start_date: date, end_date: date) -> list[date]:
    days = (end_date - start_date).days + 1
    return [start_date + timedelta(days=offset) for offset in range(days)]
