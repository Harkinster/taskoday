"""Commercial-free access; role, family and reward constraints remain mandatory."""
from datetime import date
from tests.test_family_children import _register_parent, _register_child, _attach_child, _me, _headers

API = "/api/v1"


def test_legacy_creation_exceeds_every_former_android_commercial_cap(client):
    parent = _register_parent(client, "free.parent@example.com", "Free QA")
    child = _register_child(client, "free.child@example.com", "Free Child")
    child_id = _attach_child(client, parent, child)
    for route, count, extra in (
        ("routines", 6, {"repeat_type": "daily", "can_child_delete": False}),
        ("missions", 4, {}),
        ("quests", 2, {"xp_reward": 25}),
        ("wishes", 4, {"cost_scales": 10, "is_active": True}),
    ):
        for index in range(count):
            response = client.post(f"{API}/children/{child_id}/{route}", headers=_headers(parent),
                                   json={"title": f"Free {route} {index}", **extra})
            assert response.status_code in (200, 201), response.json()
        response = client.get(f"{API}/children/{child_id}/{route}", headers=_headers(parent))
        assert response.status_code == 200
        assert len(response.json()["data"]) == count


def test_multiple_children_and_families_do_not_grant_child_management_rights(client):
    parent = _register_parent(client, "free.multi@example.com", "First family")
    child_tokens = [_register_child(client, f"free.multi{i}@example.com", f"Child {i}") for i in range(2)]
    for child in child_tokens:
        _attach_child(client, parent, child)
    assert len(client.get(f"{API}/children", headers=_headers(parent)).json()["data"]) == 2
    second = client.post(f"{API}/families", headers=_headers(parent), json={"name": "Second family"})
    assert second.status_code == 200
    assert len(client.get(f"{API}/families/me", headers=_headers(parent)).json()["data"]) == 2
    denied = client.post(f"{API}/families", headers=_headers(child_tokens[0]), json={"name": "Forbidden"})
    assert denied.status_code == 403
    second_id = second.json()["data"]["id"]
    denied = client.get(f"{API}/families/{second_id}/members", headers=_headers(child_tokens[0]))
    assert denied.status_code in (403, 404)


def test_free_advanced_recurrence_and_multiple_assignees_keep_family_isolation(client):
    parent = _register_parent(client, "free.recur@example.com", "Recurrence QA")
    children = [_register_child(client, f"free.recur{i}@example.com", f"Child {i}") for i in range(2)]
    ids = [_attach_child(client, parent, child) for child in children]
    family_id = _me(client, parent)["family_ids"][0]
    response = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(parent), json={
        "title": "Free advanced routine", "scope": "HOUSE", "kind": "ROUTINE", "assignee_user_ids": ids,
        "due_date": date.today().isoformat(), "recurrence": "WEEKLY", "recurrence_interval": 3,
        "validation_required": True, "gamification_enabled": True,
    })
    assert response.status_code == 201, response.json()
    assert response.json()["data"]["recurrence_interval"] == 3
    assert {row["user_id"] for row in response.json()["data"]["assignees"]} == set(ids)
    invalid_personal = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(parent), json={
        "title": "Invalid personal batch", "scope": "PERSONAL", "kind": "ROUTINE", "assignee_user_ids": ids,
        "recurrence": "DAILY"})
    assert invalid_personal.status_code == 422  # Data coherence, not a commercial restriction.
    outsider = _register_parent(client, "free.outsider@example.com", "Other family")
    denied = client.get(f"{API}/families/{family_id}/tasks", headers=_headers(outsider))
    assert denied.status_code in (403, 404)
    denied = client.post(f"{API}/families/{family_id}/tasks", headers=_headers(children[0]), json={
        "title": "Forbidden house quest", "scope": "HOUSE", "kind": "QUEST", "assignee_user_ids": ids})
    assert denied.status_code == 403
