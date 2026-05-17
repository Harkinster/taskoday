from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.xp import XpHistory


def xp_to_level(total_xp: int) -> int:
    # Progression simple et lisible: 100 XP par niveau.
    return max(1, total_xp // 100 + 1)


def add_xp(
    db: Session,
    child_id: int,
    amount: int,
    reason: str,
    source_type: str,
    source_id: int | None = None,
) -> tuple[ChildProfile, XpHistory]:
    profile = db.query(ChildProfile).filter(ChildProfile.user_id == child_id).first()
    if not profile:
        raise ValueError("Child profile introuvable")

    profile.xp += amount
    profile.level = xp_to_level(profile.xp)

    history = XpHistory(
        child_id=child_id,
        amount=amount,
        reason=reason,
        source_type=source_type,
        source_id=source_id,
    )
    db.add(history)
    db.flush()

    return profile, history
