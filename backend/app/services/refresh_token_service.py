import hashlib
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.refresh_token import RefreshToken
from app.models.user import User, UserRole


TOKEN_BYTES = 48
SESSION_ID_BYTES = 24


def issue_refresh_token(
    db: Session,
    *,
    user: User,
    session_id: str | None = None,
) -> tuple[RefreshToken, str]:
    now = _utcnow()
    logical_session_id = session_id or secrets.token_urlsafe(SESSION_ID_BYTES)

    for _ in range(5):
        token = secrets.token_urlsafe(TOKEN_BYTES)
        token_hash = _token_hash(token)
        if db.scalar(select(RefreshToken.id).where(RefreshToken.token_hash == token_hash)) is not None:
            continue

        refresh_token = RefreshToken(
            user_id=user.id,
            session_id=logical_session_id,
            token_hash=token_hash,
            expires_at=now + timedelta(days=settings.refresh_token_expire_days),
        )
        db.add(refresh_token)
        db.flush()
        return refresh_token, token

    raise RuntimeError("Impossible de generer un refresh token unique.")


def rotate_refresh_token(db: Session, *, token: str) -> tuple[User, RefreshToken, str]:
    now = _utcnow()
    refresh_token = _get_refresh_token(db, token=token)
    if refresh_token is None or refresh_token.revoked_at is not None:
        raise _invalid_refresh_token()
    if _as_aware_utc(refresh_token.expires_at) <= now:
        raise _invalid_refresh_token()

    user = db.get(User, refresh_token.user_id)
    if user is None or not user.is_active or user.role not in (UserRole.PARENT, UserRole.CHILD):
        raise _invalid_refresh_token()

    consumed = db.execute(
        update(RefreshToken)
        .where(
            RefreshToken.id == refresh_token.id,
            RefreshToken.revoked_at.is_(None),
            RefreshToken.expires_at > now,
        )
        .values(revoked_at=now, last_used_at=now)
        .execution_options(synchronize_session=False)
    )
    if consumed.rowcount != 1:
        raise _invalid_refresh_token()

    replacement, replacement_token = issue_refresh_token(
        db,
        user=user,
        session_id=refresh_token.session_id,
    )
    db.execute(
        update(RefreshToken)
        .where(RefreshToken.id == refresh_token.id)
        .values(replaced_by_token_id=replacement.id)
        .execution_options(synchronize_session=False)
    )
    return user, replacement, replacement_token


def revoke_refresh_token(db: Session, *, token: str) -> None:
    refresh_token = _get_refresh_token(db, token=token)
    if refresh_token is None:
        return

    db.execute(
        update(RefreshToken)
        .where(RefreshToken.id == refresh_token.id, RefreshToken.revoked_at.is_(None))
        .values(revoked_at=_utcnow())
        .execution_options(synchronize_session=False)
    )


def refresh_token_expires_in_seconds() -> int:
    return settings.refresh_token_expire_days * 24 * 60 * 60


def _get_refresh_token(db: Session, *, token: str) -> RefreshToken | None:
    return db.scalar(select(RefreshToken).where(RefreshToken.token_hash == _token_hash(token)))


def _token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _invalid_refresh_token() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Refresh token invalide ou expire.",
    )


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


def _as_aware_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)
