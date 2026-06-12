from __future__ import annotations

from typing import List

from sqlalchemy import desc, func, select
from sqlalchemy.orm import Session

from app.models.gamification import ChildWallet
from app.models.reward import ScaleTransaction, ScaleTransactionType


def get_scales_balance(db: Session, child_id: int) -> int:
    wallet = db.get(ChildWallet, child_id)
    if wallet is not None:
        return wallet.flammeches_balance

    stmt = select(func.coalesce(func.sum(ScaleTransaction.amount), 0)).where(ScaleTransaction.child_id == child_id)
    balance = int(db.scalar(stmt) or 0)
    wallet = ChildWallet(child_id=child_id, flammeches_balance=balance)
    db.add(wallet)
    db.flush()
    return balance


def get_scales_history(db: Session, child_id: int, *, limit: int = 100) -> List[ScaleTransaction]:
    stmt = (
        select(ScaleTransaction)
        .where(ScaleTransaction.child_id == child_id)
        .order_by(desc(ScaleTransaction.created_at), desc(ScaleTransaction.id))
        .limit(limit)
    )
    return list(db.scalars(stmt).all())


def _find_existing(
    db: Session,
    *,
    child_id: int,
    source_type: str,
    source_id: int | None,
    transaction_type: ScaleTransactionType,
    event_key: str,
) -> ScaleTransaction | None:
    return db.scalar(
        select(ScaleTransaction).where(
            ScaleTransaction.child_id == child_id,
            ScaleTransaction.source_type == source_type,
            ScaleTransaction.source_id == source_id,
            ScaleTransaction.transaction_type == transaction_type,
            ScaleTransaction.event_key == event_key,
        )
    )


def create_scale_transaction_if_missing(
    db: Session,
    *,
    child_id: int,
    amount: int,
    reason: str,
    source_type: str,
    source_id: int | None,
    transaction_type: ScaleTransactionType,
    event_key: str,
) -> ScaleTransaction | None:
    existing = _find_existing(
        db,
        child_id=child_id,
        source_type=source_type,
        source_id=source_id,
        transaction_type=transaction_type,
        event_key=event_key,
    )
    if existing is not None:
        return None

    wallet = db.get(ChildWallet, child_id)
    if wallet is None:
        stmt = select(func.coalesce(func.sum(ScaleTransaction.amount), 0)).where(ScaleTransaction.child_id == child_id)
        wallet = ChildWallet(child_id=child_id, flammeches_balance=int(db.scalar(stmt) or 0))
        db.add(wallet)

    transaction = ScaleTransaction(
        child_id=child_id,
        amount=amount,
        reason=reason,
        source_type=source_type,
        source_id=source_id,
        transaction_type=transaction_type,
        event_key=event_key,
    )
    db.add(transaction)
    wallet.flammeches_balance += amount
    db.flush()
    return transaction
