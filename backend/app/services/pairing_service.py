import secrets
import string
from datetime import datetime, timedelta, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.pairing import PairingCode


def _random_code(length: int = 6) -> str:
    alphabet = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))


def get_active_pairing_code(db: Session, child_user_id: int) -> PairingCode | None:
    now = datetime.now(timezone.utc)
    stmt = (
        select(PairingCode)
        .where(
            PairingCode.child_user_id == child_user_id,
            PairingCode.used_at.is_(None),
            PairingCode.expires_at > now,
        )
        .order_by(PairingCode.id.desc())
    )
    return db.execute(stmt).scalars().first()


def generate_pairing_code(db: Session, child_user_id: int) -> PairingCode:
    current_code = get_active_pairing_code(db, child_user_id)
    if current_code:
        current_code.used_at = datetime.now(timezone.utc)

    expires_at = datetime.now(timezone.utc) + timedelta(minutes=settings.pairing_code_expire_minutes)

    for _ in range(10):
        candidate = _random_code()
        exists = db.execute(select(PairingCode.id).where(PairingCode.code == candidate)).first()
        if not exists:
            pairing = PairingCode(child_user_id=child_user_id, code=candidate, expires_at=expires_at)
            db.add(pairing)
            db.flush()
            return pairing

    raise RuntimeError("Impossible de generer un code unique")


def get_valid_pairing_code(db: Session, code: str) -> PairingCode | None:
    now = datetime.now(timezone.utc)
    stmt = select(PairingCode).where(
        PairingCode.code == code,
        PairingCode.used_at.is_(None),
        PairingCode.expires_at > now,
    )
    return db.execute(stmt).scalars().first()
