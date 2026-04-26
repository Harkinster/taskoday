from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.child_profile import ChildProfile
from app.models.enums import UserRole
from app.models.family import Family
from app.models.family_member import FamilyMember
from app.models.user import User


def get_parent_family(db: Session, parent_user_id: int) -> Optional[Family]:
    stmt = (
        select(Family)
        .join(FamilyMember, FamilyMember.family_id == Family.id)
        .where(
            FamilyMember.user_id == parent_user_id,
            FamilyMember.role == UserRole.PARENT,
        )
        .limit(1)
    )
    return db.scalar(stmt)


def create_family_for_parent(db: Session, *, parent: User, family_name: str) -> Family:
    family = Family(name=family_name.strip())
    db.add(family)
    db.flush()

    membership = FamilyMember(family_id=family.id, user_id=parent.id, role=UserRole.PARENT)
    db.add(membership)
    db.commit()
    db.refresh(family)
    return family


def count_children_in_family(db: Session, family_id: int) -> int:
    stmt = select(func.count(ChildProfile.id)).where(ChildProfile.family_id == family_id)
    return int(db.scalar(stmt) or 0)

