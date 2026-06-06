"""Complete Nest backend currencies, evolution and companion state

Revision ID: 20260606_0003
Revises: 20260602_0002
Create Date: 2026-06-06 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260606_0003"
down_revision: Union[str, Sequence[str], None] = "20260602_0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "child_wallet",
        sa.Column("crystals_balance", sa.Integer(), nullable=False, server_default="0"),
    )
    op.add_column(
        "child_eggs",
        sa.Column("egg_state", sa.String(length=20), nullable=False, server_default="sleeping"),
    )
    op.add_column(
        "child_eggs",
        sa.Column("progress", sa.Integer(), nullable=False, server_default="0"),
    )
    op.add_column(
        "child_dragons",
        sa.Column("progress", sa.Integer(), nullable=False, server_default="0"),
    )
    op.add_column(
        "child_dragons",
        sa.Column("active_companion", sa.Boolean(), nullable=False, server_default=sa.text("0")),
    )
    op.create_index("ix_child_dragons_active_companion", "child_dragons", ["active_companion"], unique=False)
    op.execute(
        "UPDATE child_eggs SET egg_state='hatching', progress=100 "
        "WHERE status IN ('READY','HATCHED')"
    )
    op.execute("UPDATE child_dragons SET progress=100 WHERE stage='GUARDIAN'")

    if op.get_bind().dialect.name in {"mysql", "mariadb"}:
        op.execute(
            "ALTER TABLE chest_inventory MODIFY chest_type "
            "ENUM('SIMPLE','RARE','EPIC') NOT NULL"
        )
        op.execute(
            "ALTER TABLE child_dragons MODIFY stage "
            "ENUM('BABY','YOUNG','MEDIUM','LARGE','LEGENDARY','GUARDIAN') NOT NULL"
        )


def downgrade() -> None:
    if op.get_bind().dialect.name in {"mysql", "mariadb"}:
        op.execute("UPDATE chest_inventory SET chest_type='RARE' WHERE chest_type='EPIC'")
        op.execute(
            "UPDATE child_dragons SET stage='GUARDIAN' "
            "WHERE stage IN ('MEDIUM','LARGE','LEGENDARY')"
        )
        op.execute(
            "ALTER TABLE child_dragons MODIFY stage "
            "ENUM('BABY','YOUNG','GUARDIAN') NOT NULL"
        )
        op.execute(
            "ALTER TABLE chest_inventory MODIFY chest_type "
            "ENUM('SIMPLE','RARE') NOT NULL"
        )

    op.drop_index("ix_child_dragons_active_companion", table_name="child_dragons")
    op.drop_column("child_dragons", "active_companion")
    op.drop_column("child_dragons", "progress")
    op.drop_column("child_eggs", "progress")
    op.drop_column("child_eggs", "egg_state")
    op.drop_column("child_wallet", "crystals_balance")
