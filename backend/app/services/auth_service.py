from sqlalchemy import select
from sqlalchemy.orm import Session
from typing import Optional

from app.core.security import hash_password, verify_password
from app.models.enums import UserRole
from app.models.family import Family
from app.models.family_member import FamilyMember
from app.models.user import User


def get_user_by_email(db: Session, email: str) -> Optional[User]:
    normalized_email = email.strip().lower()
    stmt = select(User).where(User.email == normalized_email)
    return db.scalar(stmt)


def get_family_by_name(db: Session, family_name: str) -> Optional[Family]:
    normalized_name = family_name.strip()
    stmt = select(Family).where(Family.name == normalized_name)
    return db.scalar(stmt)


def register_parent(
    db: Session,
    *,
    email: str,
    password: str,
    family_name: str,
) -> User:
    normalized_email = email.strip().lower()
    normalized_family_name = family_name.strip()

    family = Family(name=normalized_family_name)
    user = User(email=normalized_email, password_hash=hash_password(password), role=UserRole.PARENT, is_active=True)
    db.add_all([family, user])
    db.flush()

    member = FamilyMember(family_id=family.id, user_id=user.id, role=UserRole.PARENT)
    db.add(member)
    db.commit()
    db.refresh(user)
    return user


def authenticate_user(db: Session, *, email: str, password: str) -> Optional[User]:
    user = get_user_by_email(db, email)
    if user is None:
        return None
    if not verify_password(password, user.password_hash):
        return None
    return user
