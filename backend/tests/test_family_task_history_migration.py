"""Revision 0012 is exercised only against disposable SQLite files."""

from alembic import command
from sqlalchemy import inspect, text

from tests.test_family_action_category_migration import _config, _revision


def test_sqlite_empty_upgrade_and_downgrade(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "head")
        assert _revision(engine) == "20261005_0012"
        assert inspect(engine).get_columns("family_task_occurrence_events")
        assert inspect(engine).get_columns("family_task_occurrence_event_participants")
        with engine.connect() as connection:
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261004_0011")
        assert _revision(engine) == "20261004_0011"
        assert not inspect(engine).has_table("family_task_occurrence_events")
    finally:
        engine.dispose()


def test_sqlite_backfill_only_reconstructible_current_cycle(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261004_0011")
        with engine.begin() as connection:
            connection.execute(text(
                "INSERT INTO users (id,email,password_hash,role,display_name) VALUES "
                "(1,'migration-parent@example.com','x','PARENT','Parent'),"
                "(2,'migration-child@example.com','x','CHILD','Child')"
            ))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'Migration',1)"))
            for task_id, category in ((1, "TASKODAY_PERSONAL_ROUTINE"), (2, "TASKODAY_PERSONAL_MISSION"), (3, "TASKODAY_HOUSE_QUEST"), (4, "TASKODAY_HOUSE_QUEST"), (5, "TASKODAY_HOUSE_QUEST")):
                connection.execute(text(
                    "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,priority,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                    "VALUES (:id,1,:title,1,:category,'NORMAL','NONE',1,0,0,1)"
                ), {"id": task_id, "title": f"Action {task_id}", "category": category})
            connection.execute(text("INSERT INTO family_task_assignees (task_id,user_id) VALUES (1,2)"))
            for occurrence_id, status, completed_by, completed_at, validated_by, validated_at in (
                (1, "COMPLETED", 2, "2026-10-05 10:00:00", None, None),
                (2, "PENDING_VALIDATION", 2, "2026-10-05 11:00:00", None, None),
                (3, "VALIDATED", 2, "2026-10-05 12:00:00", 1, "2026-10-05 12:10:00"),
                (4, "TODO", None, None, None, None),
                (5, "COMPLETED", None, "2026-10-05 13:00:00", None, None),
            ):
                connection.execute(text(
                    "INSERT INTO family_task_occurrences "
                    "(id,task_id,category,scheduled_date,status,completed_by_user_id,completed_at,validated_by_user_id,validated_at) "
                    "VALUES (:id,:task_id,:category,'2026-10-05',:status,:completed_by,:completed_at,:validated_by,:validated_at)"
                ), {
                    "id": occurrence_id, "task_id": occurrence_id,
                    "category": ("TASKODAY_PERSONAL_ROUTINE" if occurrence_id == 1 else "TASKODAY_PERSONAL_MISSION" if occurrence_id == 2 else "TASKODAY_HOUSE_QUEST"),
                    "status": status, "completed_by": completed_by, "completed_at": completed_at,
                    "validated_by": validated_by, "validated_at": validated_at,
                })
        command.upgrade(config, "20261005_0012")
        with engine.connect() as connection:
            rows = connection.execute(text(
                "SELECT occurrence_id,event_type,status_from,status_to,actor_user_id,category,legacy_inferred "
                "FROM family_task_occurrence_events ORDER BY id"
            )).all()
            assert rows == [
                (1, "COMPLETE", "TODO", "COMPLETED", 2, "TASKODAY_PERSONAL_ROUTINE", 1),
                (2, "COMPLETE", "TODO", "PENDING_VALIDATION", 2, "TASKODAY_PERSONAL_MISSION", 1),
                (3, "COMPLETE", "TODO", "PENDING_VALIDATION", 2, "TASKODAY_HOUSE_QUEST", 1),
                (3, "VALIDATE", "PENDING_VALIDATION", "VALIDATED", 1, "TASKODAY_HOUSE_QUEST", 1),
            ]
            assert connection.scalar(text("SELECT COUNT(*) FROM family_task_occurrence_events WHERE occurrence_id IN (4,5)")) == 0
            assert connection.execute(text(
                "SELECT occurred_at FROM family_task_occurrence_events WHERE occurrence_id = 3 ORDER BY id"
            )).scalars().all() == ["2026-10-05 12:00:00", "2026-10-05 12:10:00"]
            assert connection.scalar(text("SELECT user_id FROM family_task_occurrence_event_participants")) == 2
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261004_0011")
        assert _revision(engine) == "20261004_0011"
        with engine.connect() as connection:
            assert connection.scalar(text("SELECT COUNT(*) FROM family_task_occurrences")) == 5
    finally:
        engine.dispose()
