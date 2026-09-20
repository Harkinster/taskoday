"""Add numeric interval to family task recurrence.

Revision ID: 20260920_0008
Revises: 20260829_0007
Create Date: 2026-09-20 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260920_0008"
down_revision: Union[str, Sequence[str], None] = "20260829_0007"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("family_tasks") as batch_op:
        batch_op.add_column(sa.Column("recurrence_interval", sa.Integer(), nullable=False, server_default=sa.text("1")))
        batch_op.create_check_constraint(
            "ck_family_tasks_recurrence_interval_positive",
            sa.text("recurrence_interval >= 1"),
        )


def downgrade() -> None:
    with op.batch_alter_table("family_tasks") as batch_op:
        batch_op.drop_constraint("ck_family_tasks_recurrence_interval_positive", type_="check")
        batch_op.drop_column("recurrence_interval")
