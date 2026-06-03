def _register_parent(client, email: str, family_name: str, password: str = "supersecret123") -> str:
    response = client.post(
        "/api/v1/auth/register-parent",
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
        "/api/v1/auth/register-child",
        json={
            "email": email,
            "password": password,
            "display_name": display_name,
            "birth_date": "2014-07-03",
        },
    )
    assert response.status_code == 201
    return response.json()["access_token"]


def _attach_child(client, parent_token: str, child_token: str) -> int:
    pairing = client.post("/api/v1/pairing/generate-code", headers={"Authorization": f"Bearer {child_token}"})
    assert pairing.status_code == 200
    code = pairing.json()["data"]["code"]

    attached = client.post(
        "/api/v1/pairing/attach-child",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"code": code},
    )
    assert attached.status_code == 200
    return attached.json()["data"]["child_id"]


def _create_and_complete_routine(client, parent_token: str, child_token: str, child_id: int, title: str) -> int:
    routine = client.post(
        f"/api/v1/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": title, "repeat_type": "daily", "can_child_delete": False},
    )
    assert routine.status_code == 200
    routine_id = routine.json()["data"]["id"]
    complete = client.post(f"/api/v1/routines/{routine_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete.status_code == 200
    return routine_id


def _create_and_complete_mission(client, parent_token: str, child_token: str, child_id: int) -> int:
    mission = client.post(
        f"/api/v1/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Ranger le bureau"},
    )
    assert mission.status_code == 200
    mission_id = mission.json()["data"]["id"]
    complete = client.post(f"/api/v1/missions/{mission_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete.status_code == 200
    return mission_id


def _create_and_complete_quest(client, parent_token: str, child_token: str, child_id: int) -> int:
    quest = client.post(
        f"/api/v1/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Lire 10 minutes", "xp_reward": 25},
    )
    assert quest.status_code == 200
    quest_id = quest.json()["data"]["id"]
    complete = client.post(f"/api/v1/quests/{quest_id}/complete", headers={"Authorization": f"Bearer {child_token}"})
    assert complete.status_code == 200
    return quest_id


def _setup_family(client, suffix: str):
    parent_token = _register_parent(client, f"game.parent.{suffix}@example.com", f"Famille {suffix}")
    child_token = _register_child(client, f"game.child.{suffix}@example.com", suffix)
    child_id = _attach_child(client, parent_token, child_token)
    return parent_token, child_token, child_id


def _item_quantity(items, item_key: str) -> int:
    for item in items:
        if item["key"] == item_key:
            return item["quantity"]
    return 0


def test_task_completion_adds_guardian_xp_flammeches_and_chest_progress(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "progress")

    _create_and_complete_routine(client, parent_token, child_token, child_id, "Se brosser les dents")
    _create_and_complete_mission(client, parent_token, child_token, child_id)
    _create_and_complete_quest(client, parent_token, child_token, child_id)

    progress = client.get(f"/api/v1/children/{child_id}/progress", headers={"Authorization": f"Bearer {child_token}"})
    assert progress.status_code == 200
    payload = progress.json()["data"]
    assert payload["guardian"]["xp"] == 50
    assert payload["wallet"]["flammeches"] == 20
    assert payload["chest_progress"]["points"] == 4
    assert payload["chest_progress"]["unopened_chests"] == 1

    _create_and_complete_routine(client, parent_token, child_token, child_id, "Preparer le sac")

    chests = client.get(f"/api/v1/children/{child_id}/chests", headers={"Authorization": f"Bearer {child_token}"})
    assert chests.status_code == 200
    chest_types = sorted(chest["type"] for chest in chests.json()["data"]["chests"])
    assert chest_types == ["rare", "simple"]

    flammeches = client.get(f"/api/v1/children/{child_id}/flammeches", headers={"Authorization": f"Bearer {child_token}"})
    assert flammeches.status_code == 200
    assert flammeches.json()["data"]["balance"] == 22


def test_open_chest_adds_items_hatch_consumes_items_and_dragon_evolves(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "loot")
    _create_and_complete_quest(client, parent_token, child_token, child_id)

    chests = client.get(f"/api/v1/children/{child_id}/chests", headers={"Authorization": f"Bearer {child_token}"})
    rare_chest = next(chest for chest in chests.json()["data"]["chests"] if chest["type"] == "rare")

    opened = client.post(
        f"/api/v1/children/{child_id}/chests/{rare_chest['id']}/open",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert opened.status_code == 200
    assert opened.json()["data"]["granted_egg"]["egg_key"] == "oeuf_braise"

    inventory = client.get(f"/api/v1/children/{child_id}/inventory", headers={"Authorization": f"Bearer {child_token}"})
    items = inventory.json()["data"]["items"]
    assert _item_quantity(items, "pomme_dragon") == 8
    assert _item_quantity(items, "petit_cristal") == 6
    assert _item_quantity(items, "rune_ancienne") == 3

    eggs = client.get(f"/api/v1/children/{child_id}/eggs", headers={"Authorization": f"Bearer {child_token}"})
    egg_id = eggs.json()["data"]["eggs"][0]["id"]
    hatched = client.post(
        f"/api/v1/children/{child_id}/eggs/{egg_id}/hatch",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert hatched.status_code == 200
    assert hatched.json()["data"]["dragon"]["dragon_key"] == "dragon_braise"
    assert hatched.json()["data"]["dragon"]["stage"] == "baby"

    dragons = client.get(f"/api/v1/children/{child_id}/dragons", headers={"Authorization": f"Bearer {child_token}"})
    dragon_id = dragons.json()["data"]["dragons"][0]["id"]
    evolved = client.post(
        f"/api/v1/children/{child_id}/dragons/{dragon_id}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert evolved.status_code == 200
    assert evolved.json()["data"]["dragon"]["stage"] == "young"

    remaining = evolved.json()["data"]["inventory"]["items"]
    assert _item_quantity(remaining, "pomme_dragon") == 0
    assert _item_quantity(remaining, "petit_cristal") == 0
    assert _item_quantity(remaining, "rune_ancienne") == 1


def test_wish_alias_approval_spends_flammeches_and_creates_scroll(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "wish")
    _create_and_complete_quest(client, parent_token, child_token, child_id)

    reward = client.post(
        f"/api/v1/children/{child_id}/wishes",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Choisir le dessert", "description": "Bonus du soir", "cost_scales": 10, "is_active": True},
    )
    assert reward.status_code == 201

    request = client.post(
        f"/api/v1/wishes/{reward.json()['data']['id']}/requests",
        headers={"Authorization": f"Bearer {child_token}"},
        json={},
    )
    assert request.status_code == 201
    assert request.json()["data"]["status"] == "pending"

    approved = client.patch(
        f"/api/v1/reward-requests/{request.json()['data']['id']}",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"status": "approved"},
    )
    assert approved.status_code == 200
    assert approved.json()["data"]["coupon"]["status"] == "available"

    flammeches = client.get(f"/api/v1/children/{child_id}/flammeches", headers={"Authorization": f"Bearer {child_token}"})
    assert flammeches.json()["data"]["balance"] == 2

    scrolls = client.get(f"/api/v1/children/{child_id}/scrolls", headers={"Authorization": f"Bearer {child_token}"})
    assert scrolls.status_code == 200
    assert scrolls.json()["data"]["scrolls"][0]["status"] == "available"
