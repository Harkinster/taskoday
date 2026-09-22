from __future__ import annotations

import hashlib
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_invite import FamilyInvite
from app.models.user import User, UserRole


FAMILY_INVITE_TTL = timedelta(hours=48)
TOKEN_BYTES = 24


def create_parent_invite(db: Session, *, family_id: int, user: User) -> tuple[FamilyInvite, str]:
    _ensure_family_parent(db, family_id=family_id, user=user)
    now = _utcnow()

    db.execute(
        update(FamilyInvite)
        .where(
            FamilyInvite.family_id == family_id,
            FamilyInvite.created_by_user_id == user.id,
            FamilyInvite.accepted_at.is_(None),
            FamilyInvite.expires_at > now,
        )
        .values(expires_at=now)
        .execution_options(synchronize_session=False)
    )

    expires_at = now + FAMILY_INVITE_TTL
    for _ in range(10):
        token = secrets.token_urlsafe(TOKEN_BYTES)
        token_hash = _token_hash(token)
        exists = db.execute(select(FamilyInvite.id).where(FamilyInvite.token_hash == token_hash)).first()
        if exists:
            continue

        invite = FamilyInvite(
            family_id=family_id,
            created_by_user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
        )
        db.add(invite)
        db.flush()
        return invite, token

    raise RuntimeError("Impossible de generer une invitation unique.")


def validate_parent_invite_code(db: Session, *, code: str) -> FamilyInvite:
    invite = _get_invite_by_code(db, code)
    _ensure_invite_can_be_used(db, invite=invite, now=_utcnow())
    return invite


def accept_parent_invite(db: Session, *, code: str, user: User) -> FamilyInvite:
    if user.role != UserRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")

    now = _utcnow()
    invite = _get_invite_by_code(db, code)
    _ensure_invite_can_be_used(db, invite=invite, now=now)
    _ensure_user_can_join_family(db, user=user, family_id=invite.family_id)

    result = db.execute(
        update(FamilyInvite)
        .where(
            FamilyInvite.id == invite.id,
            FamilyInvite.accepted_at.is_(None),
            FamilyInvite.expires_at > now,
        )
        .values(accepted_at=now, accepted_by_user_id=user.id)
        .execution_options(synchronize_session=False)
    )
    if result.rowcount != 1:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Invitation deja utilisee ou expiree.")

    db.add(FamilyMember(family_id=invite.family_id, user_id=user.id, role=FamilyMemberRole.PARENT))
    db.flush()
    db.refresh(invite)
    return invite


def invite_payload(db: Session, invite: FamilyInvite, token: str) -> dict:
    family = db.get(Family, invite.family_id)
    return {
        "invite_id": invite.id,
        "code": token,
        "expires_at": invite.expires_at,
        "family_id": invite.family_id,
        "family_name": family.name if family else None,
    }


def accepted_invite_payload(db: Session, invite: FamilyInvite) -> dict:
    family = db.get(Family, invite.family_id)
    return {
        "invite_id": invite.id,
        "family_id": invite.family_id,
        "family_name": family.name if family else None,
        "accepted_at": invite.accepted_at,
    }


def _get_invite_by_code(db: Session, code: str) -> FamilyInvite:
    token_hash = _token_hash(code)
    invite = db.scalar(select(FamilyInvite).where(FamilyInvite.token_hash == token_hash))
    if invite is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Invitation introuvable ou invalide.")
    return invite


def _ensure_invite_can_be_used(db: Session, *, invite: FamilyInvite, now: datetime) -> None:
    if invite.accepted_at is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Invitation deja utilisee.")
    if _as_aware_utc(invite.expires_at) <= now:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Invitation expiree.")
    if db.get(Family, invite.family_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Invitation introuvable ou invalide.")


def _ensure_user_can_join_family(db: Session, *, user: User, family_id: int) -> None:
    family_ids = db.scalars(select(FamilyMember.family_id).where(FamilyMember.user_id == user.id)).all()
    if family_id in family_ids:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Ce compte appartient deja a ce foyer.")


def _ensure_family_parent(db: Session, *, family_id: int, user: User) -> FamilyMember:
    membership = db.scalar(
        select(FamilyMember).where(FamilyMember.family_id == family_id, FamilyMember.user_id == user.id)
    )
    if membership is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Famille introuvable.")
    if user.role != UserRole.PARENT or membership.role != FamilyMemberRole.PARENT:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Action non autorisee.")
    return membership


def _token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


def _as_aware_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)
