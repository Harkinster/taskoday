from datetime import date, timedelta

from tests.test_family_tasks_api import API, _create_task, _headers, _register_child_and_attach, _register_parent, _today


def _post(client, token, occurrence_id, action, json=None):
    return client.post(f"{API}/task-occurrences/{occurrence_id}/{action}", headers=_headers(token), json=json)


def test_house_start_join_complete_validation_reopen_uses_independent_cycles(client):
    parent, parent_id, family_id = _register_parent(client, "collaboration-cycles")
    child_a, child_a_id = _register_child_and_attach(client, parent, "collaboration-cycles-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "collaboration-cycles-b")
    task = _create_task(client, parent, family_id, {
        "title": "Garage", "scope": "HOUSE", "kind": "MISSION", "due_date": date.today().isoformat(),
        "assignee_user_ids": [child_a_id], "validation_required": True,
    })
    occurrence_id = next(x["occurrence_id"] for x in _today(client, parent, family_id, date.today())["items"] if x["task_id"] == task["id"])

    started = _post(client, child_a, occurrence_id, "start")
    assert started.status_code == 200
    assert started.json()["data"]["status"] == "IN_PROGRESS"
    assert started.json()["data"]["cycle_number"] == 1
    assert started.json()["data"]["contributors"] == [{"user_id": child_a_id, "display_name": "Child collaboration-cycles-a"}]
    assert _post(client, child_a, occurrence_id, "start").status_code == 200  # idempotent
    assert _post(client, child_b, occurrence_id, "start").status_code == 409
    assert _post(client, child_b, occurrence_id, "join").status_code == 200
    assert _post(client, child_b, occurrence_id, "join").status_code == 200  # no duplicate event
    assert _post(client, parent, occurrence_id, "complete").status_code == 403  # Parent must contribute too

    complete = _post(client, child_a, occurrence_id, "complete")
    assert complete.status_code == 200
    assert complete.json()["data"]["status"] == "PENDING_VALIDATION"
    assert {x["user_id"] for x in complete.json()["data"]["contributors"]} == {child_a_id, child_b_id}
    assert _post(client, child_b, occurrence_id, "join").status_code == 409
    assert _post(client, parent, occurrence_id, "validate").json()["data"]["status"] == "VALIDATED"

    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(parent)).json()["data"]["items"]
    events = list(reversed(events))
    assert [e["event_type"] for e in events] == ["START", "JOIN", "COMPLETE", "VALIDATE"]
    assert [e["actor_user_id"] for e in events] == [child_a_id, child_b_id, child_a_id, parent_id]
    assert all(set(e["contributor_user_ids"]) == {child_a_id, child_b_id} for e in events)
    assert all(e["scope"] == "HOUSE" and e["kind"] == "MISSION" for e in events)

    assert _post(client, parent, occurrence_id, "reopen").json()["data"]["status"] == "TODO"
    reopened = client.get(f"{API}/families/{family_id}/tasks/today", headers=_headers(parent)).json()["data"]["items"]
    current = next(x for x in reopened if x["occurrence_id"] == occurrence_id)
    assert current["contributors"] == []
    assert _post(client, parent, occurrence_id, "start").json()["data"]["cycle_number"] == 2
    second = _post(client, parent, occurrence_id, "complete")
    assert second.status_code == 200 and second.json()["data"]["status"] == "PENDING_VALIDATION"
    assert second.json()["data"]["contributors"] == [{"user_id": parent_id, "display_name": "parent.collaboration-cycles"}]
    assert _post(client, parent, occurrence_id, "validate").status_code == 200

    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(parent)).json()["data"]["items"]
    events = list(reversed(events))
    assert [e["event_type"] for e in events] == ["START", "JOIN", "COMPLETE", "VALIDATE", "REOPEN", "START", "COMPLETE", "VALIDATE"]
    assert [e["cycle_number"] for e in events] == [1, 1, 1, 1, 1, 2, 2, 2]
    assert events[0]["contributor_user_ids"] == [child_a_id, child_b_id]
    assert events[5]["contributor_user_ids"] == [parent_id]


def test_house_contributor_must_join_before_completion_and_personal_cannot_start(client):
    parent, parent_id, family_id = _register_parent(client, "collaboration-permissions")
    child, child_id = _register_child_and_attach(client, parent, "collaboration-permissions")
    house = _create_task(client, parent, family_id, {
        "title": "House", "scope": "HOUSE", "kind": "QUEST", "due_date": date.today().isoformat(),
    })
    personal = _create_task(client, parent, family_id, {
        "title": "Personal", "scope": "PERSONAL", "kind": "MISSION", "due_date": date.today().isoformat(),
        "assignee_user_ids": [child_id],
    })
    items = _today(client, parent, family_id, date.today())["items"]
    house_id = next(x["occurrence_id"] for x in items if x["task_id"] == house["id"])
    personal_id = next(x["occurrence_id"] for x in items if x["task_id"] == personal["id"])
    assert _post(client, child, house_id, "complete").status_code == 409
    assert _post(client, child, house_id, "start").status_code == 200
    assert _post(client, child, house_id, "complete").status_code == 200
    assert _post(client, child, personal_id, "start").status_code == 409


def test_parent_only_reschedule_and_fail_keep_mission_history(client):
    parent, parent_id, family_id = _register_parent(client, "mission-outcomes")
    child, _ = _register_child_and_attach(client, parent, "mission-outcomes")
    yesterday = date.today() - timedelta(days=1)
    due = _create_task(client, parent, family_id, {
        "title": "Reportable", "scope": "HOUSE", "kind": "MISSION", "due_date": yesterday.isoformat(),
    })
    failed = _create_task(client, parent, family_id, {
        "title": "Ratée", "scope": "HOUSE", "kind": "MISSION", "due_date": yesterday.isoformat(),
    })
    items = _today(client, parent, family_id, yesterday)["items"]
    due_id = next(x["occurrence_id"] for x in items if x["task_id"] == due["id"])
    failed_id = next(x["occurrence_id"] for x in items if x["task_id"] == failed["id"])
    assert _post(client, child, due_id, "reschedule", {"due_date": date.today().isoformat()}).status_code == 403
    assert _post(client, child, failed_id, "fail").status_code == 403
    rescheduled = _post(client, parent, due_id, "reschedule", {"due_date": (date.today() + timedelta(days=2)).isoformat()})
    assert rescheduled.status_code == 200
    assert rescheduled.json()["data"]["due_date"] == (date.today() + timedelta(days=2)).isoformat()
    marked_failed = _post(client, parent, failed_id, "fail")
    assert marked_failed.status_code == 200
    assert marked_failed.json()["data"]["status"] == "FAILED"
    assert _post(client, parent, failed_id, "complete").status_code == 409
    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(parent)).json()["data"]["items"]
    by_type = {event["event_type"]: event for event in events}
    assert by_type["RESCHEDULE"]["metadata"] == {
        "old_due_date": yesterday.isoformat(), "new_due_date": (date.today() + timedelta(days=2)).isoformat(),
    }
    assert by_type["FAIL"]["status_to"] == "FAILED"
