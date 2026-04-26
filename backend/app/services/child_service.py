from datetime import date
from typing import List, Optional, Tuple

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.child_profile import ChildProfile
from app.models.enums import UserRole
from app.models.family_member import FamilyMember
from app.models.user import User

ChildRow = Tuple[ChildProfile, User]


def create_child_for_family(
    db: Session,
    *,
    family_id: int,
    email: str,
    password: str,
    display_name: str,
    birth_date: Optional[date],
) -> ChildRow:
    normalized_email = email.strip().lower()

    child_user = User(
        email=normalized_email,
        password_hash=hash_password(password),
        role=UserRole.CHILD,
        is_active=True,
    )
    db.add(child_user)
    db.flush()

    child_membership = FamilyMember(family_id=family_id, user_id=child_user.id, role=UserRole.CHILD)
    db.add(child_membership)

    child_profile = ChildProfile(
        family_id=family_id,
        user_id=child_user.id,
        display_name=display_name.strip(),
        birth_date=birth_date,
    )
    db.add(child_profile)
    db.commit()
    db.refresh(child_profile)
    db.refresh(child_user)
    return child_profile, child_user


def get_children_for_family(db: Session, family_id: int) -> List[ChildRow]:
    stmt = (
        select(ChildProfile, User)
        .join(User, User.id == ChildProfile.user_id)
        .where(ChildProfile.family_id == family_id)
        .order_by(ChildProfile.id.asc())
    )
    rows = db.execute(stmt).all()
    return [(row[0], row[1]) for row in rows]


def get_child_in_family(db: Session, *, family_id: int, child_id: int) -> Optional[ChildRow]:
    stmt = (
        select(ChildProfile, User)
        .join(User, User.id == ChildProfile.user_id)
        .where(
            ChildProfile.family_id == family_id,
            ChildProfile.id == child_id,
        )
        .limit(1)
    )
    row = db.execute(stmt).first()
    if row is None:
        return None
    return row[0], row[1]


def get_child_by_user_id(db: Session, user_id: int) -> Optional[ChildRow]:
    stmt = (
        select(ChildProfile, User)
        .join(User, User.id == ChildProfile.user_id)
        .where(ChildProfile.user_id == user_id)
        .limit(1)
    )
    row = db.execute(stmt).first()
    if row is None:
        return None
    return row[0], row[1]

