from datetime import date
from concurrent.futures import ThreadPoolExecutor

from app.services.resource_utility_service import ChestRandomSource
from tests.test_family_tasks_api import API, _create_task, _headers, _register_child_and_attach, _register_parent, _today


class FixedChestRandom(ChestRandomSource):
    def choose(self, options):
        return options[0]


def _resources(client, token, family_id):
    response = client.get(f"{API}/families/{family_id}/resources/me", headers=_headers(token))
    assert response.status_code == 200, response.text
    return response.json()["data"]


def _earn_personal(client, parent, child, child_id, family_id, suffix="earn"):
    task = _create_task(client, parent, family_id, {"title": "Mission reward", "scope": "PERSONAL",
        "kind": "MISSION", "due_date": date.today().isoformat(), "assignee_user_ids": [child_id]})
    occurrence = next(row for row in _today(client, child, family_id, date.today())["items"] if row["task_id"] == task["id"])
    complete = client.post(f"{API}/task-occurrences/{occurrence['occurrence_id']}/complete", headers=_headers(child))
    assert complete.status_code == 200, complete.text


def test_wish_request_reject_does_not_spend_then_approve_is_idempotent(client):
    parent, _, family_id = _register_parent(client, "wish-utility")
    child, child_id = _register_child_and_attach(client, parent, "wish-utility-child")
    _earn_personal(client, parent, child, child_id, family_id, "wish-utility-child")
    offer = client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(parent),
        json={"title": "Choisir le film", "flame_cost": 1})
    assert offer.status_code == 201, offer.text
    offer_id = offer.json()["data"]["id"]
    before = _resources(client, child, family_id)
    request = client.post(f"{API}/families/{family_id}/wishes/requests", headers=_headers(child), json={"offer_id": offer_id})
    assert request.status_code == 201
    request_id = request.json()["data"]["id"]
    assert _resources(client, child, family_id)["flames"] == before["flames"]
    assert client.post(f"{API}/families/{family_id}/wishes/requests/{request_id}/reject", headers=_headers(parent)).status_code == 200
    assert _resources(client, child, family_id)["flames"] == before["flames"]
    request = client.post(f"{API}/families/{family_id}/wishes/requests", headers=_headers(child), json={"offer_id": offer_id})
    request_id = request.json()["data"]["id"]
    assert client.post(f"{API}/families/{family_id}/wishes/requests/{request_id}/approve", headers=_headers(parent)).status_code == 200
    after = _resources(client, child, family_id)
    assert after["flames"] == before["flames"] - 1
    assert client.post(f"{API}/families/{family_id}/wishes/requests/{request_id}/approve", headers=_headers(parent)).status_code == 200
    assert _resources(client, child, family_id)["flames"] == after["flames"]


def test_child_cannot_manage_offers_and_family_access_isolated(client):
    parent, _, family_id = _register_parent(client, "wish-security")
    child, _ = _register_child_and_attach(client, parent, "wish-security-child")
    assert client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(child),
        json={"title": "Forbidden", "flame_cost": 1}).status_code == 403
    assert client.get(f"{API}/families/{family_id + 999}/resources/me", headers=_headers(child)).status_code == 404
    catalog = client.get(f"{API}/families/{family_id}/chests/catalog", headers=_headers(child))
    assert catalog.status_code == 200
    assert [(x["chest_type"], x["crystal_cost"], x["drop_count"]) for x in catalog.json()["data"]["chests"]] == [
        ("COMMON", 3, 1), ("RARE", 8, 3), ("EPIC", 15, 6),
    ]
    assert client.get(f"{API}/families/{family_id}/collection", headers=_headers(parent)).status_code == 200


def test_chest_open_debits_once_persists_loot_and_updates_existing_collection(client, monkeypatch):
    parent, _, family_id = _register_parent(client, "chest-utility")
    child, child_id = _register_child_and_attach(client, parent, "chest-utility-child")
    _earn_personal(client, parent, child, child_id, family_id, "chest-utility-child")
    monkeypatch.setattr("app.services.resource_utility_service.CHEST_RANDOM", FixedChestRandom())
    before = _resources(client, child, family_id)
    body = {"chest_type": "COMMON", "idempotency_key": "common-open-0001"}
    opened = client.post(f"{API}/families/{family_id}/chests/open", headers=_headers(child), json=body)
    assert opened.status_code == 200, opened.text
    data = opened.json()["data"]
    assert data["crystal_cost"] == 3
    assert sum(row["quantity"] for row in data["drops"]) == 1
    assert _resources(client, child, family_id)["crystals"] == before["crystals"] - 3
    retry = client.post(f"{API}/families/{family_id}/chests/open", headers=_headers(child), json=body)
    assert retry.status_code == 200
    assert retry.json()["data"] == data
    history = client.get(f"{API}/families/{family_id}/chests/opens", headers=_headers(child))
    assert history.status_code == 200
    assert history.json()["data"]["items"] == [data]
    assert _resources(client, child, family_id)["crystals"] == before["crystals"] - 3
    collection = client.get(f"{API}/families/{family_id}/collection", headers=_headers(child)).json()["data"]["items"]
    assert any(row["collectible_key"] == data["drops"][0]["collectible_key"] and row["quantity"] == 1 for row in collection)


def test_insufficient_crystals_does_not_create_chest_or_spend(client):
    parent, _, family_id = _register_parent(client, "chest-insufficient")
    child, _ = _register_child_and_attach(client, parent, "chest-insufficient-child")
    response = client.post(f"{API}/families/{family_id}/chests/open", headers=_headers(child),
        json={"chest_type": "EPIC", "idempotency_key": "epic-open-00001"})
    assert response.status_code == 409
    assert _resources(client, child, family_id)["crystals"] == 0


def test_insufficient_wish_approval_leaves_request_pending_and_spend_absent(client):
    parent, _, family_id = _register_parent(client, "wish-insufficient")
    child, _ = _register_child_and_attach(client, parent, "wish-insufficient-child")
    offer = client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(parent),
        json={"title": "Un souhait", "flame_cost": 1}).json()["data"]
    req = client.post(f"{API}/families/{family_id}/wishes/requests", headers=_headers(child),
        json={"offer_id": offer["id"]}).json()["data"]
    denied = client.post(f"{API}/families/{family_id}/wishes/requests/{req['id']}/approve", headers=_headers(parent))
    assert denied.status_code == 409
    assert _resources(client, child, family_id)["flames"] == 0
    listed = client.get(f"{API}/families/{family_id}/wishes/requests", headers=_headers(parent)).json()["data"]["items"]
    assert next(row for row in listed if row["id"] == req["id"])["status"] == "PENDING"


def test_parent_can_obtain_own_wish_once(client):
    parent, parent_id, family_id = _register_parent(client, "wish-parent-own")
    task = _create_task(client, parent, family_id, {"title": "Parent mission", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [parent_id]})
    occurrence = next(row for row in _today(client, parent, family_id, date.today())["items"] if row["task_id"] == task["id"])
    assert client.post(f"{API}/task-occurrences/{occurrence['occurrence_id']}/complete", headers=_headers(parent)).status_code == 200
    offer = client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(parent),
        json={"title": "Choisir le film", "flame_cost": 1}).json()["data"]
    before = _resources(client, parent, family_id)["flames"]
    body = {"offer_id": offer["id"], "idempotency_key": "parent-obtain-00001"}
    first = client.post(f"{API}/families/{family_id}/wishes/obtain", headers=_headers(parent), json=body)
    assert first.status_code == 200 and first.json()["data"]["status"] == "APPROVED"
    second = client.post(f"{API}/families/{family_id}/wishes/obtain", headers=_headers(parent), json=body)
    assert second.status_code == 200 and second.json()["data"]["id"] == first.json()["data"]["id"]
    assert _resources(client, parent, family_id)["flames"] == before - 1


def test_concurrent_wish_approvals_cannot_overspend_one_personal_balance(client):
    parent, _, family_id = _register_parent(client, "wish-concurrent")
    child, child_id = _register_child_and_attach(client, parent, "wish-concurrent-child")
    _earn_personal(client, parent, child, child_id, family_id, "wish-concurrent-child")
    offer = client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(parent),
        json={"title": "Coût partagé", "flame_cost": 2}).json()["data"]
    requests = [client.post(f"{API}/families/{family_id}/wishes/requests", headers=_headers(child),
        json={"offer_id": offer["id"]}).json()["data"]["id"] for _ in range(2)]

    def approve(request_id):
        return client.post(f"{API}/families/{family_id}/wishes/requests/{request_id}/approve",
            headers=_headers(parent)).status_code

    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(approve, requests))
    assert statuses.count(200) == 1
    assert statuses.count(409) == 1
    assert _resources(client, child, family_id)["flames"] == 1


def test_concurrent_chest_opens_cannot_double_spend_same_crystals(client):
    parent, _, family_id = _register_parent(client, "chest-concurrent")
    child, child_id = _register_child_and_attach(client, parent, "chest-concurrent-child")
    task = _create_task(client, parent, family_id, {"title": "Quest crystal", "scope": "HOUSE", "kind": "QUEST",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id]})
    occurrence = next(row for row in _today(client, child, family_id, date.today())["items"] if row["task_id"] == task["id"])
    occurrence_id = occurrence["occurrence_id"]
    assert client.post(f"{API}/task-occurrences/{occurrence_id}/start", headers=_headers(child)).status_code == 200
    assert client.post(f"{API}/task-occurrences/{occurrence_id}/complete", headers=_headers(child)).status_code == 200
    assert _resources(client, child, family_id)["crystals"] == 3

    def open_one(key):
        return client.post(f"{API}/families/{family_id}/chests/open", headers=_headers(child),
            json={"chest_type": "COMMON", "idempotency_key": key}).status_code

    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(open_one, ("chest-concurrent-A", "chest-concurrent-B")))
    assert statuses.count(200) == 1
    assert statuses.count(409) == 1
    assert _resources(client, child, family_id)["crystals"] == 0


def test_reopen_without_resource_spend_revokes_bundle(client):
    parent, _, family_id = _register_parent(client, "reopen-unspent")
    child, child_id = _register_child_and_attach(client, parent, "reopen-unspent-child")
    task = _create_task(client, parent, family_id, {"title": "Mission reward", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id]})
    occurrence = next(row for row in _today(client, child, family_id, date.today())["items"] if row["task_id"] == task["id"])
    occurrence_id = occurrence["occurrence_id"]
    assert client.post(f"{API}/task-occurrences/{occurrence_id}/complete", headers=_headers(child)).status_code == 200
    before = _resources(client, child, family_id)
    reopened = client.post(f"{API}/task-occurrences/{occurrence_id}/reopen", headers=_headers(parent))
    assert reopened.status_code == 200
    after = _resources(client, child, family_id)
    assert (after["taskoday_points"], after["flames"], after["crystals"]) == (0, 0, 0)
    assert before["taskoday_points"] > 0


def test_reopen_is_refused_without_mutation_after_reward_was_spent(client):
    parent, _, family_id = _register_parent(client, "reopen-spend")
    child, child_id = _register_child_and_attach(client, parent, "reopen-spend-child")
    task = _create_task(client, parent, family_id, {"title": "Mission reward", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id]})
    occurrence = next(row for row in _today(client, child, family_id, date.today())["items"] if row["task_id"] == task["id"])
    occurrence_id = occurrence["occurrence_id"]
    assert client.post(f"{API}/task-occurrences/{occurrence_id}/complete", headers=_headers(child)).status_code == 200
    offer = client.post(f"{API}/families/{family_id}/wishes/offers", headers=_headers(parent), json={"title": "Wish", "flame_cost": 3})
    offer_id = offer.json()["data"]["id"]
    request = client.post(f"{API}/families/{family_id}/wishes/requests", headers=_headers(child), json={"offer_id": offer_id})
    assert client.post(f"{API}/families/{family_id}/wishes/requests/{request.json()['data']['id']}/approve", headers=_headers(parent)).status_code == 200
    before = client.get(f"{API}/families/{family_id}/rewards/me", headers=_headers(child)).json()["data"]
    reopened = client.post(f"{API}/task-occurrences/{occurrence_id}/reopen", headers=_headers(parent))
    assert reopened.status_code == 409
    assert "récompenses a déjà été utilisée" in reopened.json()["error"]["message"]
    after = client.get(f"{API}/families/{family_id}/rewards/me", headers=_headers(child)).json()["data"]
    assert after["flames"] == before["flames"]
    latest = next(row for row in _today(client, parent, family_id, date.today())["items"] if row["task_id"] == task["id"])
    assert latest["status"] == "COMPLETED"
