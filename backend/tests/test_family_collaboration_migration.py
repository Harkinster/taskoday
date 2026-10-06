"""Migration 0014 tests run only against disposable SQLite databases."""

from alembic import command
from sqlalchemy import text

from tests.test_family_action_category_migration import _config, _revision


def test_upgrade_backfills_only_evidenced_house_complete_actor_as_contributor(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0013")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'parent@example.test','x','PARENT','Papa'),(2,'child@example.test','x','CHILD','Enfant')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'QA',1)"))
            connection.execute(text("INSERT INTO family_members (family_id,user_id,role) VALUES (1,1,'PARENT'),(1,2,'CHILD')"))
            for task_id, scope, kind, category, assignee in (
                (1, "HOUSE", "QUEST", "TASKODAY_HOUSE_QUEST", 2),
                (2, "PERSONAL", "MISSION", "TASKODAY_PERSONAL_MISSION", 2),
            ):
                connection.execute(text(
                    "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,scope,kind,priority,due_date,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                    "VALUES (:id,1,:title,1,:category,:scope,:kind,'NORMAL','2026-10-06','NONE',1,0,0,1)"
                ), {"id": task_id, "title": f"Action {task_id}", "category": category, "scope": scope, "kind": kind})
                connection.execute(text("INSERT INTO family_task_assignees (task_id,user_id) VALUES (:task,:user)"), {"task": task_id, "user": assignee})
                connection.execute(text(
                    "INSERT INTO family_task_occurrences (id,task_id,category,scope,kind,scheduled_date,status,completed_at,completed_by_user_id) "
                    "VALUES (:id,:id,:category,:scope,:kind,'2026-10-06','COMPLETED','2026-10-06 12:00:00',:actor)"
                ), {"id": task_id, "category": category, "scope": scope, "kind": kind, "actor": assignee})
                connection.execute(text(
                    "INSERT INTO family_task_occurrence_events (id,family_id,task_id,occurrence_id,category,scope,kind,title,scheduled_date,event_type,status_from,status_to,actor_user_id,completed_by_user_id,occurred_at,legacy_inferred) "
                    "VALUES (:id,1,:id,:id,:category,:scope,:kind,:title,'2026-10-06','COMPLETE','TODO','COMPLETED',:actor,:actor,'2026-10-06 12:00:00',1)"
                ), {"id": task_id, "category": category, "scope": scope, "kind": kind, "title": f"Action {task_id}", "actor": assignee})
        command.upgrade(config, "20261006_0014")
        assert _revision(engine) == "20261006_0014"
        with engine.connect() as connection:
            assert connection.execute(text("SELECT id,status,cycle_number FROM family_task_occurrences ORDER BY id")).all() == [
                (1, "COMPLETED", 1), (2, "COMPLETED", 1),
            ]
            assert connection.execute(text("SELECT occurrence_id,cycle_number,user_id FROM family_task_occurrence_contributors")).all() == [(1, 1, 2)]
            assert connection.execute(text("SELECT id,cycle_number FROM family_task_occurrence_events ORDER BY id")).all() == [(1, 1), (2, 1)]
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261006_0013")
        assert _revision(engine) == "20261006_0013"
    finally:
        engine.dispose()
