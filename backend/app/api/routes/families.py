from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.db.session import get_db
from app.models.enums import UserRole
from app.models.user import User
from app.schemas.family import CurrentFamilyResponse, FamilyCreateRequest, FamilyResponse
from app.services.auth_service import get_family_by_name
from app.services.family_service import count_children_in_family, create_family_for_parent, get_parent_family

router = APIRouter(prefix="/families", tags=["Families"])


@router.post("", status_code=status.HTTP_201_CREATED, response_model=FamilyResponse)
def create_family(
    payload: FamilyCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> FamilyResponse:
    current_family = get_parent_family(db, current_user.id)
    if current_family is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Parent already linked to a family",
        )

    if get_family_by_name(db, payload.name) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Family name already exists",
        )

    family = create_family_for_parent(db, parent=current_user, family_name=payload.name)
    return FamilyResponse.model_validate(family)


@router.get("/current", response_model=CurrentFamilyResponse)
def get_current_family(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> CurrentFamilyResponse:
    family = get_parent_family(db, current_user.id)
    if family is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Family not found for this parent",
        )

    return CurrentFamilyResponse(
        id=family.id,
        name=family.name,
        created_at=family.created_at,
        updated_at=family.updated_at,
        children_count=count_children_in_family(db, family.id),
    )

