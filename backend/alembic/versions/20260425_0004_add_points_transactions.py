"""Add points transactions table

Revision ID: 20260425_0004
Revises: 20260425_0003
Create Date: 2026-04-25 00:30:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "20260425_0004"
down_revision: Union[str, Sequence[str], None] = "20260425_0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


planning_item_type_enum = sa.Enum(
    "routine",
    "mission",
    "quest",
    name="planning_item_type",
)

points_transaction_type_enum = sa.Enum(
    "AWARD",
    "REVOKE",
    name="points_transaction_type",
)


def upgrade() -> None:
    op.create_table(
        "points_transactions",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("item_type", planning_item_type_enum, nullable=False),
        sa.Column("item_id", sa.Integer(), nullable=False),
        sa.Column("completion_date", sa.Date(), nullable=False),
        sa.Column("transaction_type", points_transaction_type_enum, nullable=False),
        sa.Column("amount", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint(
            "child_profile_id",
            "item_type",
            "item_id",
            "completion_date",
            "transaction_type",
            name="uq_points_transaction_event",
        ),
    )
    op.create_index("ix_points_transactions_child_profile_id", "points_transactions", ["child_profile_id"], unique=False)
    op.create_index("ix_points_transactions_item_id", "points_transactions", ["item_id"], unique=False)
    op.create_index("ix_points_transactions_completion_date", "points_transactions", ["completion_date"], unique=False)
    op.create_index("ix_points_transactions_created_at", "points_transactions", ["created_at"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_points_transactions_created_at", table_name="points_transactions")
    op.drop_index("ix_points_transactions_completion_date", table_name="points_transactions")
    op.drop_index("ix_points_transactions_item_id", table_name="points_transactions")
    op.drop_index("ix_points_transactions_child_profile_id", table_name="points_transactions")
    op.drop_table("points_transactions")

