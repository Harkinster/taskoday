from datetime import date, timedelta
from typing import Generator

import pytest
from sqlalchemy import create_engine, func, select
from sqlalchemy.orm import Session, sessionmaker

from app.db.base import Base
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_task import (
    FamilyTask,
    FamilyTaskOccurrence,
    FamilyTaskOccurrenceStatus,
    FamilyTaskPriority,
    FamilyTaskRecurrence,
)
from app.models.user import User, UserRole
from app.services import family_task_service


@pytest.fixture()
def db(tmp_path) -> Generator[Session, None, None]:
    engine = create_engine(
        f"sqlite+pysqlite:///{tmp_path / 'taskoday_concurrency_test.db'}",
        connect_args={"check_same_thread": False},
    )
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, class_=Session)
    Base.metadata.create_all(bind=engine)

    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)
        engine.dispose()


def test_get_or_create_occurrence_recovers_from_unique_collision_and_keeps_session_usable(
    db: Session,
    monkeypatch,
) -> None:
    task = _create_family_task(db)
    scheduled_date = date(2026, 8, 28)

    existing = FamilyTaskOccurrence(
        task_id=task.id,
        scheduled_date=scheduled_date,
        status=FamilyTaskOccurrenceStatus.TODO,
    )
    db.add(existing)
    db.commit()
    existing_id = existing.id

    real_find = family_task_service._find_occurrence
    find_calls = 0

    def raced_find(session: Session, *, task_id: int, scheduled_date: date) -> FamilyTaskOccurrence | None:
        nonlocal find_calls
        find_calls += 1
        if find_calls == 1:
            return None
        return real_find(session, task_id=task_id, scheduled_date=scheduled_date)

    monkeypatch.setattr(family_task_service, "_find_occurrence", raced_find)

    occurrence = family_task_service.get_or_create_occurrence(
        db,
        task=task,
        scheduled_date=scheduled_date,
    )

    assert occurrence.id == existing_id
    assert occurrence.task_id == task.id
    assert occurrence.scheduled_date == scheduled_date
    assert _occurrence_count(db, task.id, scheduled_date) == 1

    next_occurrence = family_task_service.get_or_create_occurrence(
        db,
        task=task,
        scheduled_date=scheduled_date + timedelta(days=1),
    )
    db.commit()

    assert next_occurrence.task_id == task.id
    assert next_occurrence.scheduled_date == scheduled_date + timedelta(days=1)
    assert _occurrence_count(db, task.id, scheduled_date) == 1
    assert _occurrence_count(db, task.id, scheduled_date + timedelta(days=1)) == 1


def _create_family_task(db: Session) -> FamilyTask:
    user = User(
        email="parent.concurrency@example.com",
        password_hash="not-used",
        role=UserRole.PARENT,
    )
    db.add(user)
    db.flush()

    family = Family(name="Famille Concurrency", created_by_user_id=user.id)
    db.add(family)
    db.flush()

    db.add(FamilyMember(family_id=family.id, user_id=user.id, role=FamilyMemberRole.PARENT))

    task = FamilyTask(
        family_id=family.id,
        title="Tache concurrence",
        creator_user_id=user.id,
        priority=FamilyTaskPriority.NORMAL,
        due_date=date(2026, 8, 28),
        recurrence=FamilyTaskRecurrence.NONE,
        validation_required=False,
        gamification_enabled=False,
        active=True,
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


def _occurrence_count(db: Session, task_id: int, scheduled_date: date) -> int:
    return int(
        db.scalar(
            select(func.count(FamilyTaskOccurrence.id)).where(
                FamilyTaskOccurrence.task_id == task_id,
                FamilyTaskOccurrence.scheduled_date == scheduled_date,
            )
        )
        or 0
    )
