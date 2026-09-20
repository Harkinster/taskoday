from datetime import date
from types import SimpleNamespace

import pytest

from app.models.family_task import FamilyTaskRecurrence
from app.schemas.family_task import FamilyTaskCreateRequest, FamilyTaskUpdateRequest
from app.services.family_task_service import is_task_scheduled_for_date


def _task(*, start: date, recurrence: FamilyTaskRecurrence, interval: int = 1, weekdays: str | None = None):
    return SimpleNamespace(
        due_date=start,
        due_at=None,
        created_at=None,
        recurrence=recurrence,
        recurrence_interval=interval,
        selected_weekdays=weekdays,
    )


def test_daily_interval_one_and_two() -> None:
    start = date(2026, 9, 20)
    daily = _task(start=start, recurrence=FamilyTaskRecurrence.DAILY)
    every_two_days = _task(start=start, recurrence=FamilyTaskRecurrence.DAILY, interval=2)

    assert is_task_scheduled_for_date(daily, start)
    assert is_task_scheduled_for_date(daily, date(2026, 9, 21))
    assert is_task_scheduled_for_date(every_two_days, start)
    assert not is_task_scheduled_for_date(every_two_days, date(2026, 9, 21))
    assert is_task_scheduled_for_date(every_two_days, date(2026, 9, 22))


def test_weekly_interval_one_and_two() -> None:
    start = date(2026, 9, 22)  # Tuesday
    weekly = _task(start=start, recurrence=FamilyTaskRecurrence.WEEKLY)
    every_two_weeks = _task(start=start, recurrence=FamilyTaskRecurrence.WEEKLY, interval=2)

    assert is_task_scheduled_for_date(weekly, date(2026, 9, 29))
    assert is_task_scheduled_for_date(every_two_weeks, start)
    assert not is_task_scheduled_for_date(every_two_weeks, date(2026, 9, 29))
    assert is_task_scheduled_for_date(every_two_weeks, date(2026, 10, 6))


def test_selected_tuesday_every_two_weeks() -> None:
    task = _task(
        start=date(2026, 9, 21),
        recurrence=FamilyTaskRecurrence.SELECTED_WEEKDAYS,
        interval=2,
        weekdays="2",
    )

    assert is_task_scheduled_for_date(task, date(2026, 9, 22))
    assert not is_task_scheduled_for_date(task, date(2026, 9, 29))
    assert is_task_scheduled_for_date(task, date(2026, 10, 6))
    assert not is_task_scheduled_for_date(task, date(2026, 9, 20))


def test_selected_monday_wednesday_every_three_weeks() -> None:
    task = _task(
        start=date(2026, 9, 21),
        recurrence=FamilyTaskRecurrence.SELECTED_WEEKDAYS,
        interval=3,
        weekdays="1,3",
    )

    assert is_task_scheduled_for_date(task, date(2026, 9, 21))
    assert is_task_scheduled_for_date(task, date(2026, 9, 23))
    assert not is_task_scheduled_for_date(task, date(2026, 9, 28))
    assert is_task_scheduled_for_date(task, date(2026, 10, 12))


def test_selected_weekday_before_start_is_never_generated() -> None:
    task = _task(
        start=date(2026, 9, 23),  # Wednesday
        recurrence=FamilyTaskRecurrence.SELECTED_WEEKDAYS,
        interval=1,
        weekdays="1,3",
    )

    assert not is_task_scheduled_for_date(task, date(2026, 9, 21))
    assert is_task_scheduled_for_date(task, date(2026, 9, 23))


def test_interval_defaults_for_old_payloads_and_rejects_invalid_values() -> None:
    old_payload = {
        "title": "Ancienne tâche",
        "due_date": "2026-09-20",
        "recurrence": "DAILY",
    }
    assert FamilyTaskCreateRequest.model_validate(old_payload).recurrence_interval == 1
    assert FamilyTaskUpdateRequest.model_validate({}).recurrence_interval is None

    with pytest.raises(ValueError):
        FamilyTaskCreateRequest.model_validate({**old_payload, "recurrence_interval": 0})
    with pytest.raises(ValueError):
        FamilyTaskUpdateRequest.model_validate({"recurrence_interval": -1})
