from datetime import date, timedelta


API = "/api/v1"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _due_at(day: date, hour: int = 8) -> str:
    return f"{day.isoformat()}T{hour:02d}:00:00+00:00"


def _register_parent(client, slug: str) -> tuple[str, int, int]:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": f"parent.{slug}@example.com",
            "password": "supersecret123",
            "family_name": f"Famille {slug}",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]

    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    return token, me.json()["id"], me.json()["family_ids"][0]


def _register_child_and_attach(client, parent_token: str, slug: str) -> tuple[str, int]:
    child = client.post(
        f"{API}/auth/register-child",
        json={
            "email": f"child.{slug}@example.com",
            "password": "childsecret123",
            "display_name": f"Child {slug}",
        },
    )
    assert child.status_code == 201
    child_token = child.json()["access_token"]

    pairing = client.post(f"{API}/pairing/generate-code", headers=_headers(child_token))
    assert pairing.status_code == 200
    attached = client.post(
        f"{API}/pairing/attach-child",
        headers=_headers(parent_token),
        json={"code": pairing.json()["data"]["code"]},
    )
    assert attached.status_code == 200
    return child_token, attached.json()["data"]["child_id"]


def _create_task(client, token: str, family_id: int, payload: dict) -> dict:
    response = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(token), json=payload)
    assert response.status_code == 201
    return response.json()["data"]


def _today(client, token: str, family_id: int, target: date) -> dict:
    response = client.get(
        f"{API}/families/{family_id}/tasks/today",
        headers=_headers(token),
        params={"date": target.isoformat()},
    )
    assert response.status_code == 200
    return response.json()["data"]


def _range(client, token: str, family_id: int, start: date, end: date) -> dict:
    response = client.get(
        f"{API}/families/{family_id}/task-occurrences",
        headers=_headers(token),
        params={"start_date": start.isoformat(), "end_date": end.isoformat()},
    )
    assert response.status_code == 200
    return response.json()["data"]


def _item_by_title(today_payload: dict, title: str) -> dict:
    matches = [item for item in today_payload["items"] if item["title"] == title]
    assert len(matches) == 1
    return matches[0]


def test_create_family_tasks_with_assignments_recurrence_and_today_view(client) -> None:
    parent_token, parent_id, family_id = _register_parent(client, "family-tasks-create")
    _, child_id = _register_child_and_attach(client, parent_token, "family-tasks-create")
    today = date.today()
    selected_day = date(2026, 8, 21)

    unassigned = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache ponctuelle foyer",
            "due_at": _due_at(today),
            "gamification_enabled": False,
        },
    )
    assert unassigned["assignees"] == []
    assert unassigned["gamification_enabled"] is False
    assert unassigned["recurrence"] == "NONE"
    assert unassigned["due_date"] == today.isoformat()
    assert unassigned["has_due_time"] is True
    assert unassigned["due_time"] == "08:00:00"

    date_only = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache jour sans heure",
            "due_date": today.isoformat(),
            "gamification_enabled": False,
        },
    )
    assert date_only["due_at"] is None
    assert date_only["due_date"] == today.isoformat()
    assert date_only["has_due_time"] is False
    assert date_only["due_time"] is None

    timed = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache avec heure explicite",
            "due_date": today.isoformat(),
            "due_time": "14:30:00",
            "gamification_enabled": False,
        },
    )
    assert timed["due_at"] is not None
    assert timed["due_date"] == today.isoformat()
    assert timed["has_due_time"] is True
    assert timed["due_time"] == "14:30:00"

    unscheduled = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache sans echeance",
            "gamification_enabled": False,
        },
    )
    assert unscheduled["due_at"] is None
    assert unscheduled["due_date"] is None
    assert unscheduled["has_due_time"] is False
    assert unscheduled["due_time"] is None

    child_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache enfant",
            "due_at": _due_at(today, 9),
            "priority": "HIGH",
            "assignee_user_ids": [child_id],
        },
    )
    assert [assignee["user_id"] for assignee in child_task["assignees"]] == [child_id]

    parent_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache parent",
            "due_at": _due_at(today, 10),
            "assignee_user_ids": [parent_id],
        },
    )
    assert [assignee["user_id"] for assignee in parent_task["assignees"]] == [parent_id]

    multi_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache partagee",
            "due_at": _due_at(today, 11),
            "assignee_user_ids": [child_id, parent_id],
            "validation_required": True,
        },
    )
    assert {assignee["user_id"] for assignee in multi_task["assignees"]} == {child_id, parent_id}

    daily_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache quotidienne",
            "due_at": _due_at(today, 12),
            "recurrence": "DAILY",
        },
    )
    assert daily_task["recurrence"] == "DAILY"

    selected_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Tache lundi mercredi vendredi",
            "due_at": _due_at(selected_day, 13),
            "recurrence": "SELECTED_WEEKDAYS",
            "selected_weekdays": ["lundi", "mercredi", "vendredi"],
        },
    )
    assert selected_task["selected_weekdays"] == [1, 3, 5]

    today_payload = _today(client, parent_token, family_id, today)
    titles = {item["title"] for item in today_payload["items"]}
    assert {
        "Tache ponctuelle foyer",
        "Tache enfant",
        "Tache parent",
        "Tache partagee",
        "Tache quotidienne",
        "Tache jour sans heure",
        "Tache avec heure explicite",
    }.issubset(titles)
    assert "Tache sans echeance" not in titles

    child_item = _item_by_title(today_payload, "Tache enfant")
    assert child_item["status"] == "TODO"
    assert child_item["priority"] == "HIGH"
    assert child_item["validation_required"] is False
    assert child_item["gamification_enabled"] is False
    assert [assignee["user_id"] for assignee in child_item["assignees"]] == [child_id]

    date_only_item = _item_by_title(today_payload, "Tache jour sans heure")
    assert date_only_item["due_at"] is None
    assert date_only_item["due_date"] == today.isoformat()
    assert date_only_item["has_due_time"] is False
    assert date_only_item["due_time"] is None

    timed_item = _item_by_title(today_payload, "Tache avec heure explicite")
    assert timed_item["due_at"] is not None
    assert timed_item["due_date"] == today.isoformat()
    assert timed_item["has_due_time"] is True
    assert timed_item["due_time"] == "14:30:00"

    group_user_ids = {group["assignee"]["user_id"] for group in today_payload["by_member"] if group["assignee"]}
    assert {parent_id, child_id}.issubset(group_user_ids)
    assert any(group["assignee"] is None for group in today_payload["by_member"])

    selected_payload = _today(client, parent_token, family_id, selected_day)
    assert _item_by_title(selected_payload, "Tache lundi mercredi vendredi")["scheduled_date"] == selected_day.isoformat()


def test_completion_validation_reopen_and_next_recurrent_occurrence(client) -> None:
    parent_token, _, family_id = _register_parent(client, "family-tasks-flow")
    child_token, child_id = _register_child_and_attach(client, parent_token, "family-tasks-flow")
    today = date.today()
    tomorrow = today + timedelta(days=1)

    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Sans validation",
            "due_at": _due_at(today),
            "assignee_user_ids": [child_id],
            "gamification_enabled": False,
        },
    )
    no_validation_occurrence_id = _item_by_title(_today(client, parent_token, family_id, today), "Sans validation")[
        "occurrence_id"
    ]
    completed = client.post(
        f"{API}/task-occurrences/{no_validation_occurrence_id}/complete",
        headers=_headers(child_token),
    )
    assert completed.status_code == 200
    assert completed.json()["data"]["status"] == "COMPLETED"
    assert completed.json()["data"]["completed_by"] == child_id

    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Avec validation",
            "due_at": _due_at(today, 9),
            "assignee_user_ids": [child_id],
            "validation_required": True,
        },
    )
    validation_occurrence_id = _item_by_title(_today(client, parent_token, family_id, today), "Avec validation")[
        "occurrence_id"
    ]
    pending = client.post(
        f"{API}/task-occurrences/{validation_occurrence_id}/complete",
        headers=_headers(child_token),
    )
    assert pending.status_code == 200
    assert pending.json()["data"]["status"] == "PENDING_VALIDATION"

    validated = client.post(
        f"{API}/task-occurrences/{validation_occurrence_id}/validate",
        headers=_headers(parent_token),
    )
    assert validated.status_code == 200
    assert validated.json()["data"]["status"] == "VALIDATED"

    reopened = client.post(
        f"{API}/task-occurrences/{validation_occurrence_id}/reopen",
        headers=_headers(parent_token),
    )
    assert reopened.status_code == 200
    assert reopened.json()["data"]["status"] == "TODO"
    assert reopened.json()["data"]["completed_by"] is None
    assert reopened.json()["data"]["validated_by"] is None

    daily_task = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Occurrence quotidienne",
            "due_at": _due_at(today, 10),
            "recurrence": "DAILY",
            "assignee_user_ids": [child_id],
        },
    )
    today_daily = _item_by_title(_today(client, parent_token, family_id, today), "Occurrence quotidienne")
    daily_completed = client.post(
        f"{API}/task-occurrences/{today_daily['occurrence_id']}/complete",
        headers=_headers(child_token),
    )
    assert daily_completed.status_code == 200
    assert daily_completed.json()["data"]["status"] == "COMPLETED"

    tomorrow_daily = _item_by_title(_today(client, parent_token, family_id, tomorrow), "Occurrence quotidienne")
    assert tomorrow_daily["task_id"] == daily_task["id"]
    assert tomorrow_daily["occurrence_id"] != today_daily["occurrence_id"]
    assert tomorrow_daily["status"] == "TODO"


def test_family_task_access_isolated_between_families(client) -> None:
    parent_a_token, _, family_a_id = _register_parent(client, "family-tasks-isolation-a")
    _, child_a_id = _register_child_and_attach(client, parent_a_token, "family-tasks-isolation-a")
    parent_b_token, _, family_b_id = _register_parent(client, "family-tasks-isolation-b")
    today = date.today()

    _create_task(
        client,
        parent_a_token,
        family_a_id,
        {
            "title": "Tache famille A",
            "due_at": _due_at(today),
            "assignee_user_ids": [child_a_id],
        },
    )
    occurrence_id = _item_by_title(_today(client, parent_a_token, family_a_id, today), "Tache famille A")[
        "occurrence_id"
    ]

    forbidden_list = client.get(f"{API}/families/{family_a_id}/tasks", headers=_headers(parent_b_token))
    assert forbidden_list.status_code == 404

    family_b_today = _today(client, parent_b_token, family_b_id, today)
    assert family_b_today["items"] == []

    forbidden_complete = client.post(
        f"{API}/task-occurrences/{occurrence_id}/complete",
        headers=_headers(parent_b_token),
    )
    assert forbidden_complete.status_code == 404


def test_family_task_occurrences_range_generates_recurrences_and_keeps_existing_status(client) -> None:
    parent_token, parent_id, family_id = _register_parent(client, "family-tasks-range")
    _, child_id = _register_child_and_attach(client, parent_token, "family-tasks-range")
    start = date(2026, 8, 17)
    end = start + timedelta(days=6)

    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range daily",
            "description": "Every day",
            "due_date": start.isoformat(),
            "recurrence": "DAILY",
            "assignee_user_ids": [child_id],
        },
    )
    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range weekly",
            "due_date": (start + timedelta(days=2)).isoformat(),
            "recurrence": "WEEKLY",
        },
    )
    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range selected weekdays",
            "due_date": start.isoformat(),
            "recurrence": "SELECTED_WEEKDAYS",
            "selected_weekdays": ["lundi", "mercredi", "vendredi"],
        },
    )
    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range none",
            "due_date": (start + timedelta(days=4)).isoformat(),
        },
    )
    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range maison",
            "due_date": (start + timedelta(days=5)).isoformat(),
        },
    )
    _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range multi",
            "due_date": (start + timedelta(days=3)).isoformat(),
            "assignee_user_ids": [parent_id, child_id],
        },
    )
    inactive = _create_task(
        client,
        parent_token,
        family_id,
        {
            "title": "Range inactive",
            "due_date": (start + timedelta(days=1)).isoformat(),
        },
    )
    deleted = client.delete(f"{API}/family-tasks/{inactive['id']}", headers=_headers(parent_token))
    assert deleted.status_code == 200

    payload = _range(client, parent_token, family_id, start, end)
    assert payload["start_date"] == start.isoformat()
    assert payload["end_date"] == end.isoformat()
    items = payload["items"]

    by_title = {}
    for item in items:
        by_title.setdefault(item["title"], []).append(item)

    assert len(by_title["Range daily"]) == 7
    assert len(by_title["Range weekly"]) == 1
    assert [item["scheduled_date"] for item in by_title["Range weekly"]] == [(start + timedelta(days=2)).isoformat()]
    assert [item["scheduled_date"] for item in by_title["Range selected weekdays"]] == [
        start.isoformat(),
        (start + timedelta(days=2)).isoformat(),
        (start + timedelta(days=4)).isoformat(),
    ]
    assert len(by_title["Range none"]) == 1
    assert len(by_title["Range maison"]) == 1
    assert by_title["Range maison"][0]["assignees"] == []
    assert len(by_title["Range multi"]) == 1
    assert {assignee["user_id"] for assignee in by_title["Range multi"][0]["assignees"]} == {parent_id, child_id}
    assert "Range inactive" not in by_title

    daily_first = by_title["Range daily"][0]
    assert daily_first["description"] == "Every day"
    assert daily_first["recurrence"] == "DAILY"
    assert daily_first["due_date"] == start.isoformat()
    assert daily_first["due_time"] is None
    assert daily_first["has_due_time"] is False

    completed = client.post(
        f"{API}/task-occurrences/{daily_first['occurrence_id']}/complete",
        headers=_headers(parent_token),
    )
    assert completed.status_code == 200
    assert completed.json()["data"]["status"] == "COMPLETED"

    repeated_payload = _range(client, parent_token, family_id, start, end)
    repeated_items = repeated_payload["items"]
    assert len(repeated_items) == len(items)
    assert {item["occurrence_id"] for item in repeated_items} == {item["occurrence_id"] for item in items}
    repeated_daily_first = next(
        item
        for item in repeated_items
        if item["title"] == "Range daily" and item["scheduled_date"] == start.isoformat()
    )
    assert repeated_daily_first["occurrence_id"] == daily_first["occurrence_id"]
    assert repeated_daily_first["status"] == "COMPLETED"


def test_family_task_occurrences_range_rejects_invalid_ranges_and_other_families(client) -> None:
    parent_a_token, _, family_a_id = _register_parent(client, "family-tasks-range-access-a")
    parent_b_token, _, _ = _register_parent(client, "family-tasks-range-access-b")
    start = date(2026, 8, 17)

    _create_task(
        client,
        parent_a_token,
        family_a_id,
        {
            "title": "Private range task",
            "due_date": start.isoformat(),
        },
    )

    reversed_range = client.get(
        f"{API}/families/{family_a_id}/task-occurrences",
        headers=_headers(parent_a_token),
        params={"start_date": start.isoformat(), "end_date": (start - timedelta(days=1)).isoformat()},
    )
    assert reversed_range.status_code == 422

    oversized_range = client.get(
        f"{API}/families/{family_a_id}/task-occurrences",
        headers=_headers(parent_a_token),
        params={"start_date": start.isoformat(), "end_date": (start + timedelta(days=31)).isoformat()},
    )
    assert oversized_range.status_code == 422

    other_family = client.get(
        f"{API}/families/{family_a_id}/task-occurrences",
        headers=_headers(parent_b_token),
        params={"start_date": start.isoformat(), "end_date": (start + timedelta(days=6)).isoformat()},
    )
    assert other_family.status_code == 404
