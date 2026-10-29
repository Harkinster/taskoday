"""Existing business events consumed by the local Android notification scanner."""
from datetime import date
from tests.test_family_tasks_api import (
    API, _register_parent, _register_child_and_attach, _create_task, _today, _item_by_title, _headers,
)


def _scenario(client):
    parent, parent_id, family = _register_parent(client, "notice-parent")
    child, child_id = _register_child_and_attach(client, parent, "notice-child")
    sibling, sibling_id = _register_child_and_attach(client, parent, "notice-sibling")
    task = _create_task(client, parent, family, {
        "title": "Notification QA private mission", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id],
        "validation_required": True, "gamification_enabled": False,
    })
    occurrence = _item_by_title(_today(client, parent, family, date.today()), task["title"])["occurrence_id"]
    return parent, parent_id, child, child_id, sibling, family, occurrence


def test_notification_decisions_keep_recipient_actor_and_cycle_contract(client):
    parent, pid, child, cid, sibling, family, occurrence = _scenario(client)
    def action(token, name):
        return client.post(f"{API}/task-occurrences/{occurrence}/{name}", headers=_headers(token))
    assert action(child, "complete").json()["data"]["status"] == "PENDING_VALIDATION"
    assert action(child, "validate").status_code == 403
    assert action(parent, "validate").json()["data"]["status"] == "VALIDATED"
    assert action(parent, "reopen").json()["data"]["cycle_number"] == 2
    response = client.get(f"{API}/families/{family}/task-events", headers=_headers(child))
    assert response.status_code == 200
    events = list(reversed(response.json()["data"]["items"]))
    assert [(e["event_type"], e["actor_user_id"], e["completed_by_user_id"], e["cycle_number"]) for e in events] == [
        ("COMPLETE", cid, cid, 1), ("VALIDATE", pid, cid, 1), ("REOPEN", pid, cid, 1),
    ]
    assert all(e["family_id"] == family and e["scope"] == "PERSONAL" and e["kind"] == "MISSION" and not e["legacy_inferred"] for e in events)
    # A repeated mutation does not create a second decision notification event.
    assert action(parent, "reopen").status_code == 200
    assert len(client.get(f"{API}/families/{family}/task-events", headers=_headers(child)).json()["data"]["items"]) == 3


def test_notification_sources_enforce_sibling_and_cross_family_isolation(client):
    parent, _, child, _, sibling, family, occurrence = _scenario(client)
    assert client.post(f"{API}/task-occurrences/{occurrence}/complete", headers=_headers(child)).status_code == 200
    assert client.get(f"{API}/families/{family}/task-events", headers=_headers(sibling)).json()["data"]["items"] == []
    assert client.post(f"{API}/task-occurrences/{occurrence}/validate", headers=_headers(sibling)).status_code in (403, 404)
    outsider, _, _ = _register_parent(client, "notice-outsider")
    for path in ("task-events", "members", "task-occurrences/overdue"):
        assert client.get(f"{API}/families/{family}/{path}", headers=_headers(outsider)).status_code in (403, 404)


def test_parent_can_return_pending_mission_using_existing_reopen_event(client):
    parent, pid, child, cid, _, family, occurrence = _scenario(client)
    assert client.post(f"{API}/task-occurrences/{occurrence}/complete", headers=_headers(child)).json()["data"]["status"] == "PENDING_VALIDATION"
    response=client.post(f"{API}/task-occurrences/{occurrence}/reopen", headers=_headers(parent))
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "TODO"
    event=client.get(f"{API}/families/{family}/task-events", headers=_headers(child)).json()["data"]["items"][0]
    assert (event["event_type"],event["status_from"],event["status_to"],event["actor_user_id"],event["completed_by_user_id"]) == (
        "REOPEN","PENDING_VALIDATION","TODO",pid,cid)
