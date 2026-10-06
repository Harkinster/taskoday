"""Add canonical action scope/kind snapshots, preserving legacy category.

Backfill uses each row's own category. It never reads an occurrence or event
identity from its current definition. Downgrade discards new identities and
must only be exercised on a temporary database after new types are in use.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261006_0013"
down_revision: Union[str, Sequence[str], None] = "20261005_0012"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

IDENTITIES = {
    "TASKODAY_PERSONAL_ROUTINE": ("PERSONAL", "ROUTINE"),
    "TASKODAY_PERSONAL_MISSION": ("PERSONAL", "MISSION"),
    "TASKODAY_HOUSE_QUEST": ("HOUSE", "QUEST"),
}


def upgrade() -> None:
    with op.batch_alter_table("family_tasks") as batch:
        batch.add_column(sa.Column("end_date", sa.Date(), nullable=True))
    for table in ("family_tasks", "family_task_occurrences", "family_task_occurrence_events"):
        with op.batch_alter_table(table) as batch:
            batch.add_column(sa.Column("scope", sa.String(length=16), nullable=True))
            batch.add_column(sa.Column("kind", sa.String(length=16), nullable=True))

    connection = op.get_bind()
    for table in ("family_tasks", "family_task_occurrences", "family_task_occurrence_events"):
        rows = connection.execute(sa.text(f"SELECT id, category FROM {table}")).mappings().all()
        for row in rows:
            value = row["category"]
            category = value.strip().upper() if value is not None else ""
            if table == "family_tasks" and category in ("", "MAISON"):
                category = "TASKODAY_HOUSE_QUEST"
            if category not in IDENTITIES:
                raise RuntimeError(f"Unknown historical action category in {table} row {row['id']}.")
            scope, kind = IDENTITIES[category]
            connection.execute(
                sa.text(f"UPDATE {table} SET scope=:scope, kind=:kind WHERE id=:id"),
                {"scope": scope, "kind": kind, "id": row["id"]},
            )
        with op.batch_alter_table(table) as batch:
            batch.alter_column("scope", existing_type=sa.String(length=16), nullable=False)
            batch.alter_column("kind", existing_type=sa.String(length=16), nullable=False)

    # An open Mission has one undated occurrence; its deadline stays NULL.
    for table in ("family_task_occurrences", "family_task_occurrence_events"):
        with op.batch_alter_table(table) as batch:
            batch.alter_column("scheduled_date", existing_type=sa.Date(), nullable=True)
    op.create_index("uq_family_task_occurrence_undated", "family_task_occurrences", ["task_id"], unique=True, sqlite_where=sa.text("scheduled_date IS NULL"))


def downgrade() -> None:
    connection = op.get_bind()
    for table in ("family_tasks", "family_task_occurrences", "family_task_occurrence_events"):
        new_kind = connection.execute(sa.text(
            f"SELECT id FROM {table} WHERE category IN ('TASKODAY_HOUSE_ROUTINE','TASKODAY_HOUSE_MISSION') LIMIT 1"
        )).first()
        if new_kind:
            raise RuntimeError(f"Cannot downgrade new action identity in {table} row {new_kind.id}.")
    for table in ("family_task_occurrences", "family_task_occurrence_events"):
        missing = connection.execute(sa.text(f"SELECT id FROM {table} WHERE scheduled_date IS NULL LIMIT 1")).first()
        if missing:
            raise RuntimeError(f"Cannot downgrade undated {table} row {missing.id}.")
    op.drop_index("uq_family_task_occurrence_undated", table_name="family_task_occurrences")
    for table in ("family_task_occurrences", "family_task_occurrence_events"):
        with op.batch_alter_table(table) as batch:
            batch.alter_column("scheduled_date", existing_type=sa.Date(), nullable=False)
    for table in ("family_task_occurrence_events", "family_task_occurrences", "family_tasks"):
        with op.batch_alter_table(table) as batch:
            batch.drop_column("kind")
            batch.drop_column("scope")
    with op.batch_alter_table("family_tasks") as batch:
        batch.drop_column("end_date")
