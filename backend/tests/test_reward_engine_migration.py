"""Reward Engine migration checks use disposable SQLite files only."""

from alembic import command
from sqlalchemy import inspect, text

from tests.test_family_action_category_migration import _config, _revision


def test_reward_ledger_upgrade_empty_sqlite_and_downgrade(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0014")
        command.upgrade(config, "20261006_0015")
        assert _revision(engine) == "20261006_0015"
        inspector = inspect(engine)
        assert "reward_grants" in inspector.get_table_names()
        columns = {column["name"] for column in inspector.get_columns("reward_grants")}
        assert {
            "family_id", "beneficiary_user_id", "action_id", "action_title", "occurrence_id",
            "cycle_number", "scope", "kind", "points", "trigger_event_id", "created_at",
            "revoked_at", "revoke_event_id",
        } <= columns
        assert any(
            item["name"] == "uq_reward_grant_occurrence_cycle_beneficiary"
            for item in inspector.get_unique_constraints("reward_grants")
        )
        assert len(inspector.get_foreign_keys("reward_grants")) == 6
        with engine.connect() as connection:
            assert connection.scalar(text("SELECT COUNT(*) FROM reward_grants")) == 0
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261006_0014")
        assert _revision(engine) == "20261006_0014"
        assert "reward_grants" not in inspect(engine).get_table_names()
    finally:
        engine.dispose()


def test_existing_completion_is_not_backfilled_with_rewards(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0014")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'p@example.test','x','PARENT','Papa')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'QA',1)"))
            connection.execute(text("INSERT INTO family_members (family_id,user_id,role) VALUES (1,1,'PARENT')"))
            connection.execute(text(
                "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,scope,kind,priority,due_date,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                "VALUES (1,1,'Old completion',1,'TASKODAY_PERSONAL_MISSION','PERSONAL','MISSION','NORMAL','2026-10-06','NONE',1,0,0,1)"
            ))
            connection.execute(text("INSERT INTO family_task_assignees (task_id,user_id) VALUES (1,1)"))
            connection.execute(text(
                "INSERT INTO family_task_occurrences (id,task_id,category,scope,kind,scheduled_date,status,cycle_number,completed_at,completed_by_user_id) "
                "VALUES (1,1,'TASKODAY_PERSONAL_MISSION','PERSONAL','MISSION','2026-10-06','COMPLETED',1,'2026-10-06 12:00:00',1)"
            ))
            connection.execute(text(
                "INSERT INTO family_task_occurrence_events (id,family_id,task_id,occurrence_id,category,scope,kind,title,scheduled_date,event_type,cycle_number,status_from,status_to,actor_user_id,completed_by_user_id,occurred_at,legacy_inferred) "
                "VALUES (1,1,1,1,'TASKODAY_PERSONAL_MISSION','PERSONAL','MISSION','Old completion','2026-10-06','COMPLETE',1,'TODO','COMPLETED',1,1,'2026-10-06 12:00:00',0)"
            ))
        command.upgrade(config, "20261006_0015")
        assert _revision(engine) == "20261006_0015"
        with engine.connect() as connection:
            assert connection.scalar(text("SELECT COUNT(*) FROM family_task_occurrences")) == 1
            assert connection.scalar(text("SELECT COUNT(*) FROM family_task_occurrence_events")) == 1
            assert connection.scalar(text("SELECT COUNT(*) FROM reward_grants")) == 0
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
    finally:
        engine.dispose()


def test_0016_preserves_legacy_points_and_creates_no_resource_backfill(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0014")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'p@example.test','x','PARENT','Papa')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'QA',1)"))
            connection.execute(text("INSERT INTO family_members (family_id,user_id,role) VALUES (1,1,'PARENT')"))
            connection.execute(text("INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,scope,kind,priority,due_date,recurrence,recurrence_interval,validation_required,gamification_enabled,active) VALUES (1,1,'Old',1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','NORMAL','2026-10-06','NONE',1,0,0,1)"))
            connection.execute(text("INSERT INTO family_task_occurrences (id,task_id,category,scope,kind,scheduled_date,status,cycle_number) VALUES (1,1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','2026-10-06','COMPLETED',1)"))
            connection.execute(text("INSERT INTO family_task_occurrence_events (id,family_id,task_id,occurrence_id,category,scope,kind,title,scheduled_date,event_type,cycle_number,status_from,status_to,actor_user_id,occurred_at,legacy_inferred) VALUES (1,1,1,1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','Old','2026-10-06','COMPLETE',1,'TODO','COMPLETED',1,'2026-10-06 12:00:00',0)"))
        command.upgrade(config, "20261006_0015")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO reward_grants (id,family_id,beneficiary_user_id,action_id,action_title,occurrence_id,cycle_number,scope,kind,points,trigger_event_id) VALUES (1,1,1,1,'Old',1,1,'HOUSE','QUEST',40,1)"))
            connection.execute(text("INSERT INTO reward_grants (id,family_id,beneficiary_user_id,action_id,action_title,occurrence_id,cycle_number,scope,kind,points,trigger_event_id,revoked_at,revoke_event_id) VALUES (2,1,1,1,'Old',1,2,'HOUSE','QUEST',20,1,'2026-10-07 12:00:00',1)"))
        command.upgrade(config, "20261006_0016")
        assert _revision(engine) == "20261006_0016"
        inspector = inspect(engine)
        assert "reward_resource_grants" in inspector.get_table_names()
        assert any(item["name"] == "uq_reward_resource_cycle_beneficiary_component" for item in inspector.get_unique_constraints("reward_resource_grants"))
        assert len(inspector.get_foreign_keys("reward_resource_grants")) == 6
        assert {item["name"] for item in inspector.get_indexes("reward_resource_grants")} >= {
            "ix_reward_resource_family_beneficiary", "ix_reward_resource_reward_grant",
        }
        with engine.connect() as connection:
            assert connection.scalar(text("SELECT points FROM reward_grants WHERE id=1")) == 40
            assert connection.scalar(text("SELECT points FROM reward_grants WHERE id=2")) == 20
            assert connection.scalar(text("SELECT policy_version FROM reward_grants WHERE id=1")) == 1
            assert connection.scalar(text("SELECT policy_version FROM reward_grants WHERE id=2")) == 1
            assert connection.scalar(text("SELECT mission_bonus_crystals FROM reward_grants WHERE id=1")) == 0
            assert connection.scalar(text("SELECT COUNT(*) FROM reward_resource_grants")) == 0
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        from sqlalchemy.exc import IntegrityError
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO reward_resource_grants (family_id,beneficiary_user_id,reward_grant_id,action_id,occurrence_id,cycle_number,resource_type,component,amount) VALUES (1,1,1,1,1,1,'FLAME','BASE',1)"))
        try:
            with engine.begin() as connection:
                connection.execute(text("INSERT INTO reward_resource_grants (family_id,beneficiary_user_id,reward_grant_id,action_id,occurrence_id,cycle_number,resource_type,component,amount) VALUES (1,1,1,1,1,1,'FLAME','BASE',1)"))
        except IntegrityError:
            pass
        else:
            raise AssertionError("resource idempotence unique constraint was not enforced")
        command.downgrade(config, "20261006_0015")
        assert _revision(engine) == "20261006_0015"
        assert "reward_resource_grants" not in inspect(engine).get_table_names()
    finally:
        engine.dispose()
