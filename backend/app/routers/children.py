from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, get_accessible_child_ids, get_current_user, success_response
from app.models.child import ChildProfile
from app.models.task import Mission, Quest, Routine, TaskCompletion, TaskStatus, TaskType
from app.models.user import User, UserRole
from app.models.xp import XpHistory
from app.schemas.child import ChildUpdateRequest

router = APIRouter(prefix="/children", tags=["children"])


@router.get("")
def list_children(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    child_ids = get_accessible_child_ids(db, current_user)
    if not child_ids:
        return success_response([])

    stmt = (
        select(User, ChildProfile)
        .join(ChildProfile, ChildProfile.user_id == User.id, isouter=True)
        .where(User.id.in_(child_ids), User.role == UserRole.CHILD)
        .order_by(User.id.asc())
    )
    rows = db.execute(stmt).all()

    data = []
    for user, profile in rows:
        data.append(
            {
                "id": user.id,
                "email": user.email,
                "display_name": profile.display_name if profile else user.email.split("@")[0],
                "avatar_url": profile.avatar_url if profile else None,
                "xp": profile.xp if profile else 0,
                "level": profile.level if profile else 1,
                "created_at": user.created_at,
            }
        )

    return success_response(data)


@router.get("/{child_id}")
def get_child(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    child = ensure_child_access(db, current_user, child_id)
    profile = db.query(ChildProfile).filter(ChildProfile.user_id == child.id).first()

    return success_response(
        {
            "id": child.id,
            "email": child.email,
            "display_name": profile.display_name if profile else child.email.split("@")[0],
            "avatar_url": profile.avatar_url if profile else None,
            "xp": profile.xp if profile else 0,
            "level": profile.level if profile else 1,
            "created_at": child.created_at,
        }
    )


@router.patch("/{child_id}")
def update_child(
    child_id: int,
    payload: ChildUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    child = ensure_child_access(db, current_user, child_id)
    profile = db.query(ChildProfile).filter(ChildProfile.user_id == child.id).first()
    if not profile:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Profil enfant introuvable.")

    update_data = payload.model_dump(exclude_unset=True)
    if "display_name" in update_data:
        profile.display_name = update_data["display_name"]
    if "avatar_url" in update_data:
        profile.avatar_url = update_data["avatar_url"]

    db.commit()
    db.refresh(profile)

    return success_response(
        {
            "id": child.id,
            "display_name": profile.display_name,
            "avatar_url": profile.avatar_url,
            "xp": profile.xp,
            "level": profile.level,
        },
        message="Profil enfant mis a jour.",
    )


@router.get("/{child_id}/stats")
def child_stats(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)

    routine_total = db.scalar(select(func.count(Routine.id)).where(Routine.child_id == child_id, Routine.is_active.is_(True))) or 0
    routine_completed = (
        db.scalar(
            select(func.count(TaskCompletion.id)).where(
                TaskCompletion.child_id == child_id,
                TaskCompletion.task_type == TaskType.ROUTINE,
            )
        )
        or 0
    )

    mission_total = db.scalar(select(func.count(Mission.id)).where(Mission.child_id == child_id)) or 0
    mission_completed = (
        db.scalar(select(func.count(Mission.id)).where(Mission.child_id == child_id, Mission.status == TaskStatus.COMPLETED))
        or 0
    )

    quest_total = db.scalar(select(func.count(Quest.id)).where(Quest.child_id == child_id)) or 0
    quest_completed = (
        db.scalar(select(func.count(Quest.id)).where(Quest.child_id == child_id, Quest.status == TaskStatus.COMPLETED)) or 0
    )

    profile = db.query(ChildProfile).filter(ChildProfile.user_id == child_id).first()
    xp = profile.xp if profile else 0
    level = profile.level if profile else 1

    xp_events = db.scalar(select(func.count(XpHistory.id)).where(XpHistory.child_id == child_id)) or 0

    return success_response(
        {
            "child_id": child_id,
            "xp": xp,
            "level": level,
            "routines": {"total": routine_total, "completed": routine_completed},
            "missions": {"total": mission_total, "completed": mission_completed},
            "quests": {"total": quest_total, "completed": quest_completed},
            "xp_history_events": xp_events,
        }
    )
