"""Add Nest gamification models

Revision ID: 20260602_0002
Revises: 20260602_0001
Create Date: 2026-06-02 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260602_0002"
down_revision: Union[str, Sequence[str], None] = "20260602_0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


chest_type_enum = sa.Enum("SIMPLE", "RARE", name="chesttype")
chest_status_enum = sa.Enum("UNOPENED", "OPENED", name="cheststatus")
child_egg_status_enum = sa.Enum("LOCKED", "AVAILABLE", "READY", "HATCHED", name="childeggstatus")
dragon_stage_enum = sa.Enum("BABY", "YOUNG", "GUARDIAN", name="dragonstage")
scroll_status_enum = sa.Enum("AVAILABLE", "USED", "EXPIRED", "CANCELLED", name="scrollstatus")


def upgrade() -> None:
    op.create_table(
        "guardian_progress",
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("guardian_xp", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("chest_points", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("opened_chests", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )

    op.create_table(
        "child_wallet",
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("flammeches_balance", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )

    op.create_table(
        "eggs",
        sa.Column("key", sa.String(length=80), primary_key=True),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
    )

    op.create_table(
        "dragons",
        sa.Column("key", sa.String(length=80), primary_key=True),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
    )

    op.create_table(
        "chest_inventory",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("chest_type", chest_type_enum, nullable=False),
        sa.Column("status", chest_status_enum, nullable=False),
        sa.Column("source_type", sa.String(length=50), nullable=True),
        sa.Column("source_id", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("opened_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_chest_inventory_child_id", "chest_inventory", ["child_id"], unique=False)
    op.create_index("ix_chest_inventory_chest_type", "chest_inventory", ["chest_type"], unique=False)
    op.create_index("ix_chest_inventory_status", "chest_inventory", ["status"], unique=False)

    op.create_table(
        "item_inventory",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("item_key", sa.String(length=80), nullable=False),
        sa.Column("quantity", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint("child_id", "item_key", name="uq_item_inventory_child_item"),
    )
    op.create_index("ix_item_inventory_child_id", "item_inventory", ["child_id"], unique=False)
    op.create_index("ix_item_inventory_item_key", "item_inventory", ["item_key"], unique=False)

    op.create_table(
        "child_eggs",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("egg_key", sa.String(length=80), sa.ForeignKey("eggs.key", ondelete="CASCADE"), nullable=False),
        sa.Column("status", child_egg_status_enum, nullable=False),
        sa.Column("obtained_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("hatched_at", sa.DateTime(timezone=True), nullable=True),
        sa.UniqueConstraint("child_id", "egg_key", name="uq_child_eggs_child_egg"),
    )
    op.create_index("ix_child_eggs_child_id", "child_eggs", ["child_id"], unique=False)
    op.create_index("ix_child_eggs_egg_key", "child_eggs", ["egg_key"], unique=False)

    op.create_table(
        "child_dragons",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("child_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("dragon_key", sa.String(length=80), sa.ForeignKey("dragons.key", ondelete="CASCADE"), nullable=False),
        sa.Column("stage", dragon_stage_enum, nullable=False),
        sa.Column("unlocked_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint("child_id", "dragon_key", name="uq_child_dragons_child_dragon"),
    )
    op.create_index("ix_child_dragons_child_id", "child_dragons", ["child_id"], unique=False)
    op.create_index("ix_child_dragons_dragon_key", "child_dragons", ["dragon_key"], unique=False)

    op.add_column(
        "reward_coupons",
        sa.Column("status", scroll_status_enum, nullable=False, server_default="AVAILABLE"),
    )
    op.create_index("ix_reward_coupons_status", "reward_coupons", ["status"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_reward_coupons_status", table_name="reward_coupons")
    op.drop_column("reward_coupons", "status")

    op.drop_index("ix_child_dragons_dragon_key", table_name="child_dragons")
    op.drop_index("ix_child_dragons_child_id", table_name="child_dragons")
    op.drop_table("child_dragons")

    op.drop_index("ix_child_eggs_egg_key", table_name="child_eggs")
    op.drop_index("ix_child_eggs_child_id", table_name="child_eggs")
    op.drop_table("child_eggs")

    op.drop_index("ix_item_inventory_item_key", table_name="item_inventory")
    op.drop_index("ix_item_inventory_child_id", table_name="item_inventory")
    op.drop_table("item_inventory")

    op.drop_index("ix_chest_inventory_status", table_name="chest_inventory")
    op.drop_index("ix_chest_inventory_chest_type", table_name="chest_inventory")
    op.drop_index("ix_chest_inventory_child_id", table_name="chest_inventory")
    op.drop_table("chest_inventory")

    op.drop_table("dragons")
    op.drop_table("eggs")
    op.drop_table("child_wallet")
    op.drop_table("guardian_progress")
