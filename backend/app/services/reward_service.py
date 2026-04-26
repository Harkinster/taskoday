from __future__ import annotations

from typing import List, Optional

from sqlalchemy import desc, select
from sqlalchemy.orm import Session

from app.models.child_profile import ChildProfile
from app.models.reward import Reward
from app.models.reward_purchase import RewardPurchase
from app.services.points_service import get_points_balance


class RewardNotFoundError(Exception):
    pass


class RewardInactiveError(Exception):
    pass


class InsufficientPointsError(Exception):
    def __init__(self, balance: int, required: int) -> None:
        self.balance = balance
        self.required = required
        super().__init__(f"Insufficient points: balance={balance}, required={required}")


def create_reward(
    db: Session,
    *,
    child_id: int,
    title: str,
    description: Optional[str],
    cost_points: int,
    is_active: bool,
) -> Reward:
    reward = Reward(
        child_profile_id=child_id,
        title=title.strip(),
        description=description,
        cost_points=cost_points,
        is_active=is_active,
    )
    db.add(reward)
    db.flush()
    return reward


def list_rewards_for_child(db: Session, *, child_id: int, active_only: bool = True) -> List[Reward]:
    stmt = select(Reward).where(Reward.child_profile_id == child_id)
    if active_only:
        stmt = stmt.where(Reward.is_active.is_(True))
    stmt = stmt.order_by(Reward.cost_points.asc(), Reward.id.asc())
    return list(db.scalars(stmt).all())


def get_reward_by_id(db: Session, reward_id: int) -> Optional[Reward]:
    return db.scalar(select(Reward).where(Reward.id == reward_id))


def update_reward(
    db: Session,
    *,
    reward: Reward,
    title: Optional[str] = None,
    description: Optional[str] = None,
    description_set: bool = False,
    cost_points: Optional[int] = None,
    is_active: Optional[bool] = None,
) -> Reward:
    if title is not None:
        reward.title = title.strip()
    if description_set:
        reward.description = description
    if cost_points is not None:
        reward.cost_points = cost_points
    if is_active is not None:
        reward.is_active = is_active
    db.add(reward)
    db.flush()
    return reward


def list_reward_purchases_for_child(db: Session, *, child_id: int, limit: int = 100) -> List[RewardPurchase]:
    stmt = (
        select(RewardPurchase)
        .where(RewardPurchase.child_profile_id == child_id)
        .order_by(desc(RewardPurchase.purchased_at), desc(RewardPurchase.id))
        .limit(limit)
    )
    return list(db.scalars(stmt).all())


def purchase_reward(db: Session, *, reward_id: int) -> RewardPurchase:
    reward = db.scalar(select(Reward).where(Reward.id == reward_id).with_for_update())
    if reward is None:
        raise RewardNotFoundError()
    if not reward.is_active:
        raise RewardInactiveError()

    child = db.scalar(select(ChildProfile).where(ChildProfile.id == reward.child_profile_id).with_for_update())
    if child is None:
        raise RewardNotFoundError()

    balance = get_points_balance(db, child.id)
    if balance < reward.cost_points:
        raise InsufficientPointsError(balance=balance, required=reward.cost_points)

    purchase = RewardPurchase(
        child_profile_id=child.id,
        reward_id=reward.id,
        reward_title=reward.title,
        cost_points=reward.cost_points,
    )
    db.add(purchase)
    db.flush()
    return purchase
