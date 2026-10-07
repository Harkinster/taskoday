"""Add resource spend ledger, family wishes, and persisted Chronodria chest opens."""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261007_0017"
down_revision: Union[str, Sequence[str], None] = "20261006_0016"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "reward_resource_spends",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("resource_type", sa.String(16), nullable=False),
        sa.Column("amount", sa.Integer(), nullable=False),
        sa.Column("purpose_type", sa.String(32), nullable=False),
        sa.Column("purpose_id", sa.Integer(), nullable=True),
        sa.Column("idempotency_key", sa.String(128), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("reversed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("reversal_reason", sa.String(255), nullable=True),
        sa.CheckConstraint("amount > 0", name="ck_resource_spends_positive_amount"),
        sa.CheckConstraint("resource_type IN ('FLAME','CRYSTAL')", name="ck_resource_spends_type"),
        sa.UniqueConstraint("user_id", "idempotency_key", name="uq_resource_spend_user_idempotency"),
    )
    op.create_index("ix_resource_spend_family_user_type", "reward_resource_spends", ["family_id", "user_id", "resource_type"])
    op.create_table(
        "wish_offers",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("title", sa.String(120), nullable=False),
        sa.Column("description", sa.String(500), nullable=True),
        sa.Column("flame_cost", sa.Integer(), nullable=False),
        sa.Column("active", sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column("created_by", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.CheckConstraint("flame_cost > 0", name="ck_wish_offer_positive_cost"),
    )
    op.create_index("ix_wish_offer_family_active", "wish_offers", ["family_id", "active"])
    op.create_table(
        "wish_requests",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("offer_id", sa.Integer(), sa.ForeignKey("wish_offers.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("title_snapshot", sa.String(120), nullable=False),
        sa.Column("flame_cost", sa.Integer(), nullable=False),
        sa.Column("status", sa.String(16), server_default="PENDING", nullable=False),
        sa.Column("requested_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("decided_by", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=True),
        sa.Column("decided_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("spend_id", sa.Integer(), sa.ForeignKey("reward_resource_spends.id", ondelete="RESTRICT"), nullable=True),
        sa.CheckConstraint("status IN ('PENDING','APPROVED','REJECTED','CANCELLED')", name="ck_wish_request_status"),
    )
    op.create_index("ix_wish_request_family_status", "wish_requests", ["family_id", "status"])
    op.create_table(
        "chronodria_chest_opens",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("chest_type", sa.String(16), nullable=False),
        sa.Column("crystal_cost", sa.Integer(), nullable=False),
        sa.Column("drop_count", sa.Integer(), nullable=False),
        sa.Column("idempotency_key", sa.String(128), nullable=False),
        sa.Column("policy_version", sa.Integer(), server_default="1", nullable=False),
        sa.Column("spend_id", sa.Integer(), sa.ForeignKey("reward_resource_spends.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.CheckConstraint("crystal_cost > 0", name="ck_chest_open_positive_cost"),
        sa.CheckConstraint("chest_type IN ('COMMON','RARE','EPIC')", name="ck_chest_open_type"),
        sa.UniqueConstraint("user_id", "idempotency_key", name="uq_chest_open_user_idempotency"),
    )
    op.create_index("ix_chest_open_family_user", "chronodria_chest_opens", ["family_id", "user_id"])
    op.create_table(
        "chronodria_chest_open_drops",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("chest_open_id", sa.Integer(), sa.ForeignKey("chronodria_chest_opens.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("collectible_key", sa.String(80), nullable=False),
        sa.Column("quantity", sa.Integer(), nullable=False),
        sa.CheckConstraint("quantity > 0", name="ck_chest_drop_positive_quantity"),
        sa.UniqueConstraint("chest_open_id", "collectible_key", name="uq_chest_open_drop_key"),
    )
    op.create_index("ix_chronodria_chest_open_drops_chest_open_id", "chronodria_chest_open_drops", ["chest_open_id"])


def downgrade() -> None:
    op.drop_index("ix_chronodria_chest_open_drops_chest_open_id", table_name="chronodria_chest_open_drops")
    op.drop_table("chronodria_chest_open_drops")
    op.drop_index("ix_chest_open_family_user", table_name="chronodria_chest_opens")
    op.drop_table("chronodria_chest_opens")
    op.drop_index("ix_wish_request_family_status", table_name="wish_requests")
    op.drop_table("wish_requests")
    op.drop_index("ix_wish_offer_family_active", table_name="wish_offers")
    op.drop_table("wish_offers")
    op.drop_index("ix_resource_spend_family_user_type", table_name="reward_resource_spends")
    op.drop_table("reward_resource_spends")
