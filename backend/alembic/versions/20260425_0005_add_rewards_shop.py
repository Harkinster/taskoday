"""Add rewards shop models (reward, reward_purchase)

Revision ID: 20260425_0005
Revises: 20260425_0004
Create Date: 2026-04-25 17:20:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "20260425_0005"
down_revision: Union[str, Sequence[str], None] = "20260425_0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "rewards",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("cost_points", sa.Integer(), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_rewards_child_profile_id", "rewards", ["child_profile_id"], unique=False)
    op.create_index("ix_rewards_is_active", "rewards", ["is_active"], unique=False)

    op.create_table(
        "reward_purchases",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("reward_id", sa.Integer(), sa.ForeignKey("rewards.id", ondelete="SET NULL"), nullable=True),
        sa.Column("reward_title", sa.String(length=120), nullable=False),
        sa.Column("cost_points", sa.Integer(), nullable=False),
        sa.Column("purchased_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_reward_purchases_child_profile_id", "reward_purchases", ["child_profile_id"], unique=False)
    op.create_index("ix_reward_purchases_reward_id", "reward_purchases", ["reward_id"], unique=False)
    op.create_index("ix_reward_purchases_purchased_at", "reward_purchases", ["purchased_at"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_reward_purchases_purchased_at", table_name="reward_purchases")
    op.drop_index("ix_reward_purchases_reward_id", table_name="reward_purchases")
    op.drop_index("ix_reward_purchases_child_profile_id", table_name="reward_purchases")
    op.drop_table("reward_purchases")

    op.drop_index("ix_rewards_is_active", table_name="rewards")
    op.drop_index("ix_rewards_child_profile_id", table_name="rewards")
    op.drop_table("rewards")
