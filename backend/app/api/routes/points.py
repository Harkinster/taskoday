from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.db.session import get_db
from app.models.child_profile import ChildProfile
from app.models.enums import UserRole
from app.models.user import User
from app.schemas.points import PointsBalanceResponse, PointsHistoryItemResponse, PointsHistoryResponse
from app.services.child_service import get_child_by_user_id
from app.services.family_service import get_parent_family
from app.services.points_service import get_points_balance, get_points_history

router = APIRouter(tags=["Points"])


def _resolve_accessible_child(
    db: Session,
    *,
    current_user: User,
    child_id: int,
) -> ChildProfile:
    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Family not found for this parent",
            )
        child = db.scalar(
            select(ChildProfile).where(
                ChildProfile.id == child_id,
                ChildProfile.family_id == family.id,
            ),
        )
        if child is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Child not found",
            )
        return child

    own_child = get_child_by_user_id(db, current_user.id)
    if own_child is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Child profile not found",
        )
    if own_child[0].id != child_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Children can only access their own points",
        )
    return own_child[0]


@router.get("/children/{child_id}/points", response_model=PointsBalanceResponse)
def get_child_points(
    child_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
) -> PointsBalanceResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    return PointsBalanceResponse(
        child_id=child_id,
        balance=get_points_balance(db, child_id),
    )


@router.get("/children/{child_id}/points/history", response_model=PointsHistoryResponse)
def get_child_points_history(
    child_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
    limit: Annotated[int, Query(ge=1, le=500)] = 100,
) -> PointsHistoryResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    transactions = get_points_history(db, child_id, limit=limit)
    return PointsHistoryResponse(
        child_id=child_id,
        balance=get_points_balance(db, child_id),
        transactions=[
            PointsHistoryItemResponse(
                id=tx.id,
                item_type=tx.item_type,
                item_id=tx.item_id,
                completion_date=tx.completion_date,
                transaction_type=tx.transaction_type,
                amount=tx.amount,
                created_at=tx.created_at,
            )
            for tx in transactions
        ],
    )

