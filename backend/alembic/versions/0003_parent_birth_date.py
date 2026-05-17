"""add parent birth_date on users

Revision ID: 0003_parent_birth_date
Revises: 0002_auth_contract_schema
Create Date: 2026-05-16 16:40:00.000000

"""

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = "0003_parent_birth_date"
down_revision = "0002_auth_contract_schema"
branch_labels = None
depends_on = None


def _has_column(inspector: sa.Inspector, table_name: str, column_name: str) -> bool:
    return any(column["name"] == column_name for column in inspector.get_columns(table_name))


def upgrade() -> None:
    inspector = sa.inspect(op.get_bind())
    if not _has_column(inspector, "users", "birth_date"):
        op.add_column("users", sa.Column("birth_date", sa.Date(), nullable=True))


def downgrade() -> None:
    inspector = sa.inspect(op.get_bind())
    if _has_column(inspector, "users", "birth_date"):
        with op.batch_alter_table("users") as batch_op:
            batch_op.drop_column("birth_date")
