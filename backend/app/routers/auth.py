from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.session import get_db
from app.dependencies import get_current_user
from app.models.child import ChildProfile
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.user import User, UserRole
from app.schemas.auth import AuthMeResponse, LoginRequest, RegisterChildRequest, RegisterParentRequest, TokenResponse

router = APIRouter(prefix="/auth", tags=["auth"])


def _api_role(role: UserRole) -> str:
    return role.name


def _build_token_response(user: User) -> TokenResponse:
    token = create_access_token(subject=str(user.id), extra_claims={"role": _api_role(user.role)})
    return TokenResponse(
        access_token=token,
        token_type="bearer",
        expires_in=settings.jwt_expire_minutes * 60,
        role=_api_role(user.role),
    )


@router.post("/register-parent", status_code=status.HTTP_201_CREATED, response_model=TokenResponse)
def register_parent(payload: RegisterParentRequest, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email deja utilise.")

    existing_family = (
        db.execute(select(Family.id).where(func.lower(Family.name) == payload.family_name.lower())).scalars().first()
    )
    if existing_family:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Nom de famille deja utilise.")

    user = User(
        email=payload.email,
        password_hash=get_password_hash(payload.password),
        role=UserRole.PARENT,
        birth_date=payload.birth_date,
    )
    db.add(user)
    db.flush()

    family = Family(name=payload.family_name, created_by_user_id=user.id)
    db.add(family)
    db.flush()

    membership = FamilyMember(family_id=family.id, user_id=user.id, role=FamilyMemberRole.PARENT)
    db.add(membership)

    db.commit()
    db.refresh(user)

    return _build_token_response(user)


@router.post("/register-child", status_code=status.HTTP_201_CREATED, response_model=TokenResponse)
def register_child(payload: RegisterChildRequest, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == payload.email).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email deja utilise.")

    user = User(email=payload.email, password_hash=get_password_hash(payload.password), role=UserRole.CHILD)
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

    db.commit()
    db.refresh(user)

    return _build_token_response(user)


@router.post("/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Identifiants invalides.")

    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Utilisateur inactif.")

    return _build_token_response(user)


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
    }
