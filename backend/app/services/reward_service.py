from __future__ import annotations

import secrets
from datetime import datetime, timezone
from typing import List

from sqlalchemy import desc, select
from sqlalchemy.orm import Session

from app.models.reward import (
    ExternalReward,
    RewardCoupon,
    RewardRequest,
    RewardRequestStatus,
    ScaleTransactionType,
    ScrollStatus,
)
from app.services.scales_service import create_scale_transaction_if_missing, get_scales_balance


class RewardNotFoundError(Exception):
    pass


class RewardInactiveError(Exception):
    pass


class RewardRequestNotFoundError(Exception):
    pass


class RewardRequestStatusError(Exception):
    pass


class InsufficientScalesError(Exception):
    def __init__(self, balance: int, required: int) -> None:
        self.balance = balance
        self.required = required
        super().__init__(f"Insufficient scales: balance={balance}, required={required}")


def create_reward(
    db: Session,
    *,
    child_id: int,
    title: str,
    description: str | None,
    cost_scales: int,
    emoji: str,
    is_active: bool,
    created_by_user_id: int,
) -> ExternalReward:
    reward = ExternalReward(
        child_id=child_id,
        title=title.strip(),
        description=description.strip() if description else None,
        cost_scales=cost_scales,
        emoji=emoji.strip(),
        is_active=is_active,
        created_by_user_id=created_by_user_id,
    )
    db.add(reward)
    db.flush()
    return reward


def list_rewards_for_child(db: Session, *, child_id: int, active_only: bool = True) -> List[ExternalReward]:
    stmt = select(ExternalReward).where(ExternalReward.child_id == child_id)
    if active_only:
        stmt = stmt.where(ExternalReward.is_active.is_(True))
    stmt = stmt.order_by(ExternalReward.cost_scales.asc(), ExternalReward.id.asc())
    return list(db.scalars(stmt).all())


def get_reward_by_id(db: Session, reward_id: int) -> ExternalReward | None:
    return db.scalar(select(ExternalReward).where(ExternalReward.id == reward_id))


def update_reward(
    db: Session,
    *,
    reward: ExternalReward,
    title: str | None = None,
    description: str | None = None,
    description_set: bool = False,
    cost_scales: int | None = None,
    emoji: str | None = None,
    is_active: bool | None = None,
) -> ExternalReward:
    if title is not None:
        reward.title = title.strip()
    if description_set:
        reward.description = description.strip() if description else None
    if cost_scales is not None:
        reward.cost_scales = cost_scales
    if emoji is not None:
        reward.emoji = emoji.strip()
    if is_active is not None:
        reward.is_active = is_active
    db.add(reward)
    db.flush()
    return reward


def request_reward(
    db: Session,
    *,
    reward_id: int,
    requested_by_user_id: int,
    note: str | None,
) -> RewardRequest:
    reward = db.scalar(select(ExternalReward).where(ExternalReward.id == reward_id))
    if reward is None:
        raise RewardNotFoundError()
    if not reward.is_active:
        raise RewardInactiveError()

    balance = get_scales_balance(db, reward.child_id)
    if balance < reward.cost_scales:
        raise InsufficientScalesError(balance=balance, required=reward.cost_scales)

    reward_request = RewardRequest(
        child_id=reward.child_id,
        reward_id=reward.id,
        reward_title=reward.title,
        cost_scales=reward.cost_scales,
        status=RewardRequestStatus.PENDING,
        requested_by_user_id=requested_by_user_id,
        note=note.strip() if note else None,
    )
    db.add(reward_request)
    db.flush()
    return reward_request


def list_reward_requests_for_child(db: Session, *, child_id: int, limit: int = 100) -> List[RewardRequest]:
    stmt = (
        select(RewardRequest)
        .where(RewardRequest.child_id == child_id)
        .order_by(desc(RewardRequest.requested_at), desc(RewardRequest.id))
        .limit(limit)
    )
    return list(db.scalars(stmt).all())


def get_reward_request_by_id(db: Session, request_id: int) -> RewardRequest | None:
    return db.scalar(select(RewardRequest).where(RewardRequest.id == request_id))


def decide_reward_request(
    db: Session,
    *,
    reward_request: RewardRequest,
    status: RewardRequestStatus,
    decided_by_user_id: int,
    parent_note: str | None,
    expires_at,
) -> RewardRequest:
    if reward_request.status != RewardRequestStatus.PENDING:
        raise RewardRequestStatusError()
    if status not in {RewardRequestStatus.APPROVED, RewardRequestStatus.REFUSED, RewardRequestStatus.EXPIRED}:
        raise RewardRequestStatusError()

    reward_request.status = status
    reward_request.decided_by_user_id = decided_by_user_id
    reward_request.decided_at = datetime.now(timezone.utc)
    reward_request.parent_note = parent_note.strip() if parent_note else None
    reward_request.expires_at = expires_at

    if status == RewardRequestStatus.APPROVED:
        balance = get_scales_balance(db, reward_request.child_id)
        if balance < reward_request.cost_scales:
            raise InsufficientScalesError(balance=balance, required=reward_request.cost_scales)

        create_scale_transaction_if_missing(
            db,
            child_id=reward_request.child_id,
            amount=-abs(reward_request.cost_scales),
            reason=f"Parchemin: {reward_request.reward_title}",
            source_type="reward_request",
            source_id=reward_request.id,
            transaction_type=ScaleTransactionType.SPEND,
            event_key=f"reward_request:{reward_request.id}:approved",
        )
        coupon = RewardCoupon(
            request_id=reward_request.id,
            child_id=reward_request.child_id,
            reward_id=reward_request.reward_id,
            code=_generate_coupon_code(reward_request.id),
            status=ScrollStatus.AVAILABLE,
        )
        db.add(coupon)

    db.add(reward_request)
    db.flush()
    return reward_request


def mark_coupon_used(db: Session, *, coupon: RewardCoupon, used_by_user_id: int) -> RewardRequest:
    reward_request = db.get(RewardRequest, coupon.request_id)
    if reward_request is None:
        raise RewardRequestNotFoundError()
    if reward_request.status != RewardRequestStatus.APPROVED:
        raise RewardRequestStatusError()
    if coupon.status != ScrollStatus.AVAILABLE:
        raise RewardRequestStatusError()

    now = datetime.now(timezone.utc)
    coupon.status = ScrollStatus.USED
    coupon.used_at = now
    reward_request.status = RewardRequestStatus.USED
    reward_request.decided_by_user_id = used_by_user_id
    reward_request.decided_at = now
    db.add(coupon)
    db.add(reward_request)
    db.flush()
    return reward_request


def get_coupon_by_id(db: Session, coupon_id: int) -> RewardCoupon | None:
    return db.get(RewardCoupon, coupon_id)


def get_coupon_for_request(db: Session, request_id: int) -> RewardCoupon | None:
    return db.scalar(select(RewardCoupon).where(RewardCoupon.request_id == request_id))


def _generate_coupon_code(request_id: int) -> str:
    return f"TASKO-{request_id}-{secrets.token_hex(3).upper()}"
