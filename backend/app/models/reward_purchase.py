from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class RewardPurchase(Base):
    __tablename__ = "reward_purchases"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    child_profile_id: Mapped[int] = mapped_column(ForeignKey("child_profiles.id", ondelete="CASCADE"), index=True)
    reward_id: Mapped[Optional[int]] = mapped_column(ForeignKey("rewards.id", ondelete="SET NULL"), nullable=True, index=True)
    reward_title: Mapped[str] = mapped_column(String(120), nullable=False)
    cost_points: Mapped[int] = mapped_column(Integer, nullable=False)
    purchased_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        index=True,
    )

    child_profile = relationship("ChildProfile", back_populates="reward_purchases")
    reward = relationship("Reward", back_populates="purchases")
