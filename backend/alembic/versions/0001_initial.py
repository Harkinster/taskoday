"""initial schema

Revision ID: 0001_initial
Revises:
Create Date: 2026-04-26 00:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = "0001_initial"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("email", sa.String(length=255), nullable=False),
        sa.Column("password_hash", sa.String(length=255), nullable=False),
        sa.Column("role", sa.Enum("PARENT", "CHILD", name="userrole"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_users_email"), "users", ["email"], unique=True)
    op.create_index(op.f("ix_users_id"), "users", ["id"], unique=False)
    op.create_index(op.f("ix_users_role"), "users", ["role"], unique=False)

    op.create_table(
        "child_profiles",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("display_name", sa.String(length=120), nullable=False),
        sa.Column("avatar_url", sa.String(length=500), nullable=True),
        sa.Column("xp", sa.Integer(), nullable=False),
        sa.Column("level", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )
    op.create_index(op.f("ix_child_profiles_id"), "child_profiles", ["id"], unique=False)
    op.create_index(op.f("ix_child_profiles_user_id"), "child_profiles", ["user_id"], unique=False)

    op.create_table(
        "families",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("created_by_user_id", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_families_created_by_user_id"), "families", ["created_by_user_id"], unique=False)
    op.create_index(op.f("ix_families_id"), "families", ["id"], unique=False)

    op.create_table(
        "family_members",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("family_id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("role", sa.Enum("PARENT", "CHILD", name="familymemberrole"), nullable=False),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("family_id", "user_id", name="uq_family_member_family_user"),
    )
    op.create_index(op.f("ix_family_members_family_id"), "family_members", ["family_id"], unique=False)
    op.create_index(op.f("ix_family_members_id"), "family_members", ["id"], unique=False)
    op.create_index(op.f("ix_family_members_user_id"), "family_members", ["user_id"], unique=False)

    op.create_table(
        "pairing_codes",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("child_user_id", sa.Integer(), nullable=False),
        sa.Column("code", sa.String(length=12), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("used_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_pairing_codes_child_user_id"), "pairing_codes", ["child_user_id"], unique=False)
    op.create_index(op.f("ix_pairing_codes_code"), "pairing_codes", ["code"], unique=True)
    op.create_index(op.f("ix_pairing_codes_id"), "pairing_codes", ["id"], unique=False)

    op.create_table(
        "missions",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("child_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("status", sa.Enum("OPEN", "COMPLETED", name="taskstatus"), nullable=False),
        sa.Column("due_date", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_by_user_id", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_missions_child_id"), "missions", ["child_id"], unique=False)
    op.create_index(op.f("ix_missions_created_by_user_id"), "missions", ["created_by_user_id"], unique=False)
    op.create_index(op.f("ix_missions_id"), "missions", ["id"], unique=False)

    op.create_table(
        "quests",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("child_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("xp_reward", sa.Integer(), nullable=False),
        sa.Column("status", sa.Enum("OPEN", "COMPLETED", name="taskstatus"), nullable=False),
        sa.Column("created_by_user_id", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_quests_child_id"), "quests", ["child_id"], unique=False)
    op.create_index(op.f("ix_quests_created_by_user_id"), "quests", ["created_by_user_id"], unique=False)
    op.create_index(op.f("ix_quests_id"), "quests", ["id"], unique=False)

    op.create_table(
        "routines",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("child_id", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("period", sa.String(length=100), nullable=True),
        sa.Column("repeat_type", sa.Enum("DAILY", "WEEKLY", "CUSTOM", name="repeattype"), nullable=False),
        sa.Column("created_by_user_id", sa.Integer(), nullable=False),
        sa.Column("created_by_role", sa.String(length=20), nullable=False),
        sa.Column("can_child_delete", sa.Boolean(), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_routines_child_id"), "routines", ["child_id"], unique=False)
    op.create_index(op.f("ix_routines_created_by_user_id"), "routines", ["created_by_user_id"], unique=False)
    op.create_index(op.f("ix_routines_id"), "routines", ["id"], unique=False)

    op.create_table(
        "task_completions",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("task_type", sa.Enum("ROUTINE", "MISSION", "QUEST", name="tasktype"), nullable=False),
        sa.Column("task_id", sa.Integer(), nullable=False),
        sa.Column("child_id", sa.Integer(), nullable=False),
        sa.Column("completed_by_user_id", sa.Integer(), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["completed_by_user_id"], ["users.id"], ondelete="RESTRICT"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("task_type", "task_id", "child_id", name="uq_completion_task_child"),
    )
    op.create_index(op.f("ix_task_completions_child_id"), "task_completions", ["child_id"], unique=False)
    op.create_index(op.f("ix_task_completions_completed_by_user_id"), "task_completions", ["completed_by_user_id"], unique=False)
    op.create_index(op.f("ix_task_completions_id"), "task_completions", ["id"], unique=False)
    op.create_index(op.f("ix_task_completions_task_id"), "task_completions", ["task_id"], unique=False)
    op.create_index(op.f("ix_task_completions_task_type"), "task_completions", ["task_type"], unique=False)

    op.create_table(
        "xp_history",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("child_id", sa.Integer(), nullable=False),
        sa.Column("amount", sa.Integer(), nullable=False),
        sa.Column("reason", sa.Text(), nullable=False),
        sa.Column("source_type", sa.String(length=50), nullable=False),
        sa.Column("source_id", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["child_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_xp_history_child_id"), "xp_history", ["child_id"], unique=False)
    op.create_index(op.f("ix_xp_history_id"), "xp_history", ["id"], unique=False)
    op.create_index(op.f("ix_xp_history_source_id"), "xp_history", ["source_id"], unique=False)
    op.create_index(op.f("ix_xp_history_source_type"), "xp_history", ["source_type"], unique=False)


def downgrade() -> None:
    op.drop_index(op.f("ix_xp_history_source_type"), table_name="xp_history")
    op.drop_index(op.f("ix_xp_history_source_id"), table_name="xp_history")
    op.drop_index(op.f("ix_xp_history_id"), table_name="xp_history")
    op.drop_index(op.f("ix_xp_history_child_id"), table_name="xp_history")
    op.drop_table("xp_history")

    op.drop_index(op.f("ix_task_completions_task_type"), table_name="task_completions")
    op.drop_index(op.f("ix_task_completions_task_id"), table_name="task_completions")
    op.drop_index(op.f("ix_task_completions_id"), table_name="task_completions")
    op.drop_index(op.f("ix_task_completions_completed_by_user_id"), table_name="task_completions")
    op.drop_index(op.f("ix_task_completions_child_id"), table_name="task_completions")
    op.drop_table("task_completions")

    op.drop_index(op.f("ix_routines_id"), table_name="routines")
    op.drop_index(op.f("ix_routines_created_by_user_id"), table_name="routines")
    op.drop_index(op.f("ix_routines_child_id"), table_name="routines")
    op.drop_table("routines")

    op.drop_index(op.f("ix_quests_id"), table_name="quests")
    op.drop_index(op.f("ix_quests_created_by_user_id"), table_name="quests")
    op.drop_index(op.f("ix_quests_child_id"), table_name="quests")
    op.drop_table("quests")

    op.drop_index(op.f("ix_missions_id"), table_name="missions")
    op.drop_index(op.f("ix_missions_created_by_user_id"), table_name="missions")
    op.drop_index(op.f("ix_missions_child_id"), table_name="missions")
    op.drop_table("missions")

    op.drop_index(op.f("ix_pairing_codes_id"), table_name="pairing_codes")
    op.drop_index(op.f("ix_pairing_codes_code"), table_name="pairing_codes")
    op.drop_index(op.f("ix_pairing_codes_child_user_id"), table_name="pairing_codes")
    op.drop_table("pairing_codes")

    op.drop_index(op.f("ix_family_members_user_id"), table_name="family_members")
    op.drop_index(op.f("ix_family_members_id"), table_name="family_members")
    op.drop_index(op.f("ix_family_members_family_id"), table_name="family_members")
    op.drop_table("family_members")

    op.drop_index(op.f("ix_families_id"), table_name="families")
    op.drop_index(op.f("ix_families_created_by_user_id"), table_name="families")
    op.drop_table("families")

    op.drop_index(op.f("ix_child_profiles_user_id"), table_name="child_profiles")
    op.drop_index(op.f("ix_child_profiles_id"), table_name="child_profiles")
    op.drop_table("child_profiles")

    op.drop_index(op.f("ix_users_role"), table_name="users")
    op.drop_index(op.f("ix_users_id"), table_name="users")
    op.drop_index(op.f("ix_users_email"), table_name="users")
    op.drop_table("users")

    op.execute("DROP TYPE IF EXISTS tasktype")
    op.execute("DROP TYPE IF EXISTS repeattype")
    op.execute("DROP TYPE IF EXISTS taskstatus")
    op.execute("DROP TYPE IF EXISTS familymemberrole")
    op.execute("DROP TYPE IF EXISTS userrole")
