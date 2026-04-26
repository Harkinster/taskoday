from __future__ import annotations

from datetime import date, datetime
from typing import Optional

from sqlalchemy import Date, DateTime, ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class ChildProfile(Base):
    __tablename__ = "child_profiles"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="CASCADE"), index=True)
    user_id: Mapped[Optional[int]] = mapped_column(ForeignKey("users.id", ondelete="SET NULL"), unique=True)
    display_name: Mapped[str] = mapped_column(String(120))
    birth_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    family = relationship("Family", back_populates="child_profiles")
    user = relationship("User", back_populates="child_profile")
    routines = relationship("Routine", back_populates="child_profile", cascade="all, delete-orphan")
    missions = relationship("Mission", back_populates="child_profile", cascade="all, delete-orphan")
    quests = relationship("Quest", back_populates="child_profile", cascade="all, delete-orphan")
    task_completions = relationship("TaskCompletion", back_populates="child_profile", cascade="all, delete-orphan")
    points_transactions = relationship("PointsTransaction", back_populates="child_profile", cascade="all, delete-orphan")
    rewards = relationship("Reward", back_populates="child_profile", cascade="all, delete-orphan")
    reward_purchases = relationship("RewardPurchase", back_populates="child_profile", cascade="all, delete-orphan")
