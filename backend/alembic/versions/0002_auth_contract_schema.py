"""auth contract schema updates

Revision ID: 0002_auth_contract_schema
Revises: 0001_initial
Create Date: 2026-05-16 16:25:00.000000

"""

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = "0002_auth_contract_schema"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


def _has_column(inspector: sa.Inspector, table_name: str, column_name: str) -> bool:
    return any(column["name"] == column_name for column in inspector.get_columns(table_name))


def _has_index(inspector: sa.Inspector, table_name: str, index_name: str) -> bool:
    return any(index["name"] == index_name for index in inspector.get_indexes(table_name))


def _has_unique_constraint(inspector: sa.Inspector, table_name: str, constraint_name: str) -> bool:
    return any(
        constraint.get("name") == constraint_name for constraint in inspector.get_unique_constraints(table_name)
    )


def upgrade() -> None:
    bind = op.get_bind()
    inspector = sa.inspect(bind)

    if not _has_column(inspector, "users", "is_active"):
        op.add_column(
            "users",
            sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        )

    if not _has_column(inspector, "child_profiles", "birth_date"):
        op.add_column("child_profiles", sa.Column("birth_date", sa.Date(), nullable=True))

    if not _has_index(inspector, "families", "uq_families_name") and not _has_unique_constraint(
        inspector, "families", "uq_families_name"
    ):
        op.create_index("uq_families_name", "families", ["name"], unique=True)


def downgrade() -> None:
    bind = op.get_bind()
    inspector = sa.inspect(bind)

    if _has_index(inspector, "families", "uq_families_name"):
        op.drop_index("uq_families_name", table_name="families")

    inspector = sa.inspect(op.get_bind())
    if _has_column(inspector, "child_profiles", "birth_date"):
        with op.batch_alter_table("child_profiles") as batch_op:
            batch_op.drop_column("birth_date")

    inspector = sa.inspect(op.get_bind())
    if _has_column(inspector, "users", "is_active"):
        with op.batch_alter_table("users") as batch_op:
            batch_op.drop_column("is_active")
