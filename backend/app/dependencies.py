from typing import Any

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import Select, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import decode_access_token
from app.db.session import get_db
from app.models.family import FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.api_v1_prefix}/auth/login")


def success_response(data: Any = None, message: str | None = None) -> dict[str, Any]:
    return {
        "success": True,
        "data": data if data is not None else {},
        "message": message,
    }


def error_403(message: str = "Action non autorisee.") -> HTTPException:
    return HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=message)


def get_current_user(
    db: Session = Depends(get_db),
    token: str = Depends(oauth2_scheme),
) -> User:
    unauthorized = HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token invalide.")
    try:
        payload = decode_access_token(token)
    except ValueError:
        raise unauthorized

    sub = payload.get("sub")
    if not sub:
        raise unauthorized

    try:
        user_id = int(sub)
    except (TypeError, ValueError):
        raise unauthorized

    user = db.get(User, user_id)
    if not user:
        raise unauthorized
    return user


def require_role(*roles: UserRole):
    def checker(current_user: User = Depends(get_current_user)) -> User:
        if current_user.role not in roles:
            raise error_403()
        return current_user

    return checker


def _parent_child_query(parent_user_id: int) -> Select[tuple[int]]:
    parent_family_ids = select(FamilyMember.family_id).where(FamilyMember.user_id == parent_user_id)
    return select(FamilyMember.user_id).where(
        FamilyMember.family_id.in_(parent_family_ids),
        FamilyMember.role == FamilyMemberRole.CHILD,
    )


def is_parent_linked_to_child(db: Session, parent_user_id: int, child_user_id: int) -> bool:
    query = _parent_child_query(parent_user_id).where(FamilyMember.user_id == child_user_id)
    return db.execute(query).first() is not None


def get_accessible_child_ids(db: Session, user: User) -> list[int]:
    if user.role == UserRole.CHILD:
        return [user.id]

    rows = db.execute(_parent_child_query(user.id)).all()
    return [row[0] for row in rows]


def ensure_child_exists(db: Session, child_id: int) -> User:
    child = db.get(User, child_id)
    if not child or child.role != UserRole.CHILD:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Enfant introuvable.")
    return child


def ensure_child_access(db: Session, user: User, child_id: int) -> User:
    child = ensure_child_exists(db, child_id)
    if user.role == UserRole.CHILD and user.id != child_id:
        raise error_403()
    if user.role == UserRole.PARENT and not is_parent_linked_to_child(db, user.id, child_id):
        raise error_403()
    return child


def ensure_parent_child_access(db: Session, user: User, child_id: int) -> User:
    if user.role != UserRole.PARENT:
        raise error_403()
    child = ensure_child_exists(db, child_id)
    if not is_parent_linked_to_child(db, user.id, child_id):
        raise error_403()
    return child
