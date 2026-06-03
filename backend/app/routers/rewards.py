from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc, select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, ensure_parent_child_access, get_current_user, success_response
from app.models.reward import ExternalReward, RewardCoupon, RewardRequest, RewardRequestStatus
from app.models.user import User, UserRole
from app.schemas.reward import (
    ExternalRewardResponse,
    RewardCouponResponse,
    RewardCreateRequest,
    RewardRequestCreate,
    RewardRequestDecision,
    RewardRequestResponse,
    RewardUpdateRequest,
    ScaleBalanceResponse,
    ScaleTransactionResponse,
)
from app.services.reward_service import (
    InsufficientScalesError,
    RewardInactiveError,
    RewardNotFoundError,
    RewardRequestNotFoundError,
    RewardRequestStatusError,
    create_reward,
    decide_reward_request,
    get_coupon_by_id,
    get_coupon_for_request,
    get_reward_by_id,
    get_reward_request_by_id,
    list_reward_requests_for_child,
    list_rewards_for_child,
    mark_coupon_used,
    request_reward,
    update_reward,
)
from app.services.scales_service import get_scales_balance, get_scales_history

router = APIRouter(tags=["rewards"])


@router.get("/children/{child_id}/scales")
def get_child_scales(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(ScaleBalanceResponse(child_id=child_id, balance=get_scales_balance(db, child_id)).model_dump())


@router.get("/children/{child_id}/flammeches")
def get_child_flammeches(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response({"child_id": child_id, "balance": get_scales_balance(db, child_id), "currency": "flammeches"})


@router.get("/children/{child_id}/scales/history")
def get_child_scales_history(
    child_id: int,
    limit: int = Query(default=100, ge=1, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    transactions = get_scales_history(db, child_id, limit=limit)
    return success_response(
        {
            "child_id": child_id,
            "balance": get_scales_balance(db, child_id),
            "transactions": [scale_transaction_response(tx).model_dump(mode="json") for tx in transactions],
        }
    )


@router.get("/children/{child_id}/flammeches/history")
def get_child_flammeches_history(
    child_id: int,
    limit: int = Query(default=100, ge=1, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    transactions = get_scales_history(db, child_id, limit=limit)
    return success_response(
        {
            "child_id": child_id,
            "balance": get_scales_balance(db, child_id),
            "currency": "flammeches",
            "transactions": [scale_transaction_response(tx).model_dump(mode="json") for tx in transactions],
        }
    )


@router.post("/children/{child_id}/wishes", status_code=status.HTTP_201_CREATED)
@router.post("/children/{child_id}/rewards", status_code=status.HTTP_201_CREATED)
def create_child_reward(
    child_id: int,
    payload: RewardCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_parent_child_access(db, current_user, child_id)
    reward = create_reward(
        db,
        child_id=child_id,
        title=payload.title,
        description=payload.description,
        cost_scales=payload.cost_scales,
        emoji=payload.emoji,
        is_active=payload.is_active,
        created_by_user_id=current_user.id,
    )
    db.commit()
    db.refresh(reward)
    return success_response(external_reward_response(reward).model_dump(mode="json"), message="Recompense creee.")


@router.get("/children/{child_id}/wishes")
@router.get("/children/{child_id}/rewards")
def list_child_rewards(
    child_id: int,
    include_inactive: bool = Query(default=False),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    active_only = current_user.role == UserRole.CHILD or not include_inactive
    rewards = list_rewards_for_child(db, child_id=child_id, active_only=active_only)
    return success_response(
        {
            "child_id": child_id,
            "scales_balance": get_scales_balance(db, child_id),
            "flammeches_balance": get_scales_balance(db, child_id),
            "rewards": [external_reward_response(reward).model_dump(mode="json") for reward in rewards],
        }
    )


@router.patch("/rewards/{reward_id}")
def patch_reward(
    reward_id: int,
    payload: RewardUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    reward = resolve_parent_reward(db, current_user=current_user, reward_id=reward_id)
    update_data = payload.model_dump(exclude_unset=True)
    reward = update_reward(
        db,
        reward=reward,
        title=update_data.get("title"),
        description=update_data.get("description"),
        description_set="description" in update_data,
        cost_scales=update_data.get("cost_scales"),
        emoji=update_data.get("emoji"),
        is_active=update_data.get("is_active"),
    )
    db.commit()
    db.refresh(reward)
    return success_response(external_reward_response(reward).model_dump(mode="json"), message="Recompense mise a jour.")


@router.post("/wishes/{reward_id}/requests", status_code=status.HTTP_201_CREATED)
@router.post("/rewards/{reward_id}/requests", status_code=status.HTTP_201_CREATED)
def create_reward_request(
    reward_id: int,
    payload: RewardRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if current_user.role != UserRole.CHILD:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Seul l'enfant peut demander une recompense.")
    reward = resolve_accessible_reward(db, current_user=current_user, reward_id=reward_id)
    try:
        reward_request = request_reward(
            db,
            reward_id=reward.id,
            requested_by_user_id=current_user.id,
            note=payload.note,
        )
    except RewardInactiveError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Recompense inactive.") from exc
    except InsufficientScalesError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Flammeches insuffisantes: balance={exc.balance}, required={exc.required}.",
        ) from exc
    db.commit()
    db.refresh(reward_request)
    return success_response(reward_request_response(db, reward_request).model_dump(mode="json"), message="Demande envoyee.")


@router.get("/children/{child_id}/wish-requests")
@router.get("/children/{child_id}/reward-requests")
def list_child_reward_requests(
    child_id: int,
    limit: int = Query(default=100, ge=1, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    requests = list_reward_requests_for_child(db, child_id=child_id, limit=limit)
    return success_response(
        {
            "child_id": child_id,
            "scales_balance": get_scales_balance(db, child_id),
            "flammeches_balance": get_scales_balance(db, child_id),
            "requests": [reward_request_response(db, item).model_dump(mode="json") for item in requests],
        }
    )


@router.patch("/reward-requests/{request_id}")
def decide_child_reward_request(
    request_id: int,
    payload: RewardRequestDecision,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    reward_request = resolve_parent_reward_request(db, current_user=current_user, request_id=request_id)
    try:
        decided = decide_reward_request(
            db,
            reward_request=reward_request,
            status=payload.status,
            decided_by_user_id=current_user.id,
            parent_note=payload.parent_note,
            expires_at=payload.expires_at,
        )
    except RewardRequestStatusError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Transition de statut invalide.") from exc
    except InsufficientScalesError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Flammeches insuffisantes: balance={exc.balance}, required={exc.required}.",
        ) from exc
    db.commit()
    db.refresh(decided)
    return success_response(reward_request_response(db, decided).model_dump(mode="json"), message="Demande mise a jour.")


@router.post("/scrolls/{coupon_id}/use")
@router.post("/reward-coupons/{coupon_id}/use")
def use_reward_coupon(
    coupon_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    coupon = get_coupon_by_id(db, coupon_id)
    if coupon is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Parchemin introuvable.")
    ensure_parent_child_access(db, current_user, coupon.child_id)
    try:
        reward_request = mark_coupon_used(db, coupon=coupon, used_by_user_id=current_user.id)
    except (RewardRequestNotFoundError, RewardRequestStatusError) as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Parchemin non utilisable.") from exc
    db.commit()
    db.refresh(reward_request)
    return success_response(reward_request_response(db, reward_request).model_dump(mode="json"), message="Parchemin utilise.")


@router.get("/children/{child_id}/scrolls")
def list_child_scrolls(
    child_id: int,
    limit: int = Query(default=100, ge=1, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    coupons = db.scalars(
        select(RewardCoupon)
        .where(RewardCoupon.child_id == child_id)
        .order_by(desc(RewardCoupon.created_at), desc(RewardCoupon.id))
        .limit(limit)
    ).all()
    return success_response(
        {
            "child_id": child_id,
            "scrolls": [reward_coupon_response(coupon).model_dump(mode="json") for coupon in coupons],
        }
    )


def resolve_accessible_reward(db: Session, *, current_user: User, reward_id: int) -> ExternalReward:
    reward = get_reward_by_id(db, reward_id)
    if reward is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Recompense introuvable.")
    ensure_child_access(db, current_user, reward.child_id)
    return reward


def resolve_parent_reward(db: Session, *, current_user: User, reward_id: int) -> ExternalReward:
    reward = get_reward_by_id(db, reward_id)
    if reward is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Recompense introuvable.")
    ensure_parent_child_access(db, current_user, reward.child_id)
    return reward


def resolve_parent_reward_request(db: Session, *, current_user: User, request_id: int) -> RewardRequest:
    reward_request = get_reward_request_by_id(db, request_id)
    if reward_request is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Demande introuvable.")
    ensure_parent_child_access(db, current_user, reward_request.child_id)
    return reward_request


def external_reward_response(reward: ExternalReward) -> ExternalRewardResponse:
    return ExternalRewardResponse(
        id=reward.id,
        child_id=reward.child_id,
        title=reward.title,
        description=reward.description,
        cost_scales=reward.cost_scales,
        emoji=reward.emoji,
        is_active=reward.is_active,
        created_by_user_id=reward.created_by_user_id,
        created_at=reward.created_at,
        updated_at=reward.updated_at,
    )


def reward_request_response(db: Session, reward_request: RewardRequest) -> RewardRequestResponse:
    coupon = get_coupon_for_request(db, reward_request.id)
    return RewardRequestResponse(
        id=reward_request.id,
        child_id=reward_request.child_id,
        reward_id=reward_request.reward_id,
        reward_title=reward_request.reward_title,
        cost_scales=reward_request.cost_scales,
        status=reward_request.status,
        requested_by_user_id=reward_request.requested_by_user_id,
        decided_by_user_id=reward_request.decided_by_user_id,
        requested_at=reward_request.requested_at,
        decided_at=reward_request.decided_at,
        expires_at=reward_request.expires_at,
        note=reward_request.note,
        parent_note=reward_request.parent_note,
        coupon=reward_coupon_response(coupon) if coupon else None,
    )


def reward_coupon_response(coupon: RewardCoupon) -> RewardCouponResponse:
    return RewardCouponResponse(
        id=coupon.id,
        request_id=coupon.request_id,
        child_id=coupon.child_id,
        reward_id=coupon.reward_id,
        code=coupon.code,
        status=coupon.status,
        created_at=coupon.created_at,
        used_at=coupon.used_at,
    )


def scale_transaction_response(transaction) -> ScaleTransactionResponse:
    return ScaleTransactionResponse(
        id=transaction.id,
        child_id=transaction.child_id,
        amount=transaction.amount,
        reason=transaction.reason,
        source_type=transaction.source_type,
        source_id=transaction.source_id,
        transaction_type=transaction.transaction_type,
        event_key=transaction.event_key,
        created_at=transaction.created_at,
    )
