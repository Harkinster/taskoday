from __future__ import annotations

from datetime import datetime
from typing import Optional

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.enums import DayPart, RoutineFrequency


class Routine(Base):
    __tablename__ = "routines"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    child_profile_id: Mapped[int] = mapped_column(ForeignKey("child_profiles.id", ondelete="CASCADE"), index=True)
    title: Mapped[str] = mapped_column(String(120))
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    day_part: Mapped[DayPart] = mapped_column(Enum(DayPart, name="day_part"), nullable=False)
    frequency: Mapped[RoutineFrequency] = mapped_column(
        Enum(RoutineFrequency, name="routine_frequency"),
        default=RoutineFrequency.DAILY,
        nullable=False,
    )
    # CSV of ISO weekdays (1..7), only used when frequency = SELECTED_DAYS.
    days_of_week: Mapped[Optional[str]] = mapped_column(String(32), nullable=True)
    points_reward: Mapped[int] = mapped_column(Integer, default=1, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
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

    child_profile = relationship("ChildProfile", back_populates="routines")
    completions = relationship("TaskCompletion", back_populates="routine", cascade="all, delete-orphan")

