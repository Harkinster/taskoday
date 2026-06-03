API = "/api/v1"


def _register_parent(client, email: str, family_name: str, password: str = "supersecret123") -> str:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": email,
            "password": password,
            "family_name": family_name,
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    return response.json()["access_token"]


def _register_child(client, email: str, display_name: str, password: str = "childsecret123") -> str:
    response = client.post(
        f"{API}/auth/register-child",
        json={
            "email": email,
            "password": password,
            "display_name": display_name,
        },
    )
    assert response.status_code == 201
    return response.json()["access_token"]


def _attach_child(client, parent_token: str, child_token: str) -> int:
    pairing = client.post(f"{API}/pairing/generate-code", headers={"Authorization": f"Bearer {child_token}"})
    assert pairing.status_code == 200

    response = client.post(
        f"{API}/pairing/attach-child",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"code": pairing.json()["data"]["code"]},
    )
    assert response.status_code == 200
    return response.json()["data"]["child_id"]


def test_currency_award_and_routine_revoke_rules(client) -> None:
    parent_token = _register_parent(client, "points.parent@example.com", "Famille Points")
    child_token = _register_child(client, "points.child@example.com", "Yuna")
    child_id = _attach_child(client, parent_token, child_token)

    routine = client.post(
        f"{API}/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Routine", "repeat_type": "daily"},
    )
    assert routine.status_code == 200
    routine_id = routine.json()["data"]["id"]

    mission = client.post(
        f"{API}/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Mission", "due_date": "2026-04-27T00:00:00Z"},
    )
    assert mission.status_code == 200
    mission_id = mission.json()["data"]["id"]

    quest = client.post(
        f"{API}/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Quest", "xp_reward": 30},
    )
    assert quest.status_code == 200
    quest_id = quest.json()["data"]["id"]

    assert client.post(
        f"{API}/routines/{routine_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    ).status_code == 200
    assert client.post(
        f"{API}/missions/{mission_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    ).status_code == 200
    assert client.post(
        f"{API}/quests/{quest_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    ).status_code == 200

    balance_1 = client.get(
        f"{API}/children/{child_id}/flammeches",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert balance_1.status_code == 200
    assert balance_1.json()["data"]["balance"] == 20

    history_1 = client.get(
        f"{API}/children/{child_id}/flammeches/history",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert history_1.status_code == 200
    tx_1 = history_1.json()["data"]["transactions"]
    assert len(tx_1) == 3
    assert sorted(tx["amount"] for tx in tx_1) == [2, 6, 12]

    assert client.post(
        f"{API}/routines/{routine_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    ).status_code == 200
    balance_after_duplicate = client.get(
        f"{API}/children/{child_id}/flammeches",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert balance_after_duplicate.json()["data"]["balance"] == 20

    assert client.post(
        f"{API}/routines/{routine_id}/uncomplete",
        headers={"Authorization": f"Bearer {child_token}"},
    ).status_code == 200

    balance_2 = client.get(
        f"{API}/children/{child_id}/flammeches",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert balance_2.json()["data"]["balance"] == 18


def test_currency_access_control(client) -> None:
    parent_a = _register_parent(client, "points.parent.a@example.com", "Famille PA")
    parent_b = _register_parent(client, "points.parent.b@example.com", "Famille PB")

    child_a1_token = _register_child(client, "points.a1@example.com", "A1")
    child_a2_token = _register_child(client, "points.a2@example.com", "A2")
    child_b1_token = _register_child(client, "points.b1@example.com", "B1")
    child_a1_id = _attach_child(client, parent_a, child_a1_token)
    child_a2_id = _attach_child(client, parent_a, child_a2_token)
    child_b1_id = _attach_child(client, parent_b, child_b1_token)

    forbidden_parent = client.get(
        f"{API}/children/{child_a1_id}/flammeches",
        headers={"Authorization": f"Bearer {parent_b}"},
    )
    assert forbidden_parent.status_code == 403

    own_currency = client.get(
        f"{API}/children/{child_a1_id}/flammeches",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert own_currency.status_code == 200

    sibling_currency = client.get(
        f"{API}/children/{child_a2_id}/flammeches",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert sibling_currency.status_code == 403

    other_family_currency = client.get(
        f"{API}/children/{child_b1_id}/flammeches",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert other_family_currency.status_code == 403
