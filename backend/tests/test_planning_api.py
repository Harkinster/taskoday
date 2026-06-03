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


def test_active_task_endpoints_create_list_and_complete(client) -> None:
    parent_token = _register_parent(client, "planning.parent@example.com", "Famille Planning")
    child_token = _register_child(client, "planning.child@example.com", "Lina")
    child_id = _attach_child(client, parent_token, child_token)

    routine = client.post(
        f"{API}/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Brosser les dents", "description": "Matin", "repeat_type": "daily"},
    )
    assert routine.status_code == 200
    routine_id = routine.json()["data"]["id"]

    mission = client.post(
        f"{API}/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Piscine", "description": "Sac pret", "due_date": "2026-04-27T00:00:00Z"},
    )
    assert mission.status_code == 200
    mission_id = mission.json()["data"]["id"]

    quest = client.post(
        f"{API}/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Lire 10 min", "description": "Facultatif", "xp_reward": 30},
    )
    assert quest.status_code == 200
    quest_id = quest.json()["data"]["id"]

    routines = client.get(f"{API}/children/{child_id}/routines", headers={"Authorization": f"Bearer {child_token}"})
    assert routines.status_code == 200
    assert {item["title"] for item in routines.json()["data"]} == {"Brosser les dents"}

    missions = client.get(f"{API}/children/{child_id}/missions", headers={"Authorization": f"Bearer {child_token}"})
    assert missions.status_code == 200
    assert missions.json()["data"][0]["due_date"].startswith("2026-04-27")

    quests = client.get(f"{API}/children/{child_id}/quests", headers={"Authorization": f"Bearer {child_token}"})
    assert quests.status_code == 200
    assert quests.json()["data"][0]["xp_reward"] == 30

    complete_routine = client.post(f"{API}/routines/{routine_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete_routine.status_code == 200
    assert complete_routine.json()["data"]["completed"] is True

    uncomplete_routine = client.post(
        f"{API}/routines/{routine_id}/uncomplete",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert uncomplete_routine.status_code == 200
    assert uncomplete_routine.json()["data"]["completed"] is False

    complete_mission = client.post(f"{API}/missions/{mission_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete_mission.status_code == 200
    assert complete_mission.json()["data"]["completed"] is True

    complete_quest = client.post(f"{API}/quests/{quest_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete_quest.status_code == 200
    assert complete_quest.json()["data"]["completed"] is True


def test_active_task_endpoint_access_control(client) -> None:
    parent_a_token = _register_parent(client, "planning.a@example.com", "Famille A")
    parent_b_token = _register_parent(client, "planning.b@example.com", "Famille B")

    child_a1_token = _register_child(client, "planning.a1@example.com", "Ana")
    child_a2_token = _register_child(client, "planning.a2@example.com", "Eli")
    child_b1_token = _register_child(client, "planning.b1@example.com", "Tom")
    child_a1_id = _attach_child(client, parent_a_token, child_a1_token)
    child_a2_id = _attach_child(client, parent_a_token, child_a2_token)
    child_b1_id = _attach_child(client, parent_b_token, child_b1_token)

    routine_a1 = client.post(
        f"{API}/children/{child_a1_id}/routines",
        headers={"Authorization": f"Bearer {parent_a_token}"},
        json={"title": "Routine A1", "repeat_type": "daily"},
    )
    assert routine_a1.status_code == 200
    routine_a1_id = routine_a1.json()["data"]["id"]

    forbidden_create = client.post(
        f"{API}/children/{child_a1_id}/routines",
        headers={"Authorization": f"Bearer {parent_b_token}"},
        json={"title": "No Access", "repeat_type": "daily"},
    )
    assert forbidden_create.status_code == 403

    own_routines = client.get(
        f"{API}/children/{child_a1_id}/routines",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert own_routines.status_code == 200

    own_complete = client.post(
        f"{API}/routines/{routine_a1_id}/complete",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert own_complete.status_code == 200

    sibling_routines = client.get(
        f"{API}/children/{child_a2_id}/routines",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert sibling_routines.status_code == 403

    other_family_routines = client.get(
        f"{API}/children/{child_b1_id}/routines",
        headers={"Authorization": f"Bearer {child_a1_token}"},
    )
    assert other_family_routines.status_code == 403
