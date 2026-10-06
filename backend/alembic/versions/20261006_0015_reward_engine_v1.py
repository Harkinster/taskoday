"""Add the Taskoday Reward Engine V1 append-only grant ledger.

No historical rewards are backfilled. Downgrade drops the ledger and its history.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261006_0015"
down_revision: Union[str, Sequence[str], None] = "20261006_0014"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "reward_grants",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("beneficiary_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("action_id", sa.Integer(), sa.ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("action_title", sa.String(length=255), nullable=False),
        sa.Column("occurrence_id", sa.Integer(), sa.ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("cycle_number", sa.Integer(), nullable=False),
        sa.Column("scope", sa.String(length=16), nullable=False),
        sa.Column("kind", sa.String(length=16), nullable=False),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("trigger_event_id", sa.Integer(), sa.ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("revoke_event_id", sa.Integer(), sa.ForeignKey("family_task_occurrence_events.id", ondelete="RESTRICT"), nullable=True),
        sa.CheckConstraint("points > 0", name="ck_reward_grants_positive_points"),
        sa.CheckConstraint("cycle_number >= 0", name="ck_reward_grants_nonnegative_cycle"),
        sa.CheckConstraint(
            "(scope = 'PERSONAL' AND kind IN ('ROUTINE','MISSION')) OR "
            "(scope = 'HOUSE' AND kind IN ('ROUTINE','MISSION','QUEST'))",
            name="ck_reward_grants_scope_kind",
        ),
        sa.UniqueConstraint("occurrence_id", "cycle_number", "beneficiary_user_id", name="uq_reward_grant_occurrence_cycle_beneficiary"),
    )
    op.create_index("ix_reward_grants_family_beneficiary", "reward_grants", ["family_id", "beneficiary_user_id"])
    op.create_index("ix_reward_grants_trigger_event", "reward_grants", ["trigger_event_id"])


def downgrade() -> None:
    op.drop_index("ix_reward_grants_trigger_event", table_name="reward_grants")
    op.drop_index("ix_reward_grants_family_beneficiary", table_name="reward_grants")
    op.drop_table("reward_grants")
