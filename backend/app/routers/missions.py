from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, ensure_parent_child_access, get_current_user, success_response
from app.models.task import Mission, TaskCompletion, TaskStatus, TaskType
from app.models.user import User, UserRole
from app.schemas.task import MissionCreateRequest, MissionUpdateRequest

router = APIRouter(tags=["missions"])


@router.get("/children/{child_id}/missions")
def list_missions(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)

    missions = db.execute(select(Mission).where(Mission.child_id == child_id).order_by(Mission.id.asc())).scalars().all()

    return success_response(
        [
            {
                "id": mission.id,
                "child_id": mission.child_id,
                "title": mission.title,
                "description": mission.description,
                "status": mission.status.value,
                "due_date": mission.due_date,
                "created_by_user_id": mission.created_by_user_id,
            }
            for mission in missions
        ]
    )


@router.post("/children/{child_id}/missions")
def create_mission(
    child_id: int,
    payload: MissionCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if current_user.role == UserRole.PARENT:
        ensure_parent_child_access(db, current_user, child_id)
    elif current_user.role == UserRole.CHILD:
        if current_user.id != child_id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    else:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    mission = Mission(
        child_id=child_id,
        title=payload.title,
        description=payload.description,
        due_date=payload.due_date,
        status=TaskStatus.OPEN,
        created_by_user_id=current_user.id,
    )
    db.add(mission)
    db.commit()
    db.refresh(mission)

    return success_response(
        {
            "id": mission.id,
            "child_id": mission.child_id,
            "title": mission.title,
            "description": mission.description,
            "status": mission.status.value,
            "due_date": mission.due_date,
            "created_by_user_id": mission.created_by_user_id,
        }
    )


@router.patch("/missions/{mission_id}")
def update_mission(
    mission_id: int,
    payload: MissionUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    mission = db.get(Mission, mission_id)
    if not mission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mission introuvable.")

    ensure_parent_child_access(db, current_user, mission.child_id)

    data = payload.model_dump(exclude_unset=True)
    if "title" in data:
        mission.title = data["title"]
    if "description" in data:
        mission.description = data["description"]
    if "due_date" in data:
        mission.due_date = data["due_date"]
    if "status" in data:
        mission.status = TaskStatus(data["status"])

    db.commit()
    db.refresh(mission)

    return success_response(
        {
            "id": mission.id,
            "child_id": mission.child_id,
            "title": mission.title,
            "description": mission.description,
            "status": mission.status.value,
            "due_date": mission.due_date,
            "created_by_user_id": mission.created_by_user_id,
        },
        message="Mission mise a jour.",
    )


@router.delete("/missions/{mission_id}")
def delete_mission(mission_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    mission = db.get(Mission, mission_id)
    if not mission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mission introuvable.")

    ensure_parent_child_access(db, current_user, mission.child_id)

    db.delete(mission)
    db.commit()

    return success_response({"id": mission_id}, message="Mission supprimee.")


@router.post("/missions/{mission_id}/complete")
def complete_mission(mission_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    mission = db.get(Mission, mission_id)
    if not mission:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Mission introuvable.")

    ensure_child_access(db, current_user, mission.child_id)

    mission.status = TaskStatus.COMPLETED

    completion = db.execute(
        select(TaskCompletion).where(
            TaskCompletion.task_type == TaskType.MISSION,
            TaskCompletion.task_id == mission.id,
            TaskCompletion.child_id == mission.child_id,
        )
    ).scalars().first()

    if not completion:
        db.add(
            TaskCompletion(
                task_type=TaskType.MISSION,
                task_id=mission.id,
                child_id=mission.child_id,
                completed_by_user_id=current_user.id,
            )
        )

    db.commit()

    return success_response({"mission_id": mission.id, "completed": True}, message="Mission completee.")
