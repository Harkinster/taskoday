"""Archive a family without deleting its memberships or history."""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import func, select, update
from sqlalchemy.orm import Session

from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_invite import FamilyInvite
from app.models.user import User, UserRole


def archive_family(db: Session, *, family_id: int, actor: User) -> None:
    family = db.scalar(select(Family).where(Family.id == family_id).with_for_update())
    if family is None or family.archived_at is not None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    membership = db.scalar(select(FamilyMember).where(
        FamilyMember.family_id == family_id, FamilyMember.user_id == actor.id,
    ))
    if membership is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    if actor.role != UserRole.PARENT or membership.role != FamilyMemberRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    member_count = db.scalar(select(func.count(FamilyMember.id)).where(FamilyMember.family_id == family_id))
    if member_count != 1:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Retirez ou faites quitter les autres membres avant d'archiver cette famille.",
        )
    now = datetime.now(timezone.utc)
    family.archived_at = now
    db.execute(update(FamilyInvite).where(
        FamilyInvite.family_id == family_id,
        FamilyInvite.accepted_at.is_(None),
    ).values(expires_at=now))
