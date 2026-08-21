"""Add generic family task engine

Revision ID: 20260821_0004
Revises: 20260606_0003
Create Date: 2026-08-21 00:00:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "20260821_0004"
down_revision: Union[str, Sequence[str], None] = "20260606_0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


family_task_priority_enum = sa.Enum("LOW", "NORMAL", "HIGH", "URGENT", name="familytaskpriority")
family_task_recurrence_enum = sa.Enum(
    "NONE",
    "DAILY",
    "WEEKLY",
    "SELECTED_WEEKDAYS",
    name="familytaskrecurrence",
)
family_task_occurrence_status_enum = sa.Enum(
    "TODO",
    "COMPLETED",
    "PENDING_VALIDATION",
    "VALIDATED",
    "SKIPPED",
    name="familytaskoccurrencestatus",
)


def upgrade() -> None:
    op.create_table(
        "family_tasks",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("family_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("creator_user_id", sa.Integer(), nullable=False),
        sa.Column("category", sa.String(length=100), nullable=True),
        sa.Column("priority", family_task_priority_enum, nullable=False),
        sa.Column("due_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("recurrence", family_task_recurrence_enum, nullable=False),
        sa.Column("selected_weekdays", sa.String(length=32), nullable=True),
        sa.Column("validation_required", sa.Boolean(), nullable=False, server_default=sa.text("0")),
        sa.Column("gamification_enabled", sa.Boolean(), nullable=False, server_default=sa.text("0")),
        sa.Column("active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["creator_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_family_tasks_active"), "family_tasks", ["active"], unique=False)
    op.create_index(op.f("ix_family_tasks_creator_user_id"), "family_tasks", ["creator_user_id"], unique=False)
    op.create_index(op.f("ix_family_tasks_due_at"), "family_tasks", ["due_at"], unique=False)
    op.create_index(op.f("ix_family_tasks_family_id"), "family_tasks", ["family_id"], unique=False)
    op.create_index(op.f("ix_family_tasks_id"), "family_tasks", ["id"], unique=False)
    op.create_index(op.f("ix_family_tasks_priority"), "family_tasks", ["priority"], unique=False)
    op.create_index(op.f("ix_family_tasks_recurrence"), "family_tasks", ["recurrence"], unique=False)

    op.create_table(
        "family_task_assignees",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("task_id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["task_id"], ["family_tasks.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("task_id", "user_id", name="uq_family_task_assignee_task_user"),
    )
    op.create_index(op.f("ix_family_task_assignees_id"), "family_task_assignees", ["id"], unique=False)
    op.create_index(op.f("ix_family_task_assignees_task_id"), "family_task_assignees", ["task_id"], unique=False)
    op.create_index(op.f("ix_family_task_assignees_user_id"), "family_task_assignees", ["user_id"], unique=False)

    op.create_table(
        "family_task_occurrences",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("task_id", sa.Integer(), nullable=False),
        sa.Column("scheduled_date", sa.Date(), nullable=False),
        sa.Column("status", family_task_occurrence_status_enum, nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("completed_by_user_id", sa.Integer(), nullable=True),
        sa.Column("validated_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("validated_by_user_id", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["completed_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["task_id"], ["family_tasks.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["validated_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("task_id", "scheduled_date", name="uq_family_task_occurrence_task_date"),
    )
    op.create_index(
        op.f("ix_family_task_occurrences_completed_by_user_id"),
        "family_task_occurrences",
        ["completed_by_user_id"],
        unique=False,
    )
    op.create_index(op.f("ix_family_task_occurrences_id"), "family_task_occurrences", ["id"], unique=False)
    op.create_index(
        op.f("ix_family_task_occurrences_scheduled_date"),
        "family_task_occurrences",
        ["scheduled_date"],
        unique=False,
    )
    op.create_index(op.f("ix_family_task_occurrences_status"), "family_task_occurrences", ["status"], unique=False)
    op.create_index(op.f("ix_family_task_occurrences_task_id"), "family_task_occurrences", ["task_id"], unique=False)
    op.create_index(
        op.f("ix_family_task_occurrences_validated_by_user_id"),
        "family_task_occurrences",
        ["validated_by_user_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_family_task_occurrences_validated_by_user_id"), table_name="family_task_occurrences")
    op.drop_index(op.f("ix_family_task_occurrences_task_id"), table_name="family_task_occurrences")
    op.drop_index(op.f("ix_family_task_occurrences_status"), table_name="family_task_occurrences")
    op.drop_index(op.f("ix_family_task_occurrences_scheduled_date"), table_name="family_task_occurrences")
    op.drop_index(op.f("ix_family_task_occurrences_id"), table_name="family_task_occurrences")
    op.drop_index(op.f("ix_family_task_occurrences_completed_by_user_id"), table_name="family_task_occurrences")
    op.drop_table("family_task_occurrences")

    op.drop_index(op.f("ix_family_task_assignees_user_id"), table_name="family_task_assignees")
    op.drop_index(op.f("ix_family_task_assignees_task_id"), table_name="family_task_assignees")
    op.drop_index(op.f("ix_family_task_assignees_id"), table_name="family_task_assignees")
    op.drop_table("family_task_assignees")

    op.drop_index(op.f("ix_family_tasks_recurrence"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_priority"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_id"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_family_id"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_due_at"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_creator_user_id"), table_name="family_tasks")
    op.drop_index(op.f("ix_family_tasks_active"), table_name="family_tasks")
    op.drop_table("family_tasks")
