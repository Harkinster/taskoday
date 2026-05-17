from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole
from app.schemas.family import PairingAttachRequest
from app.services.pairing_service import generate_pairing_code, get_active_pairing_code, get_valid_pairing_code

router = APIRouter(prefix="/pairing", tags=["pairing"])


@router.post("/generate-code")
def generate_code(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    if current_user.role != UserRole.CHILD:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    pairing = generate_pairing_code(db, current_user.id)
    db.commit()
    db.refresh(pairing)

    return success_response(
        {
            "code": pairing.code,
            "expires_at": pairing.expires_at,
        }
    )


@router.get("/my-code")
def my_code(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    if current_user.role != UserRole.CHILD:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    pairing = get_active_pairing_code(db, current_user.id)
    if not pairing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Aucun code actif.")

    return success_response(
        {
            "code": pairing.code,
            "expires_at": pairing.expires_at,
        }
    )


@router.post("/attach-child")
def attach_child(
    payload: PairingAttachRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if current_user.role != UserRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    pairing = get_valid_pairing_code(db, payload.code)
    if not pairing:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Code invalide ou expire.")

    child = db.get(User, pairing.child_user_id)
    if not child or child.role != UserRole.CHILD:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Enfant introuvable.")

    family: Family | None = None
    if payload.family_id is not None:
        family = db.get(Family, payload.family_id)
        if not family:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")

        membership = db.execute(
            select(FamilyMember).where(
                FamilyMember.family_id == payload.family_id,
                FamilyMember.user_id == current_user.id,
                FamilyMember.role == FamilyMemberRole.PARENT,
            )
        ).scalars().first()
        if not membership:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    else:
        family = db.execute(
            select(Family)
            .join(FamilyMember, FamilyMember.family_id == Family.id)
            .where(FamilyMember.user_id == current_user.id, FamilyMember.role == FamilyMemberRole.PARENT)
            .order_by(Family.id.asc())
        ).scalars().first()

        if not family:
            family = Family(name=f"Famille de {current_user.email}", created_by_user_id=current_user.id)
            db.add(family)
            db.flush()
            db.add(FamilyMember(family_id=family.id, user_id=current_user.id, role=FamilyMemberRole.PARENT))

    child_membership = db.execute(
        select(FamilyMember).where(FamilyMember.family_id == family.id, FamilyMember.user_id == child.id)
    ).scalars().first()

    if not child_membership:
        db.add(FamilyMember(family_id=family.id, user_id=child.id, role=FamilyMemberRole.CHILD))

    pairing.used_at = datetime.now(timezone.utc)
    db.commit()

    return success_response(
        {
            "family_id": family.id,
            "child_id": child.id,
            "child_email": child.email,
        },
        message="Enfant associe avec succes.",
    )
