"""Add a reversible archival marker without removing family history."""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261003_0010"
down_revision: Union[str, Sequence[str], None] = "20260920_0009"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("families") as batch_op:
        batch_op.add_column(sa.Column("archived_at", sa.DateTime(timezone=True), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table("families") as batch_op:
        batch_op.drop_column("archived_at")
