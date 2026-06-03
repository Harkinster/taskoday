from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, ensure_parent_child_access, get_current_user, success_response
from app.models.task import RepeatType, Routine, TaskCompletion, TaskType
from app.models.user import User
from app.schemas.task import RoutineCreateRequest, RoutineUpdateRequest
from app.services.gamification_service import award_task_completion
from app.services.scales_service import revoke_scales_for_task_if_missing

router = APIRouter(tags=["routines"])


@router.get("/children/{child_id}/routines")
def list_routines(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)

    routines = db.execute(
        select(Routine).where(Routine.child_id == child_id, Routine.is_active.is_(True)).order_by(Routine.id.asc())
    ).scalars().all()

    completed_ids = set(
        db.execute(
            select(TaskCompletion.task_id).where(TaskCompletion.child_id == child_id, TaskCompletion.task_type == TaskType.ROUTINE)
        )
        .scalars()
        .all()
    )

    return success_response(
        [
            {
                "id": routine.id,
                "child_id": routine.child_id,
                "title": routine.title,
                "description": routine.description,
                "period": routine.period,
                "repeat_type": routine.repeat_type.value,
                "created_by_user_id": routine.created_by_user_id,
                "created_by_role": routine.created_by_role,
                "can_child_delete": routine.can_child_delete,
                "is_active": routine.is_active,
                "completed": routine.id in completed_ids,
            }
            for routine in routines
        ]
    )


@router.post("/children/{child_id}/routines")
def create_routine(
    child_id: int,
    payload: RoutineCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_parent_child_access(db, current_user, child_id)

    routine = Routine(
        child_id=child_id,
        title=payload.title,
        description=payload.description,
        period=payload.period,
        repeat_type=RepeatType(payload.repeat_type),
        created_by_user_id=current_user.id,
        created_by_role=current_user.role.value,
        can_child_delete=payload.can_child_delete,
        is_active=True,
    )
    db.add(routine)
    db.commit()
    db.refresh(routine)

    return success_response(
        {
            "id": routine.id,
            "child_id": routine.child_id,
            "title": routine.title,
            "description": routine.description,
            "period": routine.period,
            "repeat_type": routine.repeat_type.value,
            "created_by_user_id": routine.created_by_user_id,
            "created_by_role": routine.created_by_role,
            "can_child_delete": routine.can_child_delete,
            "is_active": routine.is_active,
        }
    )


@router.patch("/routines/{routine_id}")
def update_routine(
    routine_id: int,
    payload: RoutineUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    routine = db.get(Routine, routine_id)
    if not routine:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Routine introuvable.")

    ensure_parent_child_access(db, current_user, routine.child_id)

    data = payload.model_dump(exclude_unset=True)
    if "title" in data:
        routine.title = data["title"]
    if "description" in data:
        routine.description = data["description"]
    if "period" in data:
        routine.period = data["period"]
    if "repeat_type" in data:
        routine.repeat_type = RepeatType(data["repeat_type"])
    if "can_child_delete" in data:
        routine.can_child_delete = data["can_child_delete"]
    if "is_active" in data:
        routine.is_active = data["is_active"]

    db.commit()
    db.refresh(routine)

    return success_response(
        {
            "id": routine.id,
            "child_id": routine.child_id,
            "title": routine.title,
            "description": routine.description,
            "period": routine.period,
            "repeat_type": routine.repeat_type.value,
            "can_child_delete": routine.can_child_delete,
            "is_active": routine.is_active,
        },
        message="Routine mise a jour.",
    )


@router.delete("/routines/{routine_id}")
def delete_routine(routine_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    routine = db.get(Routine, routine_id)
    if not routine:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Routine introuvable.")

    ensure_parent_child_access(db, current_user, routine.child_id)
    routine.is_active = False
    db.commit()

    return success_response({"id": routine_id}, message="Routine desactivee.")


@router.post("/routines/{routine_id}/complete")
def complete_routine(routine_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    routine = db.get(Routine, routine_id)
    if not routine or not routine.is_active:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Routine introuvable.")

    ensure_child_access(db, current_user, routine.child_id)

    existing = db.execute(
        select(TaskCompletion).where(
            TaskCompletion.task_type == TaskType.ROUTINE,
            TaskCompletion.task_id == routine.id,
            TaskCompletion.child_id == routine.child_id,
        )
    ).scalars().first()

    if not existing:
        completion = TaskCompletion(
            task_type=TaskType.ROUTINE,
            task_id=routine.id,
            child_id=routine.child_id,
            completed_by_user_id=current_user.id,
        )
        db.add(completion)
        award = award_task_completion(
            db,
            child_id=routine.child_id,
            task_type=TaskType.ROUTINE,
            task_id=routine.id,
            title=routine.title,
        )
        db.commit()

        return success_response({"routine_id": routine.id, "completed": True, "award": award}, message="Routine completee.")

    return success_response({"routine_id": routine.id, "completed": True, "award": None}, message="Routine deja completee.")


@router.post("/routines/{routine_id}/uncomplete")
def uncomplete_routine(routine_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    routine = db.get(Routine, routine_id)
    if not routine:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Routine introuvable.")

    ensure_child_access(db, current_user, routine.child_id)

    completion = db.execute(
        select(TaskCompletion).where(
            TaskCompletion.task_type == TaskType.ROUTINE,
            TaskCompletion.task_id == routine.id,
            TaskCompletion.child_id == routine.child_id,
        )
    ).scalars().first()

    if completion:
        db.delete(completion)
        revoke_scales_for_task_if_missing(
            db,
            child_id=routine.child_id,
            task_type=TaskType.ROUTINE,
            task_id=routine.id,
            title=routine.title,
        )
        db.commit()

    return success_response({"routine_id": routine.id, "completed": False}, message="Routine devalidee.")
