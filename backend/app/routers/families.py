from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.child import ChildProfile
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole
from app.schemas.family import FamilyCreateRequest

router = APIRouter(prefix="/families", tags=["families"])


@router.post("")
def create_family(
    payload: FamilyCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if current_user.role != UserRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    family = Family(name=payload.name, created_by_user_id=current_user.id)
    db.add(family)
    db.flush()

    membership = FamilyMember(family_id=family.id, user_id=current_user.id, role=FamilyMemberRole.PARENT)
    db.add(membership)
    db.commit()
    db.refresh(family)

    return success_response(
        {
            "id": family.id,
            "name": family.name,
            "created_by_user_id": family.created_by_user_id,
            "created_at": family.created_at,
        }
    )


@router.get("/me")
def my_families(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    stmt = (
        select(Family)
        .join(FamilyMember, FamilyMember.family_id == Family.id)
        .where(FamilyMember.user_id == current_user.id)
        .order_by(Family.id.asc())
    )
    families = db.execute(stmt).scalars().all()

    return success_response(
        [
            {
                "id": family.id,
                "name": family.name,
                "created_by_user_id": family.created_by_user_id,
                "created_at": family.created_at,
            }
            for family in families
        ]
    )


@router.get("/{family_id}/children")
def family_children(
    family_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    is_member = db.execute(
        select(FamilyMember.id).where(FamilyMember.family_id == family_id, FamilyMember.user_id == current_user.id)
    ).first()
    if not is_member:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")

    stmt = (
        select(User, ChildProfile)
        .join(FamilyMember, FamilyMember.user_id == User.id)
        .join(ChildProfile, ChildProfile.user_id == User.id, isouter=True)
        .where(FamilyMember.family_id == family_id, FamilyMember.role == FamilyMemberRole.CHILD)
        .order_by(User.id.asc())
    )

    rows = db.execute(stmt).all()
    children = []
    for user, profile in rows:
        children.append(
            {
                "id": user.id,
                "email": user.email,
                "display_name": profile.display_name if profile else user.email.split("@")[0],
                "avatar_url": profile.avatar_url if profile else None,
                "xp": profile.xp if profile else 0,
                "level": profile.level if profile else 1,
            }
        )

    return success_response(children)
