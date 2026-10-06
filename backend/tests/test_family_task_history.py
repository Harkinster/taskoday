from datetime import date

from app.models.family_task import FamilyTask, FamilyTaskOccurrenceEvent
from tests.test_family_tasks_api import (
    API, _create_task, _db_session, _headers, _item_by_title,
    _register_child_and_attach, _register_parent, _today,
)


def _events(client, token, family_id, **params):
    response = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(token), params=params)
    assert response.status_code == 200
    return response.json()["data"]["items"]


def _post(client, token, occurrence_id, action):
    return client.post(f"{API}/task-occurrences/{occurrence_id}/{action}", headers=_headers(token))


def test_completion_reopen_and_second_cycle_keep_every_transition(client) -> None:
    parent, parent_id, family_id = _register_parent(client, "history-cycles")
    child, child_id = _register_child_and_attach(client, parent, "history-cycles")
    task = _create_task(client, parent, family_id, {
        "title": "Faire les devoirs", "category": "TASKODAY_PERSONAL_MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id],
    })
    occurrence_id = _item_by_title(_today(client, parent, family_id, date.today()), task["title"])["occurrence_id"]

    assert _post(client, child, occurrence_id, "complete").json()["data"]["status"] == "COMPLETED"
    assert _post(client, child, occurrence_id, "complete").status_code == 200
    assert len(_events(client, parent, family_id)) == 1
    reopened = _post(client, parent, occurrence_id, "reopen")
    assert reopened.json()["data"]["status"] == "TODO"
    assert reopened.json()["data"]["completed_by"] is None
    assert _post(client, parent, occurrence_id, "reopen").status_code == 200
    assert len(_events(client, parent, family_id)) == 2
    assert _post(client, child, occurrence_id, "complete").status_code == 200

    events = list(reversed(_events(client, parent, family_id)))
    assert [(event["event_type"], event["status_from"], event["status_to"], event["actor_user_id"]) for event in events] == [
        ("COMPLETE", "TODO", "COMPLETED", child_id),
        ("REOPEN", "COMPLETED", "TODO", parent_id),
        ("COMPLETE", "TODO", "COMPLETED", child_id),
    ]
    assert len({event["id"] for event in events}) == 3
    assert all(event["category"] == "TASKODAY_PERSONAL_MISSION" for event in events)
    assert all(event["occurrence_id"] == occurrence_id for event in events)
    assert all(event["occurred_at"] for event in events)
    assert all(event["legacy_inferred"] is False for event in events)

    # A changed definition does not rewrite the human label of older entries.
    changed = client.patch(f"{API}/family-tasks/{task['id']}", headers=_headers(parent), json={"title": "Nouveau titre"})
    assert changed.status_code == 200
    assert {event["title"] for event in _events(client, parent, family_id)} == {"Faire les devoirs"}


def test_validation_actors_shared_quest_and_inactive_definition_history(client) -> None:
    parent, parent_id, family_id = _register_parent(client, "history-shared")
    child_a, child_a_id = _register_child_and_attach(client, parent, "history-shared-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "history-shared-b")
    task = _create_task(client, parent, family_id, {
        "title": "Mettre la table", "category": "TASKODAY_HOUSE_QUEST",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_a_id, child_b_id],
        "validation_required": True,
    })
    occurrence_id = _item_by_title(_today(client, parent, family_id, date.today()), task["title"])["occurrence_id"]
    assert _post(client, child_a, occurrence_id, "start").json()["data"]["contributors"] == [{"user_id": child_a_id, "display_name": f"Child history-shared-a"}]
    assert _post(client, child_b, occurrence_id, "join").status_code == 200
    assert _post(client, child_a, occurrence_id, "complete").json()["data"]["status"] == "PENDING_VALIDATION"
    assert _post(client, child_b, occurrence_id, "join").status_code == 409
    assert len(_events(client, parent, family_id)) == 3  # One shared occurrence; both joined, one completed it.
    assert _post(client, parent, occurrence_id, "validate").json()["data"]["status"] == "VALIDATED"
    assert _post(client, parent, occurrence_id, "validate").status_code == 200
    assert _post(client, parent, occurrence_id, "reopen").status_code == 200
    assert _post(client, parent, occurrence_id, "start").status_code == 200
    assert _post(client, parent, occurrence_id, "complete").status_code == 200

    events = list(reversed(_events(client, parent, family_id)))
    assert [(event["event_type"], event["actor_user_id"], event["completed_by_user_id"]) for event in events] == [
        ("START", child_a_id, None),
        ("JOIN", child_b_id, None),
        ("COMPLETE", child_a_id, child_a_id),
        ("VALIDATE", parent_id, child_a_id),
        ("REOPEN", parent_id, child_a_id),
        ("START", parent_id, None),
        ("COMPLETE", parent_id, parent_id),
    ]
    assert all(event["participant_user_ids"] == [child_a_id, child_b_id] for event in events)
    assert all(event["category"] == "TASKODAY_HOUSE_QUEST" for event in events)
    assert len({event["occurrence_id"] for event in events}) == 1
    assert [event["event_type"] for event in reversed(_events(client, child_a, family_id))] == ["START", "JOIN", "COMPLETE", "VALIDATE", "REOPEN"]
    assert [event["event_type"] for event in reversed(_events(client, child_b, family_id))] == ["START", "JOIN", "COMPLETE", "VALIDATE", "REOPEN"]
    assert [event["contributor_user_ids"] for event in events] == [
        [child_a_id, child_b_id], [child_a_id, child_b_id], [child_a_id, child_b_id],
        [child_a_id, child_b_id], [child_a_id, child_b_id], [parent_id], [parent_id],
    ]
    assert client.delete(f"{API}/family-tasks/{task['id']}", headers=_headers(parent)).status_code == 200
    assert len(_events(client, parent, family_id)) == 7


def test_child_visibility_is_server_enforced_across_lists_direct_ids_and_history(client) -> None:
    parent, _, family_id = _register_parent(client, "history-visibility")
    child_a, child_a_id = _register_child_and_attach(client, parent, "history-visibility-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "history-visibility-b")
    today = date.today()
    own = _create_task(client, parent, family_id, {
        "title": "Private A", "category": "TASKODAY_PERSONAL_MISSION", "due_date": today.isoformat(),
        "assignee_user_ids": [child_a_id],
    })
    other = _create_task(client, parent, family_id, {
        "title": "Private B", "category": "TASKODAY_PERSONAL_ROUTINE", "due_date": today.isoformat(),
        "assignee_user_ids": [child_b_id], "recurrence": "DAILY",
    })
    own_routine = _create_task(client, parent, family_id, {
        "title": "Routine A", "category": "TASKODAY_PERSONAL_ROUTINE", "due_date": today.isoformat(),
        "assignee_user_ids": [child_a_id], "recurrence": "DAILY",
    })
    other_mission = _create_task(client, parent, family_id, {
        "title": "Mission B", "category": "TASKODAY_PERSONAL_MISSION", "due_date": today.isoformat(),
        "assignee_user_ids": [child_b_id],
    })
    house = _create_task(client, parent, family_id, {
        "title": "Shared house", "category": "TASKODAY_HOUSE_QUEST", "due_date": today.isoformat(),
        "assignee_user_ids": [child_b_id],
    })
    parent_items = _today(client, parent, family_id, today)["items"]
    assert {item["title"] for item in parent_items} == {"Private A", "Private B", "Routine A", "Mission B", "Shared house"}
    own_occurrence = next(item["occurrence_id"] for item in parent_items if item["title"] == "Private A")
    other_occurrence = next(item["occurrence_id"] for item in parent_items if item["title"] == "Private B")
    house_occurrence = next(item["occurrence_id"] for item in parent_items if item["title"] == "Shared house")

    for token, expected, forbidden_tasks, forbidden_occurrence in (
        (child_a, {"Private A", "Routine A", "Shared house"}, [other, other_mission], other_occurrence),
        (child_b, {"Private B", "Mission B", "Shared house"}, [own, own_routine], own_occurrence),
    ):
        definitions = client.get(f"{API}/families/{family_id}/tasks", headers=_headers(token)).json()["data"]
        assert {task["title"] for task in definitions} == expected
        inactive_definitions = client.get(f"{API}/families/{family_id}/tasks", headers=_headers(token), params={"include_inactive": "true"}).json()["data"]
        assert {task["title"] for task in inactive_definitions} == expected
        assert {item["title"] for item in _today(client, token, family_id, today)["items"]} == expected
        ranged = client.get(f"{API}/families/{family_id}/task-occurrences", headers=_headers(token), params={
            "start_date": today.isoformat(), "end_date": today.isoformat(),
        }).json()["data"]["items"]
        assert {item["title"] for item in ranged} == expected
        for forbidden_task in forbidden_tasks:
            assert client.patch(f"{API}/family-tasks/{forbidden_task['id']}", headers=_headers(token), json={"title": "probe"}).status_code == 404
        assert _post(client, token, forbidden_occurrence, "complete").status_code == 404
        assert _post(client, token, forbidden_occurrence, "validate").status_code == 404
        assert _post(client, token, forbidden_occurrence, "reopen").status_code == 404

    assert _post(client, child_a, house_occurrence, "start").status_code == 200
    assert _post(client, child_a, house_occurrence, "complete").status_code == 200
    assert _post(client, child_b, other_occurrence, "complete").status_code == 200
    assert _post(client, parent, own_occurrence, "complete").status_code == 200
    assert {event["title"] for event in _events(client, parent, family_id)} == {"Private A", "Private B", "Shared house"}
    house_event = next(event for event in _events(client, parent, family_id) if event["title"] == "Shared house")
    assert house_event["actor_user_id"] not in house_event["participant_user_ids"]
    assert {event["title"] for event in _events(client, child_a, family_id)} == {"Private A", "Shared house"}
    assert {event["title"] for event in _events(client, child_b, family_id)} == {"Private B"}
    assert _events(client, child_a, family_id, actor_user_id=child_b_id) == []

    with _db_session(client) as db:
        assert db.query(FamilyTaskOccurrenceEvent).count() == 4


def test_event_category_comes_from_occurrence_snapshot_not_definition(client) -> None:
    parent, parent_id, family_id = _register_parent(client, "history-category-snapshot")
    task = _create_task(client, parent, family_id, {
        "title": "Original mission", "category": "TASKODAY_PERSONAL_MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [parent_id],
    })
    occurrence_id = _item_by_title(_today(client, parent, family_id, date.today()), task["title"])["occurrence_id"]
    with _db_session(client) as db:
        db.get(FamilyTask, task["id"]).category = "TASKODAY_HOUSE_QUEST"
        db.commit()
    assert _post(client, parent, occurrence_id, "complete").status_code == 200
    assert _events(client, parent, family_id)[0]["category"] == "TASKODAY_PERSONAL_MISSION"
