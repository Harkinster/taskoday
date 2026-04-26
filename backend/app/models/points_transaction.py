from __future__ import annotations

from datetime import date, datetime

from sqlalchemy import Date, DateTime, Enum, ForeignKey, Integer, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.enums import PlanningItemType, PointsTransactionType


class PointsTransaction(Base):
    __tablename__ = "points_transactions"
    __table_args__ = (
        UniqueConstraint(
            "child_profile_id",
            "item_type",
            "item_id",
            "completion_date",
            "transaction_type",
            name="uq_points_transaction_event",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    child_profile_id: Mapped[int] = mapped_column(ForeignKey("child_profiles.id", ondelete="CASCADE"), index=True)
    item_type: Mapped[PlanningItemType] = mapped_column(
        Enum(PlanningItemType, name="planning_item_type"),
        nullable=False,
    )
    item_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    completion_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    transaction_type: Mapped[PointsTransactionType] = mapped_column(
        Enum(PointsTransactionType, name="points_transaction_type"),
        nullable=False,
    )
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    child_profile = relationship("ChildProfile", back_populates="points_transactions")

