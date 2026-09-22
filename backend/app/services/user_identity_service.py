from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.user import User


def user_display_name(db: Session, user: User) -> str:
    profile = db.scalar(select(ChildProfile).where(ChildProfile.user_id == user.id))
    return display_name_for_user(user, profile)


def display_name_for_user(user: User, profile: ChildProfile | None = None) -> str:
    if user.display_name:
        display_name = user.display_name.strip()
        if display_name:
            return display_name

    if profile and profile.display_name.strip():
        return profile.display_name.strip()

    return user.email.split("@")[0]


def user_reference_payload(db: Session, user_id: int | None) -> dict | None:
    if user_id is None:
        return None

    row = db.execute(
        select(User, ChildProfile)
        .join(ChildProfile, ChildProfile.user_id == User.id, isouter=True)
        .where(User.id == user_id)
    ).first()
    if row is None:
        return None

    user, profile = row
    return {"user_id": user.id, "display_name": display_name_for_user(user, profile)}
