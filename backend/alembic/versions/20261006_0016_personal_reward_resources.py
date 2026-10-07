"""Add personal reward resource ledger; intentionally no resource backfill."""

from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = "20261006_0016"
down_revision: Union[str, Sequence[str], None] = "20261006_0015"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Existing point values remain untouched and are tagged as the historical policy.
    with op.batch_alter_table("reward_grants") as batch:
        batch.add_column(sa.Column("policy_version", sa.Integer(), server_default="1", nullable=False))
        batch.add_column(sa.Column("mission_bonus_crystals", sa.Integer(), server_default="0", nullable=False))
        batch.create_check_constraint("ck_reward_grants_positive_policy_version", "policy_version > 0")
        batch.create_check_constraint("ck_reward_grants_mission_bonus_amount", "mission_bonus_crystals IN (0,2)")
    op.create_table(
        "reward_resource_grants",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("beneficiary_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("reward_grant_id", sa.Integer(), sa.ForeignKey("reward_grants.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("action_id", sa.Integer(), sa.ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("occurrence_id", sa.Integer(), sa.ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("cycle_number", sa.Integer(), nullable=False),
        sa.Column("resource_type", sa.String(16), nullable=False),
        sa.Column("component", sa.String(24), nullable=False),
        sa.Column("amount", sa.Integer(), nullable=False),
        sa.Column("source", sa.String(24), server_default="REWARD_ENGINE_V2", nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("revoke_event_id", sa.Integer(), sa.ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=True),
        sa.CheckConstraint("amount > 0", name="ck_reward_resource_positive_amount"),
        sa.CheckConstraint("cycle_number >= 0", name="ck_reward_resource_nonnegative_cycle"),
        sa.CheckConstraint("resource_type IN ('FLAME','CRYSTAL')", name="ck_reward_resource_type"),
        sa.CheckConstraint("component IN ('BASE','MISSION_BONUS')", name="ck_reward_resource_component"),
        sa.CheckConstraint("component = 'BASE' OR resource_type = 'CRYSTAL'", name="ck_reward_resource_bonus_is_crystal"),
        sa.UniqueConstraint("occurrence_id", "cycle_number", "beneficiary_user_id", "resource_type", "component", name="uq_reward_resource_cycle_beneficiary_component"),
    )
    op.create_index("ix_reward_resource_family_beneficiary", "reward_resource_grants", ["family_id", "beneficiary_user_id"])
    op.create_index("ix_reward_resource_reward_grant", "reward_resource_grants", ["reward_grant_id"])


def downgrade() -> None:
    op.drop_index("ix_reward_resource_reward_grant", table_name="reward_resource_grants")
    op.drop_index("ix_reward_resource_family_beneficiary", table_name="reward_resource_grants")
    op.drop_table("reward_resource_grants")
    with op.batch_alter_table("reward_grants") as batch:
        batch.drop_constraint("ck_reward_grants_mission_bonus_amount", type_="check")
        batch.drop_constraint("ck_reward_grants_positive_policy_version", type_="check")
        batch.drop_column("mission_bonus_crystals")
        batch.drop_column("policy_version")
