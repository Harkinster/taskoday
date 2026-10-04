"""Persist the action category on each family task occurrence.

Legacy NULL, blank and Maison definitions are documented house quests. Other
unknown categories and orphan occurrences fail migration rather than receiving
an invented historical identity.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261004_0011"
down_revision: Union[str, Sequence[str], None] = "20261003_0010"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

CANONICAL_CATEGORIES = (
    "TASKODAY_HOUSE_QUEST",
    "TASKODAY_PERSONAL_ROUTINE",
    "TASKODAY_PERSONAL_MISSION",
)


def upgrade() -> None:
    with op.batch_alter_table("family_task_occurrences") as batch_op:
        batch_op.add_column(sa.Column("category", sa.String(length=100), nullable=True))

    connection = op.get_bind()
    invalid = connection.execute(
        sa.text(
            "SELECT o.id FROM family_task_occurrences AS o "
            "LEFT JOIN family_tasks AS t ON t.id = o.task_id "
            "WHERE t.id IS NULL OR (t.category IS NOT NULL "
            "AND TRIM(t.category) <> '' AND UPPER(TRIM(t.category)) <> 'MAISON' "
            "AND UPPER(TRIM(t.category)) NOT IN (:house, :routine, :mission)) LIMIT 1"
        ),
        {"house": CANONICAL_CATEGORIES[0], "routine": CANONICAL_CATEGORIES[1], "mission": CANONICAL_CATEGORIES[2]},
    ).first()
    if invalid is not None:
        raise RuntimeError(f"Cannot snapshot unknown or orphan family task occurrence {invalid.id}.")

    connection.execute(
        sa.text(
            "UPDATE family_task_occurrences SET category = "
            "CASE WHEN (SELECT t.category FROM family_tasks AS t WHERE t.id = task_id) IS NULL "
            "OR TRIM((SELECT t.category FROM family_tasks AS t WHERE t.id = task_id)) = '' "
            "OR UPPER(TRIM((SELECT t.category FROM family_tasks AS t WHERE t.id = task_id))) = 'MAISON' "
            "THEN :house ELSE UPPER(TRIM((SELECT t.category FROM family_tasks AS t WHERE t.id = task_id))) END"
        ),
        {"house": CANONICAL_CATEGORIES[0]},
    )

    with op.batch_alter_table("family_task_occurrences") as batch_op:
        batch_op.alter_column("category", existing_type=sa.String(length=100), nullable=False)


def downgrade() -> None:
    # Destructive for snapshots created since the upgrade; never use casually.
    with op.batch_alter_table("family_task_occurrences") as batch_op:
        batch_op.drop_column("category")
