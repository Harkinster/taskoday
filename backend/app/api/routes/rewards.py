from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.db.session import get_db
from app.models.child_profile import ChildProfile
from app.models.enums import UserRole
from app.models.reward import Reward
from app.models.user import User
from app.schemas.reward import (
    RewardCreateRequest,
    RewardPurchaseResponse,
    RewardPurchaseResult,
    RewardPurchasesResponse,
    RewardResponse,
    RewardsResponse,
    RewardUpdateRequest,
)
from app.services.child_service import get_child_by_user_id
from app.services.family_service import get_parent_family
from app.services.points_service import get_points_balance
from app.services.reward_service import (
    InsufficientPointsError,
    RewardInactiveError,
    RewardNotFoundError,
    create_reward,
    get_reward_by_id,
    list_reward_purchases_for_child,
    list_rewards_for_child,
    purchase_reward,
    update_reward,
)

router = APIRouter(tags=["Rewards"])


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
            detail="Children can only access their own rewards",
        )
    return own_child[0]


def _resolve_accessible_reward(
    db: Session,
    *,
    current_user: User,
    reward_id: int,
) -> Reward:
    reward = get_reward_by_id(db, reward_id)
    if reward is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Reward not found",
        )

    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Family not found for this parent",
            )
        child_match = db.scalar(
            select(ChildProfile.id).where(
                ChildProfile.id == reward.child_profile_id,
                ChildProfile.family_id == family.id,
            ),
        )
        if child_match is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Reward not found",
            )
        return reward

    own_child = get_child_by_user_id(db, current_user.id)
    if own_child is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Child profile not found",
        )
    if own_child[0].id != reward.child_profile_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Children can only access their own rewards",
        )
    return reward


@router.post("/children/{child_id}/rewards", status_code=status.HTTP_201_CREATED, response_model=RewardResponse)
def create_child_reward(
    child_id: int,
    payload: RewardCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> RewardResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    reward = create_reward(
        db,
        child_id=child_id,
        title=payload.title,
        description=payload.description,
        cost_points=payload.cost_points,
        is_active=payload.is_active,
    )
    db.commit()
    db.refresh(reward)
    return RewardResponse.model_validate(reward)


@router.get("/children/{child_id}/rewards", response_model=RewardsResponse)
def get_child_rewards(
    child_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
    include_inactive: Annotated[bool, Query()] = False,
) -> RewardsResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    active_only = not include_inactive or current_user.role == UserRole.CHILD
    rewards = list_rewards_for_child(db, child_id=child_id, active_only=active_only)
    return RewardsResponse(
        child_id=child_id,
        rewards=[RewardResponse.model_validate(reward) for reward in rewards],
    )


@router.patch("/rewards/{reward_id}", response_model=RewardResponse)
def patch_reward(
    reward_id: int,
    payload: RewardUpdateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> RewardResponse:
    reward = _resolve_accessible_reward(db, current_user=current_user, reward_id=reward_id)
    update_data = payload.model_dump(exclude_unset=True)
    reward = update_reward(
        db,
        reward=reward,
        title=update_data.get("title"),
        description=update_data.get("description"),
        description_set="description" in update_data,
        cost_points=update_data.get("cost_points"),
        is_active=update_data.get("is_active"),
    )
    db.commit()
    db.refresh(reward)
    return RewardResponse.model_validate(reward)


@router.post("/rewards/{reward_id}/purchase", response_model=RewardPurchaseResult)
def purchase_child_reward(
    reward_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.CHILD))],
) -> RewardPurchaseResult:
    reward = _resolve_accessible_reward(db, current_user=current_user, reward_id=reward_id)
    try:
        purchase = purchase_reward(db, reward_id=reward.id)
    except RewardNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reward not found") from exc
    except RewardInactiveError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reward is inactive",
        ) from exc
    except InsufficientPointsError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Insufficient points: balance={exc.balance}, required={exc.required}",
        ) from exc

    balance = get_points_balance(db, reward.child_profile_id)
    db.commit()
    db.refresh(purchase)
    return RewardPurchaseResult(
        purchase=RewardPurchaseResponse.model_validate(purchase),
        balance=balance,
    )


@router.get("/children/{child_id}/reward-purchases", response_model=RewardPurchasesResponse)
def get_child_reward_purchases(
    child_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
    limit: Annotated[int, Query(ge=1, le=500)] = 100,
) -> RewardPurchasesResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    purchases = list_reward_purchases_for_child(db, child_id=child_id, limit=limit)
    return RewardPurchasesResponse(
        child_id=child_id,
        balance=get_points_balance(db, child_id),
        purchases=[RewardPurchaseResponse.model_validate(purchase) for purchase in purchases],
    )
