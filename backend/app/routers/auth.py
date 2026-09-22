from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.session import get_db
from app.dependencies import get_current_user
from app.models.child import ChildProfile
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole
from app.schemas.auth import (
    AuthMeResponse,
    LoginRequest,
    RefreshTokenRequest,
    RegisterChildRequest,
    RegisterParentRequest,
    TokenResponse,
)
from app.services.refresh_token_service import (
    issue_refresh_token,
    refresh_token_expires_in_seconds,
    revoke_refresh_token,
    rotate_refresh_token,
)
from app.services.family_invite_service import accept_parent_invite, validate_parent_invite_code
from app.services.user_identity_service import user_display_name

router = APIRouter(prefix="/auth", tags=["auth"])


def _api_role(role: UserRole) -> str:
    return role.name


def _build_token_response(user: User, *, refresh_token: str) -> TokenResponse:
    token = create_access_token(subject=str(user.id), extra_claims={"role": _api_role(user.role)})
    return TokenResponse(
        access_token=token,
        token_type="bearer",
        expires_in=settings.jwt_expire_minutes * 60,
        role=_api_role(user.role),
        refresh_token=refresh_token,
        refresh_expires_in=refresh_token_expires_in_seconds(),
    )


def _issue_token_response(db: Session, *, user: User) -> TokenResponse:
    _, refresh_token = issue_refresh_token(db, user=user)
    return _build_token_response(user, refresh_token=refresh_token)


@router.post("/register-parent", status_code=status.HTTP_201_CREATED, response_model=TokenResponse)
def register_parent(payload: RegisterParentRequest, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email deja utilise.")

    if payload.invite_code is not None:
        validate_parent_invite_code(db, code=payload.invite_code)
    elif payload.family_name is not None:
        existing_family = db.scalar(select(Family.id).where(func.lower(Family.name) == payload.family_name.lower()))
        if existing_family is not None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Nom de famille deja utilise.")

    user = User(
        email=payload.email,
        password_hash=get_password_hash(payload.password),
        role=UserRole.PARENT,
        display_name=payload.display_name,
        birth_date=payload.birth_date,
    )
    db.add(user)
    db.flush()

    if payload.invite_code is not None:
        accept_parent_invite(db, code=payload.invite_code, user=user)
    elif payload.family_name is not None:
        family = Family(name=payload.family_name, created_by_user_id=user.id)
        db.add(family)
        db.flush()
        db.add(FamilyMember(family_id=family.id, user_id=user.id, role=FamilyMemberRole.PARENT))

    token_response = _issue_token_response(db, user=user)
    db.commit()
    return token_response


@router.post("/register-child", status_code=status.HTTP_201_CREATED, response_model=TokenResponse)
def register_child(payload: RegisterChildRequest, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email deja utilise.")

    user = User(
        email=payload.email,
        password_hash=get_password_hash(payload.password),
        role=UserRole.CHILD,
        display_name=payload.display_name,
        birth_date=payload.birth_date,
    )
    db.add(user)
    db.flush()

    profile = ChildProfile(
        user_id=user.id,
        display_name=payload.display_name,
        birth_date=payload.birth_date,
        avatar_url=None,
        xp=0,
        level=1,
    )
    db.add(profile)

    token_response = _issue_token_response(db, user=user)
    db.commit()
    return token_response


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Identifiants invalides.")

    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Utilisateur inactif.")

    token_response = _issue_token_response(db, user=user)
    db.commit()
    return token_response


@router.post("/refresh", response_model=TokenResponse)
def refresh(payload: RefreshTokenRequest, db: Session = Depends(get_db)):
    user, _, refresh_token = rotate_refresh_token(db, token=payload.refresh_token)
    token_response = _build_token_response(user, refresh_token=refresh_token)
    db.commit()
    return token_response


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(payload: RefreshTokenRequest, db: Session = Depends(get_db)) -> Response:
    revoke_refresh_token(db, token=payload.refresh_token)
    db.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/me", response_model=AuthMeResponse)
def me(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    family_ids = (
        db.execute(
            select(FamilyMember.family_id)
            .where(FamilyMember.user_id == current_user.id)
            .order_by(FamilyMember.family_id.asc())
        )
        .scalars()
        .all()
    )

    return {
        "id": current_user.id,
        "email": current_user.email,
        "role": _api_role(current_user.role),
        "is_active": current_user.is_active,
        "family_ids": family_ids,
        "display_name": user_display_name(db, current_user),
        "birth_date": current_user.birth_date,
    }
