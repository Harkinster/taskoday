"""Add family parent invites

Revision ID: 20260823_0006
Revises: 20260822_0005
Create Date: 2026-08-23 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260823_0006"
down_revision: Union[str, Sequence[str], None] = "20260822_0005"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "family_invites",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("family_id", sa.Integer(), nullable=False),
        sa.Column("created_by_user_id", sa.Integer(), nullable=False),
        sa.Column("token_hash", sa.String(length=64), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("accepted_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("accepted_by_user_id", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["accepted_by_user_id"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_family_invites_accepted_by_user_id"), "family_invites", ["accepted_by_user_id"])
    op.create_index(op.f("ix_family_invites_created_by_user_id"), "family_invites", ["created_by_user_id"])
    op.create_index(op.f("ix_family_invites_expires_at"), "family_invites", ["expires_at"])
    op.create_index(op.f("ix_family_invites_family_id"), "family_invites", ["family_id"])
    op.create_index(op.f("ix_family_invites_id"), "family_invites", ["id"])
    op.create_index(op.f("ix_family_invites_token_hash"), "family_invites", ["token_hash"], unique=True)


def downgrade() -> None:
    op.drop_index(op.f("ix_family_invites_token_hash"), table_name="family_invites")
    op.drop_index(op.f("ix_family_invites_id"), table_name="family_invites")
    op.drop_index(op.f("ix_family_invites_family_id"), table_name="family_invites")
    op.drop_index(op.f("ix_family_invites_expires_at"), table_name="family_invites")
    op.drop_index(op.f("ix_family_invites_created_by_user_id"), table_name="family_invites")
    op.drop_index(op.f("ix_family_invites_accepted_by_user_id"), table_name="family_invites")
    op.drop_table("family_invites")
