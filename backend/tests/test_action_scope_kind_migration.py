"""Scope/kind and current-head migrations run only on disposable SQLite files."""

import pytest
from alembic import command
from sqlalchemy import inspect, text

from tests.test_family_action_category_migration import _config, _revision


def test_empty_sqlite_upgrade_downgrade(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "head")
        assert _revision(engine) == "20261007_0017"
        for table in ("family_tasks", "family_task_occurrences", "family_task_occurrence_events"):
            columns = {item["name"]: item for item in inspect(engine).get_columns(table)}
            assert columns["scope"]["nullable"] is False
            assert columns["kind"]["nullable"] is False
        with engine.connect() as connection:
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261005_0012")
        assert _revision(engine) == "20261005_0012"
    finally:
        engine.dispose()


def test_populated_sqlite_backfills_each_own_snapshot(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261005_0012")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'scope@example.com','x','PARENT','Parent')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'Scope',1)"))
            for item_id, category in enumerate(("TASKODAY_PERSONAL_ROUTINE", "TASKODAY_PERSONAL_MISSION", "TASKODAY_HOUSE_QUEST"), 1):
                connection.execute(text(
                    "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,priority,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                    "VALUES (:id,1,'Action',1,:category,'NORMAL','NONE',1,0,0,1)"
                ), {"id": item_id, "category": category})
                connection.execute(text(
                    "INSERT INTO family_task_occurrences (id,task_id,category,scheduled_date,status) VALUES (:id,:id,:category,'2026-10-06','TODO')"
                ), {"id": item_id, "category": category})
                connection.execute(text(
                    "INSERT INTO family_task_occurrence_events "
                    "(id,family_id,task_id,occurrence_id,category,title,scheduled_date,event_type,status_from,status_to,actor_user_id,occurred_at,legacy_inferred) "
                    "VALUES (:id,1,:id,:id,:category,'Action','2026-10-06','COMPLETE','TODO','COMPLETED',1,'2026-10-06 10:00:00',1)"
                ), {"id": item_id, "category": category})
            # Historical snapshots must win over a later definition change.
            connection.execute(text("UPDATE family_tasks SET category='TASKODAY_HOUSE_QUEST' WHERE id=2"))
        command.upgrade(config, "20261006_0013")
        with engine.connect() as connection:
            assert connection.execute(text("SELECT scope,kind FROM family_task_occurrences ORDER BY id")).all() == [
                ("PERSONAL", "ROUTINE"), ("PERSONAL", "MISSION"), ("HOUSE", "QUEST"),
            ]
            assert connection.execute(text("SELECT scope,kind FROM family_task_occurrence_events ORDER BY id")).all() == [
                ("PERSONAL", "ROUTINE"), ("PERSONAL", "MISSION"), ("HOUSE", "QUEST"),
            ]
            assert connection.execute(text("SELECT scope,kind FROM family_tasks ORDER BY id")).all() == [
                ("PERSONAL", "ROUTINE"), ("HOUSE", "QUEST"), ("HOUSE", "QUEST"),
            ]
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261005_0012")
    finally:
        engine.dispose()


def test_unknown_category_fails_explicitly(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261005_0012")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'scope@example.com','x','PARENT','Parent')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'Scope',1)"))
            connection.execute(text(
                "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,priority,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                "VALUES (1,1,'Bad',1,'UNKNOWN','NORMAL','NONE',1,0,0,1)"
            ))
        with pytest.raises(RuntimeError, match="Unknown historical action category"):
            command.upgrade(config, "20261006_0013")
    finally:
        engine.dispose()
