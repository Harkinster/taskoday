"""Add optional scheduled_date to quests

Revision ID: 20260425_0003
Revises: 20260425_0002
Create Date: 2026-04-25 00:20:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "20260425_0003"
down_revision: Union[str, Sequence[str], None] = "20260425_0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("quests", sa.Column("scheduled_date", sa.Date(), nullable=True))
    op.create_index("ix_quests_scheduled_date", "quests", ["scheduled_date"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_quests_scheduled_date", table_name="quests")
    op.drop_column("quests", "scheduled_date")

