from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.db.session import get_db
from app.models.enums import UserRole
from app.models.user import User
from app.schemas.child import ChildCreateRequest, ChildResponse
from app.services.auth_service import get_user_by_email
from app.services.child_service import (
    ChildRow,
    create_child_for_family,
    get_child_by_user_id,
    get_child_in_family,
    get_children_for_family,
)
from app.services.family_service import get_parent_family

router = APIRouter(prefix="/children", tags=["Children"])


def _to_child_response(child_row: ChildRow) -> ChildResponse:
    profile, user = child_row
    return ChildResponse(
        id=profile.id,
        family_id=profile.family_id,
        user_id=profile.user_id,
        email=user.email,
        display_name=profile.display_name,
        birth_date=profile.birth_date,
        role=user.role,
        created_at=profile.created_at,
        updated_at=profile.updated_at,
    )


@router.post("", status_code=status.HTTP_201_CREATED, response_model=ChildResponse)
def create_child(
    payload: ChildCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> ChildResponse:
    family = get_parent_family(db, current_user.id)
    if family is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Parent must have a family before adding children",
        )

    if get_user_by_email(db, payload.email) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Email already registered",
        )

    child_row = create_child_for_family(
        db,
        family_id=family.id,
        email=payload.email,
        password=payload.password,
        display_name=payload.display_name,
        birth_date=payload.birth_date,
    )
    return _to_child_response(child_row)


@router.get("", response_model=List[ChildResponse])
def list_children(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
) -> List[ChildResponse]:
    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            return []
        rows = get_children_for_family(db, family.id)
        return [_to_child_response(row) for row in rows]

    child_row = get_child_by_user_id(db, current_user.id)
    if child_row is None:
        return []
    return [_to_child_response(child_row)]


@router.get("/{child_id}", response_model=ChildResponse)
def get_child(
    child_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
) -> ChildResponse:
    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Family not found for this parent",
            )

        child_row = get_child_in_family(db, family_id=family.id, child_id=child_id)
        if child_row is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Child not found",
            )
        return _to_child_response(child_row)

    own_child = get_child_by_user_id(db, current_user.id)
    if own_child is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Child profile not found",
        )
    if own_child[0].id != child_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Children can only access their own profile",
        )
    return _to_child_response(own_child)

