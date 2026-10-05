"""Run revision 0011 only against disposable SQLite databases."""

from pathlib import Path

import pytest
from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect, text

from app.core.config import settings


def _config(tmp_path: Path, monkeypatch) -> tuple[Config, object]:
    db_path = tmp_path / "migration-test.sqlite"
    assert db_path.parent == tmp_path and not db_path.exists()
    url = f"sqlite+pysqlite:///{db_path.as_posix()}"
    monkeypatch.setattr(settings, "database_url", url)
    config = Config(str(Path(__file__).resolve().parents[1] / "alembic.ini"))
    config.set_main_option("script_location", str(Path(__file__).resolve().parents[1] / "alembic"))
    engine = create_engine(url)
    return config, engine


def _revision(engine) -> str:
    with engine.connect() as connection:
        return connection.scalar(text("SELECT version_num FROM alembic_version"))


def _seed(engine, categories: list[str | None]) -> None:
    with engine.begin() as connection:
        connection.execute(text("INSERT INTO users (id,email,password_hash,role,display_name) VALUES (1,'migration@example.com','unused','PARENT','Migration')"))
        connection.execute(text("INSERT INTO families (id,name,created_by_user_id) VALUES (1,'Migration family',1)"))
        for index, category in enumerate(categories, start=1):
            connection.execute(text(
                "INSERT INTO family_tasks (id,family_id,title,creator_user_id,category,priority,recurrence,recurrence_interval,validation_required,gamification_enabled,active) "
                "VALUES (:id,1,:title,1,:category,'NORMAL','NONE',1,0,0,1)"
            ), {"id": index, "title": f"Action {index}", "category": category})
            connection.execute(text(
                "INSERT INTO family_task_occurrences (id,task_id,scheduled_date,status) VALUES (:id,:id,'2026-10-04','TODO')"
            ), {"id": index})


def test_sqlite_upgrade_backfills_existing_occurrences_and_downgrades(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261003_0010")
        _seed(engine, ["TASKODAY_PERSONAL_ROUTINE", "TASKODAY_PERSONAL_MISSION", "TASKODAY_HOUSE_QUEST", "Maison", None, ""])
        command.upgrade(config, "20261004_0011")
        assert _revision(engine) == "20261004_0011"
        columns = {column["name"]: column for column in inspect(engine).get_columns("family_task_occurrences")}
        assert columns["category"]["nullable"] is False
        with engine.connect() as connection:
            values = connection.execute(text("SELECT category FROM family_task_occurrences ORDER BY id")).scalars().all()
            assert values == [
                "TASKODAY_PERSONAL_ROUTINE", "TASKODAY_PERSONAL_MISSION",
                "TASKODAY_HOUSE_QUEST", "TASKODAY_HOUSE_QUEST",
                "TASKODAY_HOUSE_QUEST", "TASKODAY_HOUSE_QUEST",
            ]
            assert connection.scalar(text("SELECT COUNT(*) FROM family_task_occurrences WHERE category IS NULL")) == 0
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
        command.downgrade(config, "20261003_0010")
        assert _revision(engine) == "20261003_0010"
        assert "category" not in {column["name"] for column in inspect(engine).get_columns("family_task_occurrences")}
    finally:
        engine.dispose()


def test_sqlite_upgrade_on_empty_database(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261004_0011")
        assert _revision(engine) == "20261004_0011"
        assert inspect(engine).get_columns("family_task_occurrences")
        with engine.connect() as connection:
            assert connection.scalar(text("PRAGMA integrity_check")) == "ok"
    finally:
        engine.dispose()


def test_sqlite_upgrade_rejects_unknown_legacy_category(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261003_0010")
        _seed(engine, ["unknown category"])
        with pytest.raises(RuntimeError, match="unknown or orphan"):
            command.upgrade(config, "20261004_0011")
    finally:
        engine.dispose()


def test_sqlite_upgrade_rejects_orphan_occurrence(tmp_path, monkeypatch) -> None:
    config, engine = _config(tmp_path, monkeypatch)
    try:
        command.upgrade(config, "20261003_0010")
        with engine.begin() as connection:
            connection.execute(text(
                "INSERT INTO family_task_occurrences (id,task_id,scheduled_date,status) "
                "VALUES (1,999,'2026-10-04','TODO')"
            ))
        with pytest.raises(RuntimeError, match="unknown or orphan"):
            command.upgrade(config, "20261004_0011")
    finally:
        engine.dispose()
