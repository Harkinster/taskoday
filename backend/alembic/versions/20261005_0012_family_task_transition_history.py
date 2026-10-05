"""Keep family task occurrence transitions after reopen.

Only the currently evidenced legacy cycle is backfilled. Earlier reopen cycles
cannot be reconstructed. Downgrade drops the ledger and loses new history.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261005_0012"
down_revision: Union[str, Sequence[str], None] = "20261004_0011"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    status = sa.Enum("TODO", "COMPLETED", "PENDING_VALIDATION", "VALIDATED", "SKIPPED", name="familytaskoccurrencestatus")
    op.create_table(
        "family_task_occurrence_events",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("task_id", sa.Integer(), sa.ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("occurrence_id", sa.Integer(), sa.ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("category", sa.String(length=100), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("scheduled_date", sa.Date(), nullable=False),
        sa.Column("event_type", sa.String(length=20), nullable=False),
        sa.Column("status_from", status, nullable=False),
        sa.Column("status_to", status, nullable=False),
        sa.Column("actor_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("completed_by_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT")),
        sa.Column("occurred_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("legacy_inferred", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    for column in ("family_id", "task_id", "occurrence_id", "occurred_at"):
        op.create_index(f"ix_family_task_occurrence_events_{column}", "family_task_occurrence_events", [column])
    op.create_table(
        "family_task_occurrence_event_participants",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("event_id", sa.Integer(), sa.ForeignKey("family_task_occurrence_events.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.UniqueConstraint("event_id", "user_id", name="uq_family_task_event_participant"),
    )
    op.create_index("ix_family_task_occurrence_event_participants_event_id", "family_task_occurrence_event_participants", ["event_id"])
    op.create_index("ix_family_task_occurrence_event_participants_user_id", "family_task_occurrence_event_participants", ["user_id"])

    connection = op.get_bind()
    rows = connection.execute(sa.text(
        "SELECT o.id AS occurrence_id, o.task_id, o.category, o.scheduled_date, o.status, "
        "o.completed_at, o.completed_by_user_id, o.validated_at, o.validated_by_user_id, "
        "t.family_id, t.title FROM family_task_occurrences AS o "
        "JOIN family_tasks AS t ON t.id = o.task_id ORDER BY o.id"
    )).mappings().all()
    for row in rows:
        if row["status"] not in ("COMPLETED", "PENDING_VALIDATION", "VALIDATED"):
            continue
        if row["completed_at"] is None or row["completed_by_user_id"] is None:
            continue
        participants = connection.execute(sa.text(
            "SELECT user_id FROM family_task_assignees WHERE task_id = :task_id ORDER BY user_id"
        ), {"task_id": row["task_id"]}).scalars().all()

        def add(event_type: str, status_from: str, status_to: str, actor_id: int, occurred_at) -> None:
            result = connection.execute(sa.text(
                "INSERT INTO family_task_occurrence_events "
                "(family_id,task_id,occurrence_id,category,title,scheduled_date,event_type,status_from,status_to,"
                "actor_user_id,completed_by_user_id,occurred_at,legacy_inferred) "
                "VALUES (:family_id,:task_id,:occurrence_id,:category,:title,:scheduled_date,:event_type,:status_from,:status_to,"
                ":actor_user_id,:completed_by_user_id,:occurred_at,1)"
            ), {
                "family_id": row["family_id"], "task_id": row["task_id"], "occurrence_id": row["occurrence_id"],
                "category": row["category"], "title": row["title"], "scheduled_date": row["scheduled_date"],
                "event_type": event_type, "status_from": status_from, "status_to": status_to,
                "actor_user_id": actor_id, "completed_by_user_id": row["completed_by_user_id"], "occurred_at": occurred_at,
            })
            for user_id in participants:
                connection.execute(sa.text(
                    "INSERT INTO family_task_occurrence_event_participants (event_id,user_id) VALUES (:event_id,:user_id)"
                ), {"event_id": result.lastrowid, "user_id": user_id})

        completed_status = "COMPLETED" if row["status"] == "COMPLETED" else "PENDING_VALIDATION"
        add("COMPLETE", "TODO", completed_status, row["completed_by_user_id"], row["completed_at"])
        if row["status"] == "VALIDATED" and row["validated_at"] is not None and row["validated_by_user_id"] is not None:
            add("VALIDATE", "PENDING_VALIDATION", "VALIDATED", row["validated_by_user_id"], row["validated_at"])


def downgrade() -> None:
    op.drop_index("ix_family_task_occurrence_event_participants_user_id", table_name="family_task_occurrence_event_participants")
    op.drop_index("ix_family_task_occurrence_event_participants_event_id", table_name="family_task_occurrence_event_participants")
    op.drop_table("family_task_occurrence_event_participants")
    for column in ("occurred_at", "occurrence_id", "task_id", "family_id"):
        op.drop_index(f"ix_family_task_occurrence_events_{column}", table_name="family_task_occurrence_events")
    op.drop_table("family_task_occurrence_events")
