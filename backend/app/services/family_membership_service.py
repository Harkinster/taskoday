"""Membership changes scoped to one family; user accounts and child profiles survive."""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import delete, func, select, update
from sqlalchemy.orm import Session

from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_invite import FamilyInvite
from app.models.family_task import FamilyTask, FamilyTaskAssignee
from app.models.user import User, UserRole


def leave_family(db: Session, *, family_id: int, user: User) -> None:
    family = _locked_family(db, family_id)
    membership = _membership(db, family_id, user.id)
    if family is None or family.archived_at is not None or membership is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    if user.role != UserRole.PARENT or membership.role != FamilyMemberRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Un enfant ne peut pas quitter ce foyer.")
    _remove_membership(db, membership)


def remove_family_member(db: Session, *, family_id: int, target_user_id: int, actor: User) -> None:
    family = _locked_family(db, family_id)
    actor_membership = _membership(db, family_id, actor.id)
    if family is None or family.archived_at is not None or actor_membership is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    if actor.role != UserRole.PARENT or actor_membership.role != FamilyMemberRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    if target_user_id == actor.id:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Utilise l'action Quitter ce foyer.")
    target = _membership(db, family_id, target_user_id)
    if target is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Membre introuvable dans ce foyer.")
    _remove_membership(db, target)


def _locked_family(db: Session, family_id: int) -> Family | None:
    # PostgreSQL serializes concurrent removals in the same family. SQLite tests
    # ignore FOR UPDATE but still exercise the membership and role rules.
    return db.scalar(select(Family).where(Family.id == family_id).with_for_update())


def _membership(db: Session, family_id: int, user_id: int) -> FamilyMember | None:
    return db.scalar(
        select(FamilyMember).where(FamilyMember.family_id == family_id, FamilyMember.user_id == user_id)
    )


def _remove_membership(db: Session, membership: FamilyMember) -> None:
    if membership.role == FamilyMemberRole.PARENT:
        parent_count = db.scalar(
            select(func.count(FamilyMember.id)).where(
                FamilyMember.family_id == membership.family_id,
                FamilyMember.role == FamilyMemberRole.PARENT,
            )
        )
        if parent_count is None or parent_count <= 1:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Le dernier parent ne peut pas quitter ce foyer.",
            )

    # An assignment is valid only while its user belongs to this family.
    family_task_ids = select(FamilyTask.id).where(FamilyTask.family_id == membership.family_id)
    db.execute(
        delete(FamilyTaskAssignee).where(
            FamilyTaskAssignee.user_id == membership.user_id,
            FamilyTaskAssignee.task_id.in_(family_task_ids),
        )
    )
    if membership.role == FamilyMemberRole.PARENT:
        # A removed parent must not leave behind a usable invitation.
        db.execute(
            update(FamilyInvite)
            .where(
                FamilyInvite.family_id == membership.family_id,
                FamilyInvite.created_by_user_id == membership.user_id,
                FamilyInvite.accepted_at.is_(None),
            )
            .values(expires_at=datetime.now(timezone.utc))
        )
    db.delete(membership)
