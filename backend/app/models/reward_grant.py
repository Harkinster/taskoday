from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.declarative import Base


class RewardGrant(Base):
    """Immutable Taskoday reward award; revocation preserves the original grant."""

    __tablename__ = "reward_grants"
    __table_args__ = (
        UniqueConstraint(
            "occurrence_id", "cycle_number", "beneficiary_user_id",
            name="uq_reward_grant_occurrence_cycle_beneficiary",
        ),
        CheckConstraint("points > 0", name="ck_reward_grants_positive_points"),
        CheckConstraint("cycle_number >= 0", name="ck_reward_grants_nonnegative_cycle"),
        CheckConstraint(
            "(scope = 'PERSONAL' AND kind IN ('ROUTINE','MISSION')) OR "
            "(scope = 'HOUSE' AND kind IN ('ROUTINE','MISSION','QUEST'))",
            name="ck_reward_grants_scope_kind",
        ),
        Index("ix_reward_grants_family_beneficiary", "family_id", "beneficiary_user_id"),
        Index("ix_reward_grants_trigger_event", "trigger_event_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    beneficiary_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    action_id: Mapped[int] = mapped_column(ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False)
    action_title: Mapped[str] = mapped_column(String(255), nullable=False)
    occurrence_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False)
    cycle_number: Mapped[int] = mapped_column(Integer, nullable=False)
    scope: Mapped[str] = mapped_column(String(16), nullable=False)
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    points: Mapped[int] = mapped_column(Integer, nullable=False)
    trigger_event_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    revoke_event_id: Mapped[int | None] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=True)

    beneficiary = relationship("User", foreign_keys=[beneficiary_user_id])
    trigger_event = relationship("FamilyTaskOccurrenceEvent", foreign_keys=[trigger_event_id])
    revoke_event = relationship("FamilyTaskOccurrenceEvent", foreign_keys=[revoke_event_id])
