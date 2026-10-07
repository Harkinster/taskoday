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
        CheckConstraint("policy_version > 0", name="ck_reward_grants_positive_policy_version"),
        CheckConstraint("mission_bonus_crystals IN (0,2)", name="ck_reward_grants_mission_bonus_amount"),
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
    policy_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    mission_bonus_crystals: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    trigger_event_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    revoke_event_id: Mapped[int | None] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=True)

    beneficiary = relationship("User", foreign_keys=[beneficiary_user_id])
    trigger_event = relationship("FamilyTaskOccurrenceEvent", foreign_keys=[trigger_event_id])
    revoke_event = relationship("FamilyTaskOccurrenceEvent", foreign_keys=[revoke_event_id])


class RewardResourceGrant(Base):
    """Append-only, cycle-scoped resource component of a reward bundle."""

    __tablename__ = "reward_resource_grants"
    __table_args__ = (
        UniqueConstraint("occurrence_id", "cycle_number", "beneficiary_user_id", "resource_type", "component", name="uq_reward_resource_cycle_beneficiary_component"),
        CheckConstraint("amount > 0", name="ck_reward_resource_positive_amount"),
        CheckConstraint("cycle_number >= 0", name="ck_reward_resource_nonnegative_cycle"),
        CheckConstraint("resource_type IN ('FLAME','CRYSTAL')", name="ck_reward_resource_type"),
        CheckConstraint("component IN ('BASE','MISSION_BONUS')", name="ck_reward_resource_component"),
        CheckConstraint("component = 'BASE' OR resource_type = 'CRYSTAL'", name="ck_reward_resource_bonus_is_crystal"),
        Index("ix_reward_resource_family_beneficiary", "family_id", "beneficiary_user_id"),
        Index("ix_reward_resource_reward_grant", "reward_grant_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False)
    beneficiary_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    reward_grant_id: Mapped[int] = mapped_column(ForeignKey("reward_grants.id", ondelete="RESTRICT"), nullable=False)
    action_id: Mapped[int] = mapped_column(ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False)
    occurrence_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False)
    cycle_number: Mapped[int] = mapped_column(Integer, nullable=False)
    resource_type: Mapped[str] = mapped_column(String(16), nullable=False)
    component: Mapped[str] = mapped_column(String(24), nullable=False)
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    source: Mapped[str] = mapped_column(String(24), nullable=False, default="REWARD_ENGINE_V2")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    revoke_event_id: Mapped[int | None] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=True)
