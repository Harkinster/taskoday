from datetime import date

import pytest

from tests.test_family_tasks_api import _create_task, _headers, _register_parent, _register_child_and_attach, _today

API = "/api/v1"


@pytest.mark.parametrize("scope,kind,category,recurrence", [
    ("PERSONAL", "ROUTINE", "TASKODAY_PERSONAL_ROUTINE", "DAILY"),
    ("PERSONAL", "MISSION", "TASKODAY_PERSONAL_MISSION", "NONE"),
    ("HOUSE", "ROUTINE", "TASKODAY_HOUSE_ROUTINE", "DAILY"),
    ("HOUSE", "MISSION", "TASKODAY_HOUSE_MISSION", "NONE"),
    ("HOUSE", "QUEST", "TASKODAY_HOUSE_QUEST", "NONE"),
    ("HOUSE", "QUEST", "TASKODAY_HOUSE_QUEST", "DAILY"),
])
def test_supported_identity_and_snapshots(client, scope, kind, category, recurrence):
    token, parent_id, family_id = _register_parent(client, f"scope-{scope}-{kind}-{recurrence}")
    task = _create_task(client, token, family_id, {
        "title": "Action", "scope": scope, "kind": kind, "recurrence": recurrence,
        "due_date": date.today().isoformat(), "assignee_user_ids": [parent_id],
    })
    assert (task["scope"], task["kind"], task["category"]) == (scope, kind, category)
    occurrence = next(item for item in _today(client, token, family_id, date.today())["items"] if item["task_id"] == task["id"])
    assert (occurrence["scope"], occurrence["kind"], occurrence["category"]) == (scope, kind, category)
    if scope == "HOUSE":
        started = client.post(f"{API}/task-occurrences/{occurrence['occurrence_id']}/start", headers=_headers(token))
        assert started.status_code == 200
    completed = client.post(f"{API}/task-occurrences/{occurrence['occurrence_id']}/complete", headers=_headers(token))
    assert completed.status_code == 200
    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(token)).json()["data"]["items"]
    assert (events[0]["scope"], events[0]["kind"], events[0]["category"]) == (scope, kind, category)
    same = client.patch(f"{API}/family-tasks/{task['id']}", headers=_headers(token), json={"scope": scope, "kind": kind})
    assert same.status_code == 200
    for changed in ({"scope": "HOUSE" if scope == "PERSONAL" else "PERSONAL"}, {"kind": "MISSION" if kind != "MISSION" else "ROUTINE"}):
        assert client.patch(f"{API}/family-tasks/{task['id']}", headers=_headers(token), json=changed).status_code == 409
    events_again = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(token)).json()["data"]["items"]
    assert (events_again[0]["scope"], events_again[0]["kind"]) == (scope, kind)


@pytest.mark.parametrize("payload", [
    {"scope": "PERSONAL", "kind": "QUEST"},
    {"scope": "HOUSE", "kind": "ROUTINE", "recurrence": "NONE"},
    {"scope": "HOUSE", "kind": "MISSION", "recurrence": "DAILY"},
    {"scope": "PERSONAL", "kind": "MISSION", "recurrence": "WEEKLY"},
])
def test_invalid_identity_or_recurrence_is_rejected(client, payload):
    slug = "invalid-" + "-".join(str(value).lower() for value in payload.values())
    token, _, family_id = _register_parent(client, slug)
    response = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(token), json={"title": "Invalid", **payload})
    assert response.status_code == 422


def test_new_personal_action_requires_exactly_one_member(client):
    token, parent_id, family_id = _register_parent(client, "personal-one-member")
    _, child_id = _register_child_and_attach(client, token, "personal-one-member")
    for assignees in ([], [parent_id, child_id]):
        response = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(token), json={
            "title": "Personal", "scope": "PERSONAL", "kind": "MISSION", "assignee_user_ids": assignees,
        })
        assert response.status_code == 422


def test_undated_mission_has_one_open_occurrence_without_fabricated_deadline(client):
    token, parent_id, family_id = _register_parent(client, "undated-mission")
    task = _create_task(client, token, family_id, {
        "title": "Sans echeance", "scope": "PERSONAL", "kind": "MISSION", "assignee_user_ids": [parent_id],
    })
    first = _today(client, token, family_id, date.today())
    item = next(item for item in first["items"] if item["task_id"] == task["id"])
    assert item["scheduled_date"] is None and item["due_date"] is None
    second = _today(client, token, family_id, date.today())
    assert next(x for x in second["items"] if x["task_id"] == task["id"])["occurrence_id"] == item["occurrence_id"]
    assert client.post(f"{API}/task-occurrences/{item['occurrence_id']}/complete", headers=_headers(token)).status_code == 200
    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(token)).json()["data"]["items"]
    assert events[0]["scheduled_date"] is None
    assert events[0]["scope"] == "PERSONAL" and events[0]["kind"] == "MISSION"


def test_routine_optional_end_date_limits_generation(client):
    token, parent_id, family_id = _register_parent(client, "routine-end-date")
    start = date.today()
    task = _create_task(client, token, family_id, {
        "title": "Pendant deux jours", "scope": "HOUSE", "kind": "ROUTINE", "recurrence": "DAILY",
        "due_date": start.isoformat(), "end_date": start.isoformat(), "assignee_user_ids": [parent_id],
    })
    assert task["end_date"] == start.isoformat()
    assert any(item["task_id"] == task["id"] for item in _today(client, token, family_id, start)["items"])
    from datetime import timedelta
    assert all(item["task_id"] != task["id"] for item in _today(client, token, family_id, start + timedelta(days=1))["items"])


def test_openapi_documents_scope_kind(client):
    components = client.get("/openapi.json").json()["components"]["schemas"]
    for name in ("FamilyTaskOccurrenceResponse", "FamilyTaskOccurrenceEventResponse"):
        assert {"scope", "kind"} <= set(components[name]["required"])
    assert {"scope", "kind"} <= set(components["FamilyTaskDefinitionResponse"]["required"])


def test_child_sees_house_routine_and_mission_but_not_other_personal_action(client):
    token, _, family_id = _register_parent(client, "child-new-scope")
    child_token, child_id = _register_child_and_attach(client, token, "child-new-scope")
    _, other_id = _register_child_and_attach(client, token, "child-new-scope-other")
    for scope, kind, assignees in (
        ("HOUSE", "ROUTINE", [other_id]),
        ("HOUSE", "MISSION", [other_id]),
        ("PERSONAL", "MISSION", [other_id]),
    ):
        _create_task(client, token, family_id, {
            "title": f"{scope} {kind}", "scope": scope, "kind": kind,
            "recurrence": "DAILY" if kind == "ROUTINE" else "NONE",
            "due_date": date.today().isoformat(), "assignee_user_ids": assignees,
        })
    definitions = client.get(f"{API}/families/{family_id}/tasks", headers=_headers(child_token)).json()["data"]
    assert {(task["scope"], task["kind"]) for task in definitions} == {("HOUSE", "ROUTINE"), ("HOUSE", "MISSION")}
    occurrences = _today(client, child_token, family_id, date.today())["items"]
    assert {(item["scope"], item["kind"]) for item in occurrences} == {("HOUSE", "ROUTINE"), ("HOUSE", "MISSION")}
