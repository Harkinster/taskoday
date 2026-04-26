from __future__ import annotations

from datetime import date, datetime
from typing import Optional

from sqlalchemy import Date, DateTime, ForeignKey, Integer, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class TaskCompletion(Base):
    __tablename__ = "task_completions"
    __table_args__ = (
        UniqueConstraint(
            "child_profile_id",
            "routine_id",
            "completion_date",
            name="uq_task_completion_routine_day",
        ),
        UniqueConstraint(
            "child_profile_id",
            "mission_id",
            "completion_date",
            name="uq_task_completion_mission_day",
        ),
        UniqueConstraint(
            "child_profile_id",
            "quest_id",
            "completion_date",
            name="uq_task_completion_quest_day",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    child_profile_id: Mapped[int] = mapped_column(ForeignKey("child_profiles.id", ondelete="CASCADE"), index=True)
    routine_id: Mapped[Optional[int]] = mapped_column(ForeignKey("routines.id", ondelete="CASCADE"), nullable=True, index=True)
    mission_id: Mapped[Optional[int]] = mapped_column(ForeignKey("missions.id", ondelete="CASCADE"), nullable=True, index=True)
    quest_id: Mapped[Optional[int]] = mapped_column(ForeignKey("quests.id", ondelete="CASCADE"), nullable=True, index=True)
    completion_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    points_awarded: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    completed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    child_profile = relationship("ChildProfile", back_populates="task_completions")
    routine = relationship("Routine", back_populates="completions")
    mission = relationship("Mission", back_populates="completions")
    quest = relationship("Quest", back_populates="completions")

