"""Add House collaboration cycles and explicit Mission outcomes.

Legacy completion actors are the only participants inferred. Older collaboration
and cycles erased before transition history are not reconstructible.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261006_0014"
down_revision: Union[str, Sequence[str], None] = "20261006_0013"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

OLD_STATUSES = ("TODO", "COMPLETED", "PENDING_VALIDATION", "VALIDATED", "SKIPPED")
NEW_STATUSES = (*OLD_STATUSES[:1], "IN_PROGRESS", *OLD_STATUSES[1:4], "FAILED", OLD_STATUSES[4])


def upgrade() -> None:
    status_old = sa.Enum(*OLD_STATUSES, name="familytaskoccurrencestatus")
    status_new = sa.Enum(*NEW_STATUSES, name="familytaskoccurrencestatus")

    with op.batch_alter_table("family_task_occurrences") as batch:
        batch.add_column(sa.Column("cycle_number", sa.Integer(), nullable=False, server_default="0"))
        batch.alter_column("status", existing_type=status_old, type_=status_new, existing_nullable=False)

    with op.batch_alter_table("family_task_occurrence_events") as batch:
        batch.add_column(sa.Column("cycle_number", sa.Integer(), nullable=True))
        batch.add_column(sa.Column("metadata_json", sa.Text(), nullable=True))
        batch.alter_column("status_from", existing_type=status_old, type_=status_new, existing_nullable=False)
        batch.alter_column("status_to", existing_type=status_old, type_=status_new, existing_nullable=False)

    op.create_table(
        "family_task_occurrence_contributors",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("occurrence_id", sa.Integer(), sa.ForeignKey("family_task_occurrences.id", ondelete="CASCADE"), nullable=False),
        sa.Column("cycle_number", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("joined_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("occurrence_id", "cycle_number", "user_id", name="uq_task_occurrence_cycle_contributor"),
    )
    op.create_index(
        "ix_task_occurrence_contributor_occurrence_cycle",
        "family_task_occurrence_contributors",
        ["occurrence_id", "cycle_number"],
    )

    connection = op.get_bind()
    occurrences = connection.execute(sa.text("SELECT id, status FROM family_task_occurrences ORDER BY id")).mappings().all()
    for occurrence in occurrences:
        events = connection.execute(sa.text(
            "SELECT id, event_type, actor_user_id, occurred_at FROM family_task_occurrence_events "
            "WHERE occurrence_id=:id ORDER BY occurred_at, id"
        ), {"id": occurrence["id"]}).mappings().all()
        cycle = 0
        last_event = None
        for event in events:
            if event["event_type"] == "COMPLETE":
                if cycle == 0 or (last_event is not None and last_event["event_type"] == "REOPEN"):
                    cycle += 1
                connection.execute(sa.text(
                    "UPDATE family_task_occurrence_events SET cycle_number=:cycle WHERE id=:id"
                ), {"cycle": cycle, "id": event["id"]})
                is_house = connection.execute(sa.text(
                    "SELECT scope FROM family_task_occurrence_events WHERE id=:id"
                ), {"id": event["id"]}).scalar_one() == "HOUSE"
                if is_house:
                    connection.execute(sa.text(
                        "INSERT OR IGNORE INTO family_task_occurrence_contributors "
                        "(occurrence_id,cycle_number,user_id,joined_at) VALUES (:occurrence,:cycle,:user,:joined)"
                    ), {"occurrence": occurrence["id"], "cycle": cycle, "user": event["actor_user_id"], "joined": event["occurred_at"]})
            else:
                connection.execute(sa.text(
                    "UPDATE family_task_occurrence_events SET cycle_number=:cycle WHERE id=:id"
                ), {"cycle": cycle or None, "id": event["id"]})
            last_event = event
        current_cycle = cycle + 1 if occurrence["status"] == "TODO" and last_event is not None and last_event["event_type"] == "REOPEN" else cycle
        connection.execute(sa.text(
            "UPDATE family_task_occurrences SET cycle_number=:cycle WHERE id=:id"
        ), {"cycle": current_cycle, "id": occurrence["id"]})


def downgrade() -> None:
    op.drop_index("ix_task_occurrence_contributor_occurrence_cycle", table_name="family_task_occurrence_contributors")
    op.drop_table("family_task_occurrence_contributors")
    status_old = sa.Enum(*OLD_STATUSES, name="familytaskoccurrencestatus")
    status_new = sa.Enum(*NEW_STATUSES, name="familytaskoccurrencestatus")
    with op.batch_alter_table("family_task_occurrence_events") as batch:
        batch.drop_column("cycle_number")
        batch.drop_column("metadata_json")
        batch.alter_column("status_from", existing_type=status_new, type_=status_old, existing_nullable=False)
        batch.alter_column("status_to", existing_type=status_new, type_=status_old, existing_nullable=False)
    with op.batch_alter_table("family_task_occurrences") as batch:
        batch.drop_column("cycle_number")
        batch.alter_column("status", existing_type=status_new, type_=status_old, existing_nullable=False)
