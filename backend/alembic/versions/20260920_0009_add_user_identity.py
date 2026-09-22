"""Add the canonical user display name and backfill reliable child identity.

Revision ID: 20260920_0009
Revises: 20260920_0008
Create Date: 2026-09-20 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260920_0009"
down_revision: Union[str, Sequence[str], None] = "20260920_0008"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("users") as batch_op:
        batch_op.add_column(sa.Column("display_name", sa.String(length=120), nullable=True))

    op.execute(
        sa.text(
            """
            UPDATE users
            SET display_name = (
                SELECT child_profiles.display_name
                FROM child_profiles
                WHERE child_profiles.user_id = users.id
                  AND TRIM(child_profiles.display_name) <> ''
            )
            WHERE display_name IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM child_profiles
                  WHERE child_profiles.user_id = users.id
                    AND TRIM(child_profiles.display_name) <> ''
              )
            """
        )
    )
    op.execute(
        sa.text(
            """
            UPDATE users
            SET birth_date = (
                SELECT child_profiles.birth_date
                FROM child_profiles
                WHERE child_profiles.user_id = users.id
                  AND child_profiles.birth_date IS NOT NULL
            )
            WHERE birth_date IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM child_profiles
                  WHERE child_profiles.user_id = users.id
                    AND child_profiles.birth_date IS NOT NULL
              )
            """
        )
    )


def downgrade() -> None:
    with op.batch_alter_table("users") as batch_op:
        batch_op.drop_column("display_name")
