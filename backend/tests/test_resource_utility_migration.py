"""SQLite lifecycle checks for resource utility migration 0017."""
from alembic import command
from sqlalchemy import inspect, text
from sqlalchemy.exc import IntegrityError

from tests.test_family_action_category_migration import _config, _revision


EXPECTED = {"reward_resource_spends", "wish_offers", "wish_requests",
            "chronodria_chest_opens", "chronodria_chest_open_drops"}


def test_0017_empty_upgrade_and_downgrade(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0016")
        command.upgrade(config, "20261007_0017")
        assert _revision(engine) == "20261007_0017"
        assert EXPECTED <= set(inspect(engine).get_table_names())
        with engine.connect() as connection:
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261006_0016")
        assert not (EXPECTED & set(inspect(engine).get_table_names()))
    finally:
        engine.dispose()


def test_0017_populated_0016_preserves_grants_and_adds_no_spends(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261006_0016")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'p@qa.test','x','PARENT','Parent')"))
            connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'QA',1)"))
            connection.execute(text("INSERT INTO family_members (family_id,user_id,role) VALUES (1,1,'PARENT')"))
            connection.execute(text("INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,scope,kind,priority,due_date,recurrence,recurrence_interval,validation_required,gamification_enabled,active) VALUES (1,1,'Old',1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','NORMAL','2026-10-06','NONE',1,0,0,1)"))
            connection.execute(text("INSERT INTO family_task_occurrences (id,task_id,category,scope,kind,scheduled_date,status,cycle_number) VALUES (1,1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','2026-10-06','COMPLETED',1)"))
            connection.execute(text("INSERT INTO family_task_occurrence_events (id,family_id,task_id,occurrence_id,category,scope,kind,title,scheduled_date,event_type,cycle_number,status_from,status_to,actor_user_id,occurred_at,legacy_inferred) VALUES (1,1,1,1,'TASKODAY_HOUSE_QUEST','HOUSE','QUEST','Old','2026-10-06','COMPLETE',1,'TODO','COMPLETED',1,'2026-10-06 12:00:00',0)"))
            connection.execute(text("INSERT INTO reward_grants (id,family_id,beneficiary_user_id,action_id,action_title,occurrence_id,cycle_number,scope,kind,points,trigger_event_id) VALUES (1,1,1,1,'Old',1,1,'HOUSE','QUEST',40,1)"))
        command.upgrade(config, "20261007_0017")
        with engine.connect() as connection:
            assert connection.scalar(text("SELECT points FROM reward_grants WHERE id=1")) == 40
            assert connection.scalar(text("SELECT COUNT(*) FROM reward_resource_spends")) == 0
            assert connection.scalar(text("SELECT COUNT(*) FROM chronodria_chest_opens")) == 0
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        inspector = inspect(engine)
        assert any(x["name"] == "uq_resource_spend_user_idempotency" for x in inspector.get_unique_constraints("reward_resource_spends"))
        assert any(x["name"] == "uq_chest_open_user_idempotency" for x in inspector.get_unique_constraints("chronodria_chest_opens"))
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO reward_resource_spends (id,family_id,user_id,resource_type,amount,purpose_type,idempotency_key) VALUES (1,1,1,'FLAME',1,'QA','key1')"))
        try:
            with engine.begin() as connection:
                connection.execute(text("INSERT INTO reward_resource_spends (id,family_id,user_id,resource_type,amount,purpose_type,idempotency_key) VALUES (2,1,1,'FLAME',1,'QA','key1')"))
        except IntegrityError:
            pass
        else:
            raise AssertionError("spend idempotency constraint was not enforced")
        command.downgrade(config, "20261006_0016")
        assert _revision(engine) == "20261006_0016"
    finally:
        engine.dispose()
