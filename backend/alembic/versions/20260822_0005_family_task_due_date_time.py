"""Add explicit date and optional time to family tasks

Revision ID: 20260822_0005
Revises: 20260821_0004
Create Date: 2026-08-22 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260822_0005"
down_revision: Union[str, Sequence[str], None] = "20260821_0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("family_tasks", sa.Column("due_date", sa.Date(), nullable=True))
    op.add_column("family_tasks", sa.Column("due_time", sa.Time(), nullable=True))
    op.create_index(op.f("ix_family_tasks_due_date"), "family_tasks", ["due_date"], unique=False)

    dialect_name = op.get_bind().dialect.name
    if dialect_name in {"mysql", "mariadb"}:
        op.execute(
            "UPDATE family_tasks SET due_date = DATE(due_at), due_time = TIME(due_at) "
            "WHERE due_at IS NOT NULL"
        )
    else:
        op.execute(
            "UPDATE family_tasks SET due_date = date(due_at), due_time = time(due_at) "
            "WHERE due_at IS NOT NULL"
        )


def downgrade() -> None:
    op.drop_index(op.f("ix_family_tasks_due_date"), table_name="family_tasks")
    op.drop_column("family_tasks", "due_time")
    op.drop_column("family_tasks", "due_date")
