from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.family_task import FamilyTask, FamilyTaskPriority, FamilyTaskRecurrence
from app.models.user import User
from app.schemas.family_task import FamilyTaskCreateRequest, FamilyTaskUpdateRequest
from app.services.family_task_service import (
    complete_occurrence,
    ensure_family_member,
    ensure_family_parent,
    get_occurrence_for_member,
    get_or_create_occurrence,
    get_task_for_member,
    group_items_by_member,
    is_task_scheduled_for_date,
    normalize_weekdays,
    normalize_due_fields,
    normalize_due_update,
    occurrence_payload,
    parse_weekdays,
    reopen_occurrence,
    replace_task_assignees,
    task_payload,
    validate_assignee_user_ids,
    validate_occurrence,
    weekdays_to_storage,
)

router = APIRouter(tags=["family-tasks"])


@router.get("/families/{family_id}/tasks")
def list_family_tasks(
    family_id: int,
    include_inactive: bool = False,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_family_member(db, family_id=family_id, user=current_user)

    stmt = select(FamilyTask).where(FamilyTask.family_id == family_id).order_by(FamilyTask.id.asc())
    if not include_inactive:
        stmt = stmt.where(FamilyTask.active.is_(True))
    tasks = db.scalars(stmt).all()

    return success_response([task_payload(db, task) for task in tasks])


@router.post("/families/{family_id}/tasks", status_code=status.HTTP_201_CREATED)
def create_family_task(
    family_id: int,
    payload: FamilyTaskCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    recurrence = FamilyTaskRecurrence(payload.recurrence)
    selected_weekdays = _selected_weekdays_for_recurrence(recurrence, payload.selected_weekdays)
    due_at, due_date, due_time = normalize_due_fields(
        due_at=payload.due_at,
        due_date=payload.due_date,
        due_time=payload.due_time,
    )
    assignee_user_ids = validate_assignee_user_ids(
        db,
        family_id=family_id,
        assignee_user_ids=payload.assignee_user_ids,
    )

    task = FamilyTask(
        family_id=family_id,
        title=payload.title,
        description=payload.description,
        creator_user_id=current_user.id,
        category=payload.category,
        priority=FamilyTaskPriority(payload.priority),
        due_at=due_at,
        due_date=due_date,
        due_time=due_time,
        recurrence=recurrence,
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


@router.get("/families/{family_id}/tasks/today")
def family_tasks_today(
    family_id: int,
    target_date: date | None = Query(default=None, alias="date"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_family_member(db, family_id=family_id, user=current_user)
    scheduled_date = target_date or date.today()

    tasks = db.scalars(
        select(FamilyTask)
        .where(FamilyTask.family_id == family_id, FamilyTask.active.is_(True))
        .order_by(FamilyTask.id.asc())
    ).all()
    occurrences = [
        get_or_create_occurrence(db, task=task, scheduled_date=scheduled_date)
        for task in tasks
        if is_task_scheduled_for_date(task, scheduled_date)
    ]
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


@router.patch("/family-tasks/{task_id}")
def update_family_task(
    task_id: int,
    payload: FamilyTaskUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    task, _ = get_task_for_member(db, task_id=task_id, user=current_user)
    ensure_family_parent(db, family_id=task.family_id, user=current_user)

    data = payload.model_dump(exclude_unset=True)
    assignee_user_ids = data.pop("assignee_user_ids", None)
    recurrence_was_set = "recurrence" in data
    selected_weekdays_was_set = "selected_weekdays" in data
    selected_weekdays_payload = data.pop("selected_weekdays", None)
    due_was_set = "due_at" in data or "due_date" in data or "due_time" in data

    if "title" in data:
        task.title = data["title"]
    if "description" in data:
        task.description = data["description"]
    if "category" in data:
        task.category = data["category"]
    if "priority" in data:
        task.priority = FamilyTaskPriority(data["priority"])
    if due_was_set:
        due_at, due_date, due_time = normalize_due_update(task, data)
        task.due_at = due_at
        task.due_date = due_date
        task.due_time = due_time
    if "validation_required" in data:
        task.validation_required = data["validation_required"]
    if "gamification_enabled" in data:
        task.gamification_enabled = data["gamification_enabled"]
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


@router.delete("/family-tasks/{task_id}")
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


@router.post("/task-occurrences/{occurrence_id}/complete")
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


@router.post("/task-occurrences/{occurrence_id}/validate")
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


@router.post("/task-occurrences/{occurrence_id}/reopen")
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
