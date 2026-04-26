from datetime import date


def _register_parent(client, email: str, family_name: str, password: str = "supersecret123") -> str:
    response = client.post(
        "/auth/register-parent",
        json={
            "email": email,
            "password": password,
            "family_name": family_name,
        },
    )
    assert response.status_code == 201
    return response.json()["access_token"]


def _login(client, email: str, password: str) -> str:
    response = client.post(
        "/auth/login",
        json={"email": email, "password": password},
    )
    assert response.status_code == 200
    return response.json()["access_token"]


def _create_child(client, parent_token: str, email: str, display_name: str) -> dict:
    response = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "email": email,
            "password": "childsecret123",
            "display_name": display_name,
        },
    )
    assert response.status_code == 201
    return response.json()


def test_points_award_and_revoke_rules(client) -> None:
    parent_token = _register_parent(client, "points.parent@example.com", "Famille Points")
    child = _create_child(client, parent_token, "points.child@example.com", "Yuna")
    child_id = child["id"]
    target_date = date(2026, 4, 27)

    routine = client.post(
        f"/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Routine",
            "day_part": "MATIN",
            "frequency": "DAILY",
            "is_active": True,
        },
    )
    assert routine.status_code == 201
    routine_id = routine.json()["id"]

    mission = client.post(
        f"/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Mission",
            "day_part": "MIDI",
            "scheduled_date": target_date.isoformat(),
            "is_active": True,
        },
    )
    assert mission.status_code == 201
    mission_id = mission.json()["id"]

    quest = client.post(
        f"/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Quest",
            "day_part": "SOIR",
            "is_active": True,
        },
    )
    assert quest.status_code == 201
    quest_id = quest.json()["id"]

    # complete: +1 +2 +3
    assert client.patch(
        f"/planning/routine/{routine_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200
    assert client.patch(
        f"/planning/mission/{mission_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200
    assert client.patch(
        f"/planning/quest/{quest_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200

    balance_1 = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert balance_1.status_code == 200
    assert balance_1.json()["balance"] == 6

    history_1 = client.get(
        f"/children/{child_id}/points/history",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert history_1.status_code == 200
    tx_1 = history_1.json()["transactions"]
    assert len(tx_1) == 3
    assert sorted(tx["amount"] for tx in tx_1) == [1, 2, 3]

    # idempotent complete: no extra points
    assert client.patch(
        f"/planning/routine/{routine_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200
    balance_after_duplicate = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert balance_after_duplicate.json()["balance"] == 6

    # uncomplete mission: -2
    assert client.patch(
        f"/planning/mission/{mission_id}/uncomplete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200

    balance_2 = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert balance_2.json()["balance"] == 4

    history_2 = client.get(
        f"/children/{child_id}/points/history",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    tx_2 = history_2.json()["transactions"]
    assert len(tx_2) == 4
    assert any(tx["amount"] == -2 for tx in tx_2)

    # idempotent uncomplete: no extra negative points
    assert client.patch(
        f"/planning/mission/{mission_id}/uncomplete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    ).status_code == 200
    balance_3 = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert balance_3.json()["balance"] == 4


def test_points_access_control(client) -> None:
    parent_a = _register_parent(client, "points.parent.a@example.com", "Famille PA")
    parent_b = _register_parent(client, "points.parent.b@example.com", "Famille PB")

    child_a1 = _create_child(client, parent_a, "points.a1@example.com", "A1")
    child_a2 = _create_child(client, parent_a, "points.a2@example.com", "A2")
    child_b1 = _create_child(client, parent_b, "points.b1@example.com", "B1")

    # parent B cannot read parent A child points
    forbidden_parent = client.get(
        f"/children/{child_a1['id']}/points",
        headers={"Authorization": f"Bearer {parent_b}"},
    )
    assert forbidden_parent.status_code == 404

    child_a1_token = _login(client, "points.a1@example.com", "childsecret123")

    own_points = client.get(
        f"/children/{child_a1['id']}/points",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert own_points.status_code == 200

    sibling_points = client.get(
        f"/children/{child_a2['id']}/points",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert sibling_points.status_code == 403

    other_family_points = client.get(
        f"/children/{child_b1['id']}/points",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert other_family_points.status_code == 403

