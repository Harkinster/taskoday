from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.declarative import Base


class RewardResourceSpend(Base):
    __tablename__ = "reward_resource_spends"
    __table_args__ = (
        CheckConstraint("amount > 0", name="ck_resource_spends_positive_amount"),
        CheckConstraint("resource_type IN ('FLAME','CRYSTAL')", name="ck_resource_spends_type"),
        UniqueConstraint("user_id", "idempotency_key", name="uq_resource_spend_user_idempotency"),
        Index("ix_resource_spend_family_user_type", "family_id", "user_id", "resource_type"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    resource_type: Mapped[str] = mapped_column(String(16), nullable=False)
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    purpose_type: Mapped[str] = mapped_column(String(32), nullable=False)
    purpose_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    idempotency_key: Mapped[str] = mapped_column(String(128), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    reversed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    reversal_reason: Mapped[str | None] = mapped_column(String(255), nullable=True)


class WishOffer(Base):
    __tablename__ = "wish_offers"
    __table_args__ = (CheckConstraint("flame_cost > 0", name="ck_wish_offer_positive_cost"), Index("ix_wish_offer_family_active", "family_id", "active"))
    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    title: Mapped[str] = mapped_column(String(120), nullable=False)
    description: Mapped[str | None] = mapped_column(String(500), nullable=True)
    flame_cost: Mapped[int] = mapped_column(Integer, nullable=False)
    active: Mapped[bool] = mapped_column(nullable=False, default=True, server_default="1")
    created_by: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class WishRequest(Base):
    __tablename__ = "wish_requests"
    __table_args__ = (CheckConstraint("status IN ('PENDING','APPROVED','REJECTED','CANCELLED')", name="ck_wish_request_status"), Index("ix_wish_request_family_status", "family_id", "status"))
    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    offer_id: Mapped[int] = mapped_column(ForeignKey("wish_offers.id", ondelete="RESTRICT"), nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    title_snapshot: Mapped[str] = mapped_column(String(120), nullable=False)
    flame_cost: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING", server_default="PENDING")
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    decided_by: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=True)
    decided_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    spend_id: Mapped[int | None] = mapped_column(ForeignKey("reward_resource_spends.id", ondelete="RESTRICT"), nullable=True)
    requester = relationship("User", foreign_keys=[user_id])


class ChestOpen(Base):
    __tablename__ = "chronodria_chest_opens"
    __table_args__ = (
        CheckConstraint("crystal_cost > 0", name="ck_chest_open_positive_cost"),
        CheckConstraint("chest_type IN ('COMMON','RARE','EPIC')", name="ck_chest_open_type"),
        UniqueConstraint("user_id", "idempotency_key", name="uq_chest_open_user_idempotency"),
        Index("ix_chest_open_family_user", "family_id", "user_id"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    chest_type: Mapped[str] = mapped_column(String(16), nullable=False)
    crystal_cost: Mapped[int] = mapped_column(Integer, nullable=False)
    drop_count: Mapped[int] = mapped_column(Integer, nullable=False)
    idempotency_key: Mapped[str] = mapped_column(String(128), nullable=False)
    policy_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    spend_id: Mapped[int] = mapped_column(ForeignKey("reward_resource_spends.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class ChestOpenDrop(Base):
    __tablename__ = "chronodria_chest_open_drops"
    __table_args__ = (UniqueConstraint("chest_open_id", "collectible_key", name="uq_chest_open_drop_key"), CheckConstraint("quantity > 0", name="ck_chest_drop_positive_quantity"))
    id: Mapped[int] = mapped_column(primary_key=True)
    chest_open_id: Mapped[int] = mapped_column(ForeignKey("chronodria_chest_opens.id", ondelete="RESTRICT"), nullable=False, index=True)
    collectible_key: Mapped[str] = mapped_column(String(80), nullable=False)
    quantity: Mapped[int] = mapped_column(Integer, nullable=False)
