"""Add planning models (routine, mission, quest, task_completion)

Revision ID: 20260425_0002
Revises: 20260425_0001
Create Date: 2026-04-25 00:10:00.000000
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "20260425_0002"
down_revision: Union[str, Sequence[str], None] = "20260425_0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


day_part_enum = sa.Enum(
    "MATIN",
    "MATINEE",
    "MIDI",
    "APRES_MIDI",
    "SOIR",
    "SOIREE",
    name="day_part",
)

routine_frequency_enum = sa.Enum(
    "DAILY",
    "SELECTED_DAYS",
    name="routine_frequency",
)


def upgrade() -> None:
    op.create_table(
        "routines",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("day_part", day_part_enum, nullable=False),
        sa.Column("frequency", routine_frequency_enum, nullable=False),
        sa.Column("days_of_week", sa.String(length=32), nullable=True),
        sa.Column("points_reward", sa.Integer(), nullable=False, server_default=sa.text("1")),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_routines_child_profile_id", "routines", ["child_profile_id"], unique=False)
    op.create_index("ix_routines_day_part", "routines", ["day_part"], unique=False)
    op.create_index("ix_routines_is_active", "routines", ["is_active"], unique=False)

    op.create_table(
        "missions",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("day_part", day_part_enum, nullable=False),
        sa.Column("scheduled_date", sa.Date(), nullable=False),
        sa.Column("points_reward", sa.Integer(), nullable=False, server_default=sa.text("2")),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_missions_child_profile_id", "missions", ["child_profile_id"], unique=False)
    op.create_index("ix_missions_day_part", "missions", ["day_part"], unique=False)
    op.create_index("ix_missions_scheduled_date", "missions", ["scheduled_date"], unique=False)
    op.create_index("ix_missions_is_active", "missions", ["is_active"], unique=False)

    op.create_table(
        "quests",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("day_part", day_part_enum, nullable=False),
        sa.Column("points_reward", sa.Integer(), nullable=False, server_default=sa.text("3")),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_quests_child_profile_id", "quests", ["child_profile_id"], unique=False)
    op.create_index("ix_quests_day_part", "quests", ["day_part"], unique=False)
    op.create_index("ix_quests_is_active", "quests", ["is_active"], unique=False)

    op.create_table(
        "task_completions",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("child_profile_id", sa.Integer(), sa.ForeignKey("child_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("routine_id", sa.Integer(), sa.ForeignKey("routines.id", ondelete="CASCADE"), nullable=True),
        sa.Column("mission_id", sa.Integer(), sa.ForeignKey("missions.id", ondelete="CASCADE"), nullable=True),
        sa.Column("quest_id", sa.Integer(), sa.ForeignKey("quests.id", ondelete="CASCADE"), nullable=True),
        sa.Column("completion_date", sa.Date(), nullable=False),
        sa.Column("points_awarded", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("completed_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint(
            "child_profile_id",
            "routine_id",
            "completion_date",
            name="uq_task_completion_routine_day",
        ),
        sa.UniqueConstraint(
            "child_profile_id",
            "mission_id",
            "completion_date",
            name="uq_task_completion_mission_day",
        ),
        sa.UniqueConstraint(
            "child_profile_id",
            "quest_id",
            "completion_date",
            name="uq_task_completion_quest_day",
        ),
    )
    op.create_index("ix_task_completions_child_profile_id", "task_completions", ["child_profile_id"], unique=False)
    op.create_index("ix_task_completions_routine_id", "task_completions", ["routine_id"], unique=False)
    op.create_index("ix_task_completions_mission_id", "task_completions", ["mission_id"], unique=False)
    op.create_index("ix_task_completions_quest_id", "task_completions", ["quest_id"], unique=False)
    op.create_index("ix_task_completions_completion_date", "task_completions", ["completion_date"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_task_completions_completion_date", table_name="task_completions")
    op.drop_index("ix_task_completions_quest_id", table_name="task_completions")
    op.drop_index("ix_task_completions_mission_id", table_name="task_completions")
    op.drop_index("ix_task_completions_routine_id", table_name="task_completions")
    op.drop_index("ix_task_completions_child_profile_id", table_name="task_completions")
    op.drop_table("task_completions")

    op.drop_index("ix_quests_is_active", table_name="quests")
    op.drop_index("ix_quests_day_part", table_name="quests")
    op.drop_index("ix_quests_child_profile_id", table_name="quests")
    op.drop_table("quests")

    op.drop_index("ix_missions_is_active", table_name="missions")
    op.drop_index("ix_missions_scheduled_date", table_name="missions")
    op.drop_index("ix_missions_day_part", table_name="missions")
    op.drop_index("ix_missions_child_profile_id", table_name="missions")
    op.drop_table("missions")

    op.drop_index("ix_routines_is_active", table_name="routines")
    op.drop_index("ix_routines_day_part", table_name="routines")
    op.drop_index("ix_routines_child_profile_id", table_name="routines")
    op.drop_table("routines")

