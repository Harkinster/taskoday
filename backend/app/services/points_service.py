from __future__ import annotations

from typing import List

from sqlalchemy import desc, func, select
from sqlalchemy.orm import Session

from app.models.enums import PlanningItemType, PointsTransactionType
from app.models.points_transaction import PointsTransaction
from app.models.reward_purchase import RewardPurchase


def points_for_item_type(item_type: PlanningItemType) -> int:
    if item_type == PlanningItemType.ROUTINE:
        return 1
    if item_type == PlanningItemType.MISSION:
        return 2
    return 3


def get_points_balance(db: Session, child_id: int) -> int:
    earned_stmt = select(func.coalesce(func.sum(PointsTransaction.amount), 0)).where(
        PointsTransaction.child_profile_id == child_id,
    )
    spent_stmt = select(func.coalesce(func.sum(RewardPurchase.cost_points), 0)).where(
        RewardPurchase.child_profile_id == child_id,
    )
    earned = int(db.scalar(earned_stmt) or 0)
    spent = int(db.scalar(spent_stmt) or 0)
    return earned - spent


def get_points_history(db: Session, child_id: int, *, limit: int = 100) -> List[PointsTransaction]:
    stmt = (
        select(PointsTransaction)
        .where(PointsTransaction.child_profile_id == child_id)
        .order_by(desc(PointsTransaction.created_at), desc(PointsTransaction.id))
        .limit(limit)
    )
    return list(db.scalars(stmt).all())


def _get_existing_event(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    completion_date,
    transaction_type: PointsTransactionType,
) -> PointsTransaction | None:
    stmt = select(PointsTransaction).where(
        PointsTransaction.child_profile_id == child_id,
        PointsTransaction.item_type == item_type,
        PointsTransaction.item_id == item_id,
        PointsTransaction.completion_date == completion_date,
        PointsTransaction.transaction_type == transaction_type,
    )
    return db.scalar(stmt)


def create_award_if_missing(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    completion_date,
    amount: int,
) -> PointsTransaction | None:
    existing = _get_existing_event(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        completion_date=completion_date,
        transaction_type=PointsTransactionType.AWARD,
    )
    if existing is not None:
        return None
    transaction = PointsTransaction(
        child_profile_id=child_id,
        item_type=item_type,
        item_id=item_id,
        completion_date=completion_date,
        transaction_type=PointsTransactionType.AWARD,
        amount=amount,
    )
    db.add(transaction)
    return transaction


def create_revoke_if_missing(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    completion_date,
    amount: int,
) -> PointsTransaction | None:
    existing = _get_existing_event(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        completion_date=completion_date,
        transaction_type=PointsTransactionType.REVOKE,
    )
    if existing is not None:
        return None
    transaction = PointsTransaction(
        child_profile_id=child_id,
        item_type=item_type,
        item_id=item_id,
        completion_date=completion_date,
        transaction_type=PointsTransactionType.REVOKE,
        amount=-abs(amount),
    )
    db.add(transaction)
    return transaction
