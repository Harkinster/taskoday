"""Add external rewards, scale transactions, reward requests and coupons

Revision ID: 20260602_0001
Revises: 0003_parent_birth_date
Create Date: 2026-06-02 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260602_0001"
down_revision: Union[str, Sequence[str], None] = "0003_parent_birth_date"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


reward_request_status_enum = sa.Enum(
    "PENDING",
    "APPROVED",
    "REFUSED",
    "USED",
    "EXPIRED",
    name="rewardrequeststatus",
)

scale_transaction_type_enum = sa.Enum(
    "AWARD",
    "REVOKE",
    "SPEND",
    name="scaletransactiontype",
)


def upgrade() -> None:
    op.create_table(
        "external_rewards",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("cost_scales", sa.Integer(), nullable=False),
        sa.Column("emoji", sa.String(length=16), nullable=False, server_default="gift"),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_by_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_external_rewards_child_id", "external_rewards", ["child_id"], unique=False)
    op.create_index("ix_external_rewards_created_by_user_id", "external_rewards", ["created_by_user_id"], unique=False)
    op.create_index("ix_external_rewards_is_active", "external_rewards", ["is_active"], unique=False)

    op.create_table(
        "scale_transactions",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("amount", sa.Integer(), nullable=False),
        sa.Column("reason", sa.Text(), nullable=False),
        sa.Column("source_type", sa.String(length=50), nullable=False),
        sa.Column("source_id", sa.Integer(), nullable=True),
        sa.Column("transaction_type", scale_transaction_type_enum, nullable=False),
        sa.Column("event_key", sa.String(length=120), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint(
            "child_id",
            "source_type",
            "source_id",
            "transaction_type",
            "event_key",
            name="uq_scale_transaction_event",
        ),
    )
    op.create_index("ix_scale_transactions_child_id", "scale_transactions", ["child_id"], unique=False)
    op.create_index("ix_scale_transactions_source_type", "scale_transactions", ["source_type"], unique=False)
    op.create_index("ix_scale_transactions_source_id", "scale_transactions", ["source_id"], unique=False)
    op.create_index("ix_scale_transactions_transaction_type", "scale_transactions", ["transaction_type"], unique=False)
    op.create_index("ix_scale_transactions_event_key", "scale_transactions", ["event_key"], unique=False)

    op.create_table(
        "reward_requests",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("reward_id", sa.Integer(), sa.ForeignKey("external_rewards.id", ondelete="SET NULL"), nullable=True),
        sa.Column("reward_title", sa.String(length=255), nullable=False),
        sa.Column("cost_scales", sa.Integer(), nullable=False),
        sa.Column("status", reward_request_status_enum, nullable=False),
        sa.Column("requested_by_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("decided_by_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=True),
        sa.Column("requested_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("decided_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("note", sa.Text(), nullable=True),
        sa.Column("parent_note", sa.Text(), nullable=True),
    )
    op.create_index("ix_reward_requests_child_id", "reward_requests", ["child_id"], unique=False)
    op.create_index("ix_reward_requests_reward_id", "reward_requests", ["reward_id"], unique=False)
    op.create_index("ix_reward_requests_status", "reward_requests", ["status"], unique=False)

    op.create_table(
        "reward_coupons",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("request_id", sa.Integer(), sa.ForeignKey("reward_requests.id", ondelete="CASCADE"), nullable=False),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("reward_id", sa.Integer(), sa.ForeignKey("external_rewards.id", ondelete="SET NULL"), nullable=True),
        sa.Column("code", sa.String(length=32), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("used_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_reward_coupons_request_id", "reward_coupons", ["request_id"], unique=True)
    op.create_index("ix_reward_coupons_child_id", "reward_coupons", ["child_id"], unique=False)
    op.create_index("ix_reward_coupons_reward_id", "reward_coupons", ["reward_id"], unique=False)
    op.create_index("ix_reward_coupons_code", "reward_coupons", ["code"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_reward_coupons_code", table_name="reward_coupons")
    op.drop_index("ix_reward_coupons_reward_id", table_name="reward_coupons")
    op.drop_index("ix_reward_coupons_child_id", table_name="reward_coupons")
    op.drop_index("ix_reward_coupons_request_id", table_name="reward_coupons")
    op.drop_table("reward_coupons")

    op.drop_index("ix_reward_requests_status", table_name="reward_requests")
    op.drop_index("ix_reward_requests_reward_id", table_name="reward_requests")
    op.drop_index("ix_reward_requests_child_id", table_name="reward_requests")
    op.drop_table("reward_requests")

    op.drop_index("ix_scale_transactions_event_key", table_name="scale_transactions")
    op.drop_index("ix_scale_transactions_transaction_type", table_name="scale_transactions")
    op.drop_index("ix_scale_transactions_source_id", table_name="scale_transactions")
    op.drop_index("ix_scale_transactions_source_type", table_name="scale_transactions")
    op.drop_index("ix_scale_transactions_child_id", table_name="scale_transactions")
    op.drop_table("scale_transactions")

    op.drop_index("ix_external_rewards_is_active", table_name="external_rewards")
    op.drop_index("ix_external_rewards_created_by_user_id", table_name="external_rewards")
    op.drop_index("ix_external_rewards_child_id", table_name="external_rewards")
    op.drop_table("external_rewards")
