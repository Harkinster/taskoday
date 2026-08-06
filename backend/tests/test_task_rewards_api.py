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


def _setup_family(client, suffix: str):
    parent_token = _register_parent(client, f"task-rewards.parent.{suffix}@example.com", f"Famille Rewards {suffix}")
    child_token = _register_child(client, f"task-rewards.child.{suffix}@example.com", f"Rewards {suffix}")
    child_id = _attach_child(client, parent_token, child_token)
    return parent_token, child_token, child_id


def _create_tasks(client, parent_token: str, child_id: int, *, quest_xp: int = 42) -> dict[str, int]:
    parent_headers = {"Authorization": f"Bearer {parent_token}"}

    routine = client.post(
        f"{API}/children/{child_id}/routines",
        headers=parent_headers,
        json={"title": "Routine reward", "repeat_type": "daily"},
    )
    assert routine.status_code == 200

    mission = client.post(
        f"{API}/children/{child_id}/missions",
        headers=parent_headers,
        json={"title": "Mission reward"},
    )
    assert mission.status_code == 200

    quest = client.post(
        f"{API}/children/{child_id}/quests",
        headers=parent_headers,
        json={"title": "Quest reward", "xp_reward": quest_xp},
    )
    assert quest.status_code == 200

    return {
        "routine": routine.json()["data"]["id"],
        "mission": mission.json()["data"]["id"],
        "quest": quest.json()["data"]["id"],
    }


def _progress(client, child_token: str, child_id: int) -> dict:
    response = client.get(
        f"{API}/children/{child_id}/progress",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert response.status_code == 200
    return response.json()["data"]


def test_task_rewards_use_expected_values_and_completion_is_idempotent(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "values")
    task_ids = _create_tasks(client, parent_token, child_id, quest_xp=42)
    child_headers = {"Authorization": f"Bearer {child_token}"}

    routine = client.post(f"{API}/routines/{task_ids['routine']}/complete", headers=child_headers)
    assert routine.status_code == 200
    assert routine.json()["data"]["award"]["guardian_xp_awarded"] == 5
    assert routine.json()["data"]["award"]["flammeches_awarded"] == 2
    assert routine.json()["data"]["award"]["crystals_awarded"] == 1
    assert routine.json()["data"]["award"]["chest_points_awarded"] == 1
    routine_duplicate = client.post(f"{API}/routines/{task_ids['routine']}/complete", headers=child_headers)
    assert routine_duplicate.status_code == 200
    assert routine_duplicate.json()["data"]["award"] is None

    mission = client.post(f"{API}/missions/{task_ids['mission']}/complete", headers=child_headers)
    assert mission.status_code == 200
    assert mission.json()["data"]["award"]["guardian_xp_awarded"] == 15
    assert mission.json()["data"]["award"]["flammeches_awarded"] == 6
    assert mission.json()["data"]["award"]["crystals_awarded"] == 3
    assert mission.json()["data"]["award"]["chest_points_awarded"] == 3
    mission_duplicate = client.post(f"{API}/missions/{task_ids['mission']}/complete", headers=child_headers)
    assert mission_duplicate.status_code == 200
    assert mission_duplicate.json()["data"]["award"] is None

    quest = client.post(f"{API}/quests/{task_ids['quest']}/complete", headers=child_headers)
    assert quest.status_code == 200
    assert quest.json()["data"]["award"]["guardian_xp_awarded"] == 42
    assert quest.json()["data"]["award"]["flammeches_awarded"] == 12
    assert quest.json()["data"]["award"]["crystals_awarded"] == 6
    assert quest.json()["data"]["award"]["chest_points_awarded"] == 0
    assert [chest["type"] for chest in quest.json()["data"]["award"]["granted_chests"]] == ["rare"]
    quest_duplicate = client.post(f"{API}/quests/{task_ids['quest']}/complete", headers=child_headers)
    assert quest_duplicate.status_code == 200
    assert quest_duplicate.json()["data"]["award"] is None

    progress = _progress(client, child_token, child_id)
    assert progress["guardian"]["xp"] == 62
    assert progress["wallet"] == {"flammeches": 20, "crystals": 10}
    assert progress["chest_progress"]["points"] == 4
    assert progress["chest_progress"]["unopened_chests"] == 1


def test_task_uncomplete_rolls_back_rewards_symmetrically_for_all_task_types(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "rollback")
    task_ids = _create_tasks(client, parent_token, child_id, quest_xp=42)
    child_headers = {"Authorization": f"Bearer {child_token}"}

    assert client.post(f"{API}/routines/{task_ids['routine']}/complete", headers=child_headers).status_code == 200
    assert client.post(f"{API}/missions/{task_ids['mission']}/complete", headers=child_headers).status_code == 200
    assert client.post(f"{API}/quests/{task_ids['quest']}/complete", headers=child_headers).status_code == 200

    quest_uncomplete = client.post(f"{API}/quests/{task_ids['quest']}/uncomplete", headers=child_headers)
    assert quest_uncomplete.status_code == 200
    assert quest_uncomplete.json()["data"]["completed"] is False
    progress = _progress(client, child_token, child_id)
    assert progress["guardian"]["xp"] == 20
    assert progress["wallet"] == {"flammeches": 8, "crystals": 4}
    assert progress["chest_progress"]["points"] == 4
    assert progress["chest_progress"]["unopened_chests"] == 0

    mission_uncomplete = client.post(f"{API}/missions/{task_ids['mission']}/uncomplete", headers=child_headers)
    assert mission_uncomplete.status_code == 200
    assert mission_uncomplete.json()["data"]["completed"] is False
    progress = _progress(client, child_token, child_id)
    assert progress["guardian"]["xp"] == 5
    assert progress["wallet"] == {"flammeches": 2, "crystals": 1}
    assert progress["chest_progress"]["points"] == 1

    routine_uncomplete = client.post(f"{API}/routines/{task_ids['routine']}/uncomplete", headers=child_headers)
    assert routine_uncomplete.status_code == 200
    assert routine_uncomplete.json()["data"]["completed"] is False
    progress = _progress(client, child_token, child_id)
    assert progress["guardian"]["xp"] == 0
    assert progress["wallet"] == {"flammeches": 0, "crystals": 0}
    assert progress["chest_progress"]["points"] == 0

    assert client.post(f"{API}/quests/{task_ids['quest']}/uncomplete", headers=child_headers).status_code == 200
    assert client.post(f"{API}/missions/{task_ids['mission']}/uncomplete", headers=child_headers).status_code == 200
    assert client.post(f"{API}/routines/{task_ids['routine']}/uncomplete", headers=child_headers).status_code == 200


def test_task_uncomplete_refuses_when_generated_chest_was_opened(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "opened-chest")
    task_ids = _create_tasks(client, parent_token, child_id, quest_xp=42)
    child_headers = {"Authorization": f"Bearer {child_token}"}

    assert client.post(f"{API}/quests/{task_ids['quest']}/complete", headers=child_headers).status_code == 200
    chests = client.get(f"{API}/children/{child_id}/chests", headers=child_headers)
    assert chests.status_code == 200
    rare_chest = next(chest for chest in chests.json()["data"]["chests"] if chest["type"] == "rare")
    opened = client.post(f"{API}/children/{child_id}/chests/{rare_chest['id']}/open", headers=child_headers)
    assert opened.status_code == 200

    refused = client.post(f"{API}/quests/{task_ids['quest']}/uncomplete", headers=child_headers)
    assert refused.status_code == 409
    assert "coffre genere a deja ete ouvert" in refused.json()["error"]["message"].lower()

    progress = _progress(client, child_token, child_id)
    assert progress["guardian"]["xp"] == 42
    assert progress["wallet"] == {"flammeches": 12, "crystals": 6}
    assert progress["chest_progress"]["opened_chests"] == 1
    quests = client.get(f"{API}/children/{child_id}/quests", headers=child_headers)
    assert quests.json()["data"][0]["status"] == "completed"
