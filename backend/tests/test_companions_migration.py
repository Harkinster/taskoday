from alembic import command
from sqlalchemy import inspect, text
from sqlalchemy.exc import IntegrityError

from tests.test_family_action_category_migration import _config, _revision


def test_0018_empty_upgrade_and_downgrade(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261007_0017")
        command.upgrade(config, "20261008_0018")
        assert _revision(engine) == "20261008_0018"
        columns = {column["name"] for column in inspect(engine).get_columns("child_dragons")}
        assert {"lineage_id", "display_name", "starter_key"} <= columns
        with engine.connect() as connection:
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
            assert connection.scalar(text("SELECT COUNT(*) FROM child_dragons")) == 0
            assert connection.scalar(text("SELECT COUNT(*) FROM dragons WHERE key IN ('dragon_fulmio','dragon_sylvyn','dragon_phenor','dragon_lunarys','dragon_pyron','dragon_chronyx','dragon_ambrio','dragon_cristao')")) == 8
        command.downgrade(config, "20261007_0017")
        assert _revision(engine) == "20261007_0017"
        assert not ({"lineage_id", "display_name", "starter_key"} & {c["name"] for c in inspect(engine).get_columns("child_dragons")})
    finally:
        engine.dispose()


def test_0018_preserves_legacy_rows_and_enforces_one_starter(tmp_path, monkeypatch):
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261007_0017")
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'legacy@example.test','x','CHILD','Legacy')"))
            connection.execute(text("INSERT INTO dragons (key,title,is_active) VALUES ('dragon_braise','Dragon braise',1)"))
            connection.execute(text("INSERT INTO child_dragons (id,child_id,dragon_key,stage,progress,active_companion) VALUES (7,1,'dragon_braise','YOUNG',42,1)"))
            connection.execute(text("INSERT INTO eggs (key,title,is_active) VALUES ('oeuf_braise','Oeuf braise',1)"))
            connection.execute(text("INSERT INTO child_eggs (id,child_id,egg_key,status,egg_state,progress) VALUES (9,1,'oeuf_braise','AVAILABLE','warm',25)"))

        command.upgrade(config, "20261008_0018")
        with engine.connect() as connection:
            legacy = connection.execute(text("SELECT child_id,dragon_key,stage,progress,active_companion,lineage_id,display_name,starter_key FROM child_dragons WHERE id=7")).one()
            assert tuple(legacy) == (1, "dragon_braise", "YOUNG", 42, 1, None, None, None)
            egg = connection.execute(text("SELECT child_id,egg_key,status,egg_state,progress FROM child_eggs WHERE id=9")).one()
            assert tuple(egg) == (1, "oeuf_braise", "AVAILABLE", "warm", 25)
            assert connection.scalar(text("SELECT COUNT(*) FROM child_dragons WHERE lineage_id IS NOT NULL")) == 0
            assert connection.scalar(text("PRAGMA foreign_key_check")) is None
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"

        constraints = inspect(engine).get_unique_constraints("child_dragons")
        assert any(item["name"] == "uq_child_dragons_owner_starter" for item in constraints)
        assert any(item["name"] == "uq_child_dragons_child_dragon" for item in constraints)
        assert any(item["name"] == "ck_child_dragon_lineage_official" for item in inspect(engine).get_check_constraints("child_dragons"))
        assert any(item["name"] == "ix_child_dragons_lineage_id" for item in inspect(engine).get_indexes("child_dragons"))
        with engine.begin() as connection:
            connection.execute(text("INSERT INTO child_dragons (id,child_id,dragon_key,stage,progress,active_companion,lineage_id,display_name,starter_key) VALUES (10,1,'dragon_fulmio','BABY',0,1,'FULMIO','Storm','INITIAL')"))
        try:
            with engine.begin() as connection:
                connection.execute(text("INSERT INTO child_dragons (id,child_id,dragon_key,stage,progress,active_companion,lineage_id,display_name,starter_key) VALUES (11,1,'dragon_pyron','BABY',0,0,'PYRON','Ember','INITIAL')"))
        except IntegrityError:
            pass
        else:
            raise AssertionError("starter uniqueness constraint was not enforced")

        with engine.begin() as connection:
            connection.execute(text("DELETE FROM child_dragons WHERE id=10"))
        command.downgrade(config, "20261007_0017")
        assert _revision(engine) == "20261007_0017"
        with engine.connect() as connection:
            legacy = connection.execute(text("SELECT child_id,dragon_key,stage,progress,active_companion FROM child_dragons WHERE id=7")).one()
            assert tuple(legacy) == (1, "dragon_braise", "YOUNG", 42, 1)
            assert connection.scalar(text("SELECT COUNT(*) FROM child_eggs WHERE id=9")) == 1
    finally:
        engine.dispose()
