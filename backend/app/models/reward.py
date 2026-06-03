import enum
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class RewardRequestStatus(str, enum.Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REFUSED = "refused"
    USED = "used"
    EXPIRED = "expired"


class ScaleTransactionType(str, enum.Enum):
    AWARD = "award"
    REVOKE = "revoke"
    SPEND = "spend"


class ScrollStatus(str, enum.Enum):
    AVAILABLE = "available"
    USED = "used"
    EXPIRED = "expired"
    CANCELLED = "cancelled"


class ExternalReward(Base):
    __tablename__ = "external_rewards"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    cost_scales: Mapped[int] = mapped_column(Integer, nullable=False)
    emoji: Mapped[str] = mapped_column(String(16), default="gift", nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False, index=True)
    created_by_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )


class ScaleTransaction(Base):
    __tablename__ = "scale_transactions"
    __table_args__ = (
        UniqueConstraint(
            "child_id",
            "source_type",
            "source_id",
            "transaction_type",
            "event_key",
            name="uq_scale_transaction_event",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    reason: Mapped[str] = mapped_column(Text, nullable=False)
    source_type: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    source_id: Mapped[int | None] = mapped_column(Integer, nullable=True, index=True)
    transaction_type: Mapped[ScaleTransactionType] = mapped_column(Enum(ScaleTransactionType), nullable=False, index=True)
    event_key: Mapped[str] = mapped_column(String(120), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class RewardRequest(Base):
    __tablename__ = "reward_requests"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    reward_id: Mapped[int | None] = mapped_column(ForeignKey("external_rewards.id", ondelete="SET NULL"), nullable=True, index=True)
    reward_title: Mapped[str] = mapped_column(String(255), nullable=False)
    cost_scales: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[RewardRequestStatus] = mapped_column(
        Enum(RewardRequestStatus),
        default=RewardRequestStatus.PENDING,
        nullable=False,
        index=True,
    )
    requested_by_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)
    decided_by_user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=True, index=True)
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    decided_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    note: Mapped[str | None] = mapped_column(Text, nullable=True)
    parent_note: Mapped[str | None] = mapped_column(Text, nullable=True)


class RewardCoupon(Base):
    __tablename__ = "reward_coupons"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    request_id: Mapped[int] = mapped_column(
        ForeignKey("reward_requests.id", ondelete="CASCADE"),
        nullable=False,
        unique=True,
        index=True,
    )
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    reward_id: Mapped[int | None] = mapped_column(ForeignKey("external_rewards.id", ondelete="SET NULL"), nullable=True, index=True)
    code: Mapped[str] = mapped_column(String(32), nullable=False, unique=True, index=True)
    status: Mapped[ScrollStatus] = mapped_column(Enum(ScrollStatus), default=ScrollStatus.AVAILABLE, nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    used_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
