from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.core.config import settings
from app.core.security import create_access_token
from app.db.session import get_db
from app.models.enums import UserRole
from app.models.user import User
from app.schemas.auth import LoginRequest, MeResponse, RegisterParentRequest, TokenResponse
from app.services.auth_service import authenticate_user, get_family_by_name, get_user_by_email, register_parent

router = APIRouter(prefix="/auth", tags=["Auth"])


def _build_token_response(user: User) -> TokenResponse:
    token = create_access_token(
        subject=str(user.id),
        extra_claims={"role": user.role.value},
    )
    return TokenResponse(
        access_token=token,
        expires_in=settings.jwt_expire_minutes * 60,
        role=user.role,
    )


@router.post("/register-parent", status_code=status.HTTP_201_CREATED, response_model=TokenResponse)
def register_parent_endpoint(payload: RegisterParentRequest, db: Annotated[Session, Depends(get_db)]) -> TokenResponse:
    if get_user_by_email(db, payload.email) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Email already registered",
        )
    if get_family_by_name(db, payload.family_name) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Family name already exists",
        )

    user = register_parent(
        db,
        email=payload.email,
        password=payload.password,
        family_name=payload.family_name,
    )
    return _build_token_response(user)


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Annotated[Session, Depends(get_db)]) -> TokenResponse:
    user = authenticate_user(db, email=payload.email, password=payload.password)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
        )
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Inactive user",
        )
    return _build_token_response(user)


@router.get("/me", response_model=MeResponse)
def me(
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
) -> MeResponse:
    return MeResponse(
        id=current_user.id,
        email=current_user.email,
        role=current_user.role,
        is_active=current_user.is_active,
        family_ids=[membership.family_id for membership in current_user.family_memberships],
    )
