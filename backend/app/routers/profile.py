from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, get_current_user, success_response
from app.models.child import ChildProfile
from app.models.family import FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole
from app.models.xp import XpHistory

router = APIRouter(tags=["profile"])


@router.get("/profile/me")
def profile_me(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    if current_user.role == UserRole.CHILD:
        profile = db.query(ChildProfile).filter(ChildProfile.user_id == current_user.id).first()
        return success_response(
            {
                "id": current_user.id,
                "email": current_user.email,
                "role": current_user.role.value,
                "display_name": profile.display_name if profile else current_user.email.split("@")[0],
                "avatar_url": profile.avatar_url if profile else None,
                "xp": profile.xp if profile else 0,
                "level": profile.level if profile else 1,
            }
        )

    family_ids = db.execute(select(FamilyMember.family_id).where(FamilyMember.user_id == current_user.id)).scalars().all()
    child_count = 0
    if family_ids:
        child_count = (
            db.scalar(
                select(func.count(func.distinct(FamilyMember.user_id))).where(
                    FamilyMember.family_id.in_(family_ids),
                    FamilyMember.role == FamilyMemberRole.CHILD,
                )
            )
            or 0
        )

    return success_response(
        {
            "id": current_user.id,
            "email": current_user.email,
            "role": current_user.role.value,
            "families_count": len(set(family_ids)),
            "children_count": child_count,
        }
    )


@router.get("/children/{child_id}/profile")
def child_profile(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    child = ensure_child_access(db, current_user, child_id)
    profile = db.query(ChildProfile).filter(ChildProfile.user_id == child_id).first()

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


@router.get("/children/{child_id}/xp-history")
def child_xp_history(
    child_id: int,
    limit: int = Query(default=100, ge=1, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)

    rows = db.execute(
        select(XpHistory).where(XpHistory.child_id == child_id).order_by(XpHistory.id.desc()).limit(limit)
    ).scalars().all()

    return success_response(
        [
            {
                "id": item.id,
                "child_id": item.child_id,
                "amount": item.amount,
                "reason": item.reason,
                "source_type": item.source_type,
                "source_id": item.source_id,
                "created_at": item.created_at,
            }
            for item in rows
        ]
    )
