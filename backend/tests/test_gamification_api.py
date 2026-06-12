from concurrent.futures import ThreadPoolExecutor

import pytest

import app.services.gamification_service as gamification_service


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


def _loot_item(loot, item_key: str) -> dict:
    return next(item for item in loot if item["key"] == item_key)


def _open_unopened_rare_chests(client, child_token: str, child_id: int) -> list[dict]:
    chests = client.get(
        f"/api/v1/children/{child_id}/chests",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["chests"]
    opened = []
    for chest in chests:
        if chest["type"] == "rare" and chest["status"] == "unopened":
            response = client.post(
                f"/api/v1/children/{child_id}/chests/{chest['id']}/open",
                headers={"Authorization": f"Bearer {child_token}"},
            )
            assert response.status_code == 200
            opened.append(response.json()["data"])
    return opened


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
    assert payload["wallet"]["crystals"] == 10
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


def test_routine_uncomplete_rolls_back_unopened_chest_and_rejects_opened_chest(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "routine-rollback")
    headers = {"Authorization": f"Bearer {child_token}"}

    _create_and_complete_mission(client, parent_token, child_token, child_id)
    _create_and_complete_routine(client, parent_token, child_token, child_id, "Premier point")
    routine_id = _create_and_complete_routine(client, parent_token, child_token, child_id, "Coffre genere")
    uncomplete_path = f"/api/v1/routines/{routine_id}/uncomplete"
    complete_path = f"/api/v1/routines/{routine_id}/complete"

    progress = client.get(f"/api/v1/children/{child_id}/progress", headers=headers).json()["data"]
    assert progress["guardian"]["xp"] == 25
    assert progress["wallet"] == {"flammeches": 10, "crystals": 5}
    assert progress["chest_progress"]["points"] == 0
    assert progress["chest_progress"]["unopened_chests"] == 1

    assert client.post(uncomplete_path, headers=headers).status_code == 200
    rolled_back = client.get(f"/api/v1/children/{child_id}/progress", headers=headers).json()["data"]
    assert rolled_back["guardian"]["xp"] == 20
    assert rolled_back["wallet"] == {"flammeches": 8, "crystals": 4}
    assert rolled_back["chest_progress"]["points"] == 4
    assert rolled_back["chest_progress"]["unopened_chests"] == 0

    assert client.post(complete_path, headers=headers).status_code == 200
    recompleted = client.get(f"/api/v1/children/{child_id}/progress", headers=headers).json()["data"]
    assert recompleted["guardian"]["xp"] == 25
    assert recompleted["wallet"] == {"flammeches": 10, "crystals": 5}
    assert recompleted["chest_progress"]["points"] == 0
    assert recompleted["chest_progress"]["unopened_chests"] == 1

    chests = client.get(f"/api/v1/children/{child_id}/chests", headers=headers).json()["data"]["chests"]
    generated = next(chest for chest in chests if chest["status"] == "unopened")
    opened = client.post(f"/api/v1/children/{child_id}/chests/{generated['id']}/open", headers=headers)
    assert opened.status_code == 200

    refused = client.post(uncomplete_path, headers=headers)
    assert refused.status_code == 409
    assert "coffre genere a deja ete ouvert" in refused.json()["error"]["message"].lower()

    after_refusal = client.get(f"/api/v1/children/{child_id}/progress", headers=headers).json()["data"]
    assert after_refusal["guardian"]["xp"] == 25
    assert after_refusal["wallet"] == {"flammeches": 10, "crystals": 5}
    assert after_refusal["chest_progress"]["points"] == 0
    assert after_refusal["chest_progress"]["opened_chests"] == 1

    routines = client.get(f"/api/v1/children/{child_id}/routines", headers=headers)
    routine = next(item for item in routines.json()["data"] if item["id"] == routine_id)
    assert routine["completed"] is True


def test_egg_evolves_one_state_at_a_time_before_hatching_and_dragon_evolution(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "loot")
    for _ in range(4):
        _create_and_complete_quest(client, parent_token, child_token, child_id)

    opened = _open_unopened_rare_chests(client, child_token, child_id)
    assert len(opened) == 4
    assert opened[0]["granted_egg"]["egg_key"] == "oeuf_braise"

    inventory = client.get(f"/api/v1/children/{child_id}/inventory", headers={"Authorization": f"Bearer {child_token}"})
    items = inventory.json()["data"]["items"]
    assert _item_quantity(items, "fragment_oeuf") == 4

    eggs = client.get(f"/api/v1/children/{child_id}/eggs", headers={"Authorization": f"Bearer {child_token}"})
    egg_id = eggs.json()["data"]["eggs"][0]["id"]

    premature_hatch = client.post(
        f"/api/v1/children/{child_id}/eggs/{egg_id}/hatch",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert premature_hatch.status_code == 400
    assert "hatching" in premature_hatch.json()["error"]["message"]

    for expected_state, expected_next_state in [
        ("warm", "glowing"),
        ("glowing", "cracked"),
        ("cracked", "hatching"),
        ("hatching", None),
    ]:
        evolved = client.post(
            f"/api/v1/children/{child_id}/eggs/{egg_id}/evolve",
            headers={"Authorization": f"Bearer {child_token}"},
        )
        assert evolved.status_code == 200
        payload = evolved.json()["data"]
        assert payload["egg"]["state"] == expected_state
        assert payload["egg"]["next_state"] == expected_next_state
        assert payload["hatched"] is False
        assert payload["dragon"] is None
        if expected_state == "hatching":
            assert payload["egg"]["requirements"] == {
                "pomme_dragon": 3,
                "petit_cristal": 2,
                "pierre_chaude": 1,
            }
            assert {resource["item_key"] for resource in payload["egg"]["required_resources"]} == {
                "pomme_dragon",
                "petit_cristal",
                "pierre_chaude",
            }
        else:
            assert payload["egg"]["requirements"] == {"fragment_oeuf": 1}

    hatched = client.post(
        f"/api/v1/children/{child_id}/eggs/{egg_id}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert hatched.status_code == 200
    assert hatched.json()["data"]["hatched"] is True
    assert hatched.json()["data"]["dragon"]["dragon_key"] == "dragon_braise"
    assert hatched.json()["data"]["dragon"]["stage"] == "baby"
    assert hatched.json()["data"]["egg"]["requirements"] == {}
    assert hatched.json()["data"]["egg"]["required_resources"] == []
    assert hatched.json()["data"]["egg"]["can_evolve"] is False

    already_hatched = client.post(
        f"/api/v1/children/{child_id}/eggs/{egg_id}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert already_hatched.status_code == 400
    assert "deja eclos" in already_hatched.json()["error"]["message"]

    dragons = client.get(f"/api/v1/children/{child_id}/dragons", headers={"Authorization": f"Bearer {child_token}"})
    dragon_id = dragons.json()["data"]["dragons"][0]["id"]
    evolved = client.post(
        f"/api/v1/children/{child_id}/dragons/{dragon_id}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert evolved.status_code == 200
    assert evolved.json()["data"]["dragon"]["stage"] == "young"

    remaining = evolved.json()["data"]["inventory"]["items"]
    assert _item_quantity(remaining, "fragment_oeuf") == 0


def test_egg_contract_exposes_next_action_requirements_and_real_progress(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "egg-contract")
    _create_and_complete_quest(client, parent_token, child_token, child_id)
    opened = _open_unopened_rare_chests(client, child_token, child_id)
    assert len(opened) == 1

    sleeping_egg = client.get(
        f"/api/v1/children/{child_id}/eggs",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["eggs"][0]
    assert sleeping_egg["state"] == sleeping_egg["current_state"] == "sleeping"
    assert sleeping_egg["progress_percent"] == 0
    assert sleeping_egg["next_state"] == "warm"
    assert sleeping_egg["requirements"] == {"fragment_oeuf": 1}
    assert sleeping_egg["required_resources"] == [
        {
            "item_key": "fragment_oeuf",
            "title": "Fragment d'oeuf",
            "owned_quantity": 1,
            "required_quantity": 1,
            "is_satisfied": True,
        }
    ]
    assert sleeping_egg["can_evolve"] is True

    evolved = client.post(
        f"/api/v1/children/{child_id}/eggs/{sleeping_egg['id']}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert evolved.status_code == 200
    assert evolved.json()["data"]["egg"]["state"] == "warm"

    warm_egg = client.get(
        f"/api/v1/children/{child_id}/eggs",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["eggs"][0]
    assert warm_egg["state"] == warm_egg["current_state"] == "warm"
    assert warm_egg["progress_percent"] == 25
    assert warm_egg["next_state"] == "glowing"
    assert warm_egg["requirements"] == {"fragment_oeuf": 1}
    assert warm_egg["required_resources"][0]["owned_quantity"] == 0
    assert warm_egg["required_resources"][0]["required_quantity"] == 1
    assert warm_egg["required_resources"][0]["is_satisfied"] is False
    assert warm_egg["can_evolve"] is False

    bestiary = client.get(
        f"/api/v1/children/{child_id}/bestiary",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]
    braise = next(family for family in bestiary["families"] if family["family_id"] == "braise")
    assert braise["progress_percent"] == 25
    assert braise["egg_progress_percent"] == 25
    assert braise["next_egg_state"] == "glowing"
    assert braise["required_resources"] == warm_egg["required_resources"]
    assert braise["can_evolve"] is False
    assert braise["egg"] == warm_egg


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

    pending_scrolls = client.get(
        f"/api/v1/children/{child_id}/scrolls",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert pending_scrolls.status_code == 200
    assert pending_scrolls.json()["data"]["scrolls"] == []
    assert pending_scrolls.json()["data"]["requests"][0]["status"] == "pending"

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


def test_chest_catalog_uses_crystals_and_rejects_insufficient_balance(client) -> None:
    _, child_token, child_id = _setup_family(client, "chest-catalog")

    catalog = client.get(
        f"/api/v1/children/{child_id}/chests/catalog",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert catalog.status_code == 200
    payload = catalog.json()["data"]
    assert payload["crystals_balance"] == 0
    assert [chest["id"] for chest in payload["chests"]] == ["common", "rare", "epic"]
    assert all(chest["crystal_cost"] > 0 for chest in payload["chests"])
    assert all(chest["possible_rewards"] for chest in payload["chests"])
    possible_rewards = {chest["id"]: set(chest["possible_rewards"]) for chest in payload["chests"]}
    assert possible_rewards == {
        "common": {"pomme_dragon", "petit_cristal", "plume_douce"},
        "rare": {
            "pomme_dragon",
            "petit_cristal",
            "pierre_chaude",
            "rune_ancienne",
            "fragment_oeuf",
            "oeuf_braise",
            "essence_braise",
        },
        "epic": {
            "pomme_dragon",
            "petit_cristal",
            "rune_ancienne",
            "fragment_oeuf",
            "artefact_lunaire",
            "oeuf_lunaire",
            "essence_lunaire",
        },
    }

    opened = client.post(
        f"/api/v1/children/{child_id}/chests/catalog/rare/open",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert opened.status_code == 400
    assert "Cristaux insuffisants" in opened.json()["error"]["message"]


def test_paid_chest_rolls_back_debit_when_opening_fails(client, monkeypatch) -> None:
    parent_token, child_token, child_id = _setup_family(client, "chest-rollback")
    _create_and_complete_mission(client, parent_token, child_token, child_id)

    def fail_opening(*args, **kwargs):
        raise RuntimeError("simulated opening failure")

    monkeypatch.setattr(gamification_service, "open_chest", fail_opening)
    with pytest.raises(RuntimeError, match="simulated opening failure"):
        client.post(
            f"/api/v1/children/{child_id}/chests/catalog/common/open",
            headers={"Authorization": f"Bearer {child_token}"},
        )

    crystals = client.get(
        f"/api/v1/children/{child_id}/crystals",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    inventory = client.get(
        f"/api/v1/children/{child_id}/inventory",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert crystals.json()["data"]["balance"] == 3
    assert inventory.json()["data"]["items"] == []
    assert inventory.json()["data"]["chests"] == []


def test_simultaneous_paid_chest_openings_are_atomic(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "chest-concurrent")
    _create_and_complete_mission(client, parent_token, child_token, child_id)
    headers = {"Authorization": f"Bearer {child_token}"}
    path = f"/api/v1/children/{child_id}/chests/catalog/common/open"

    with ThreadPoolExecutor(max_workers=2) as executor:
        responses = list(executor.map(lambda _: client.post(path, headers=headers), range(2)))

    assert sorted(response.status_code for response in responses) == [200, 400]
    crystals = client.get(f"/api/v1/children/{child_id}/crystals", headers=headers)
    inventory = client.get(f"/api/v1/children/{child_id}/inventory", headers=headers)
    progress = client.get(f"/api/v1/children/{child_id}/progress", headers=headers)
    assert crystals.json()["data"]["balance"] == 0
    assert _item_quantity(inventory.json()["data"]["items"], "pomme_dragon") == 2
    assert _item_quantity(inventory.json()["data"]["items"], "petit_cristal") == 1
    assert _item_quantity(inventory.json()["data"]["items"], "plume_douce") == 1
    assert progress.json()["data"]["chest_progress"]["opened_chests"] == 1

    _create_and_complete_mission(client, parent_token, child_token, child_id)
    _create_and_complete_mission(client, parent_token, child_token, child_id)
    with ThreadPoolExecutor(max_workers=2) as executor:
        responses = list(executor.map(lambda _: client.post(path, headers=headers), range(2)))

    assert [response.status_code for response in responses] == [200, 200]
    crystals = client.get(f"/api/v1/children/{child_id}/crystals", headers=headers)
    inventory = client.get(f"/api/v1/children/{child_id}/inventory", headers=headers)
    progress = client.get(f"/api/v1/children/{child_id}/progress", headers=headers)
    assert crystals.json()["data"]["balance"] == 0
    assert _item_quantity(inventory.json()["data"]["items"], "pomme_dragon") == 6
    assert _item_quantity(inventory.json()["data"]["items"], "petit_cristal") == 3
    assert _item_quantity(inventory.json()["data"]["items"], "plume_douce") == 3
    assert progress.json()["data"]["chest_progress"]["opened_chests"] == 3


def test_paid_chest_updates_inventory_compensates_duplicate_and_builds_bestiary(client) -> None:
    parent_token, child_token, child_id = _setup_family(client, "nest-mvp")
    _create_and_complete_quest(client, parent_token, child_token, child_id)

    owned_chests = client.get(
        f"/api/v1/children/{child_id}/chests",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["chests"]
    earned_rare = next(chest for chest in owned_chests if chest["type"] == "rare")
    first_open = client.post(
        f"/api/v1/children/{child_id}/chests/{earned_rare['id']}/open",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert first_open.status_code == 200
    assert first_open.json()["data"]["granted_egg"]["egg_key"] == "oeuf_braise"
    assert first_open.json()["data"]["duplicate_compensation"] is None

    _create_and_complete_mission(client, parent_token, child_token, child_id)
    flammeches_before = client.get(
        f"/api/v1/children/{child_id}/flammeches",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["balance"]

    paid_open = client.post(
        f"/api/v1/children/{child_id}/chests/catalog/rare/open",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert paid_open.status_code == 200
    paid_payload = paid_open.json()["data"]
    assert paid_payload["crystals_spent"] == 8
    assert paid_payload["crystals_balance"] == 1
    assert paid_payload["granted_egg"] is None
    assert paid_payload["duplicate_compensation"]["reason"] == "duplicate_egg_family"
    assert paid_payload["duplicate_compensation"]["item"]["key"] == "essence_braise"
    assert paid_payload["inventory_after"]["currencies"]["crystals"] == 1

    apple_loot = _loot_item(paid_payload["loot"], "pomme_dragon")
    assert apple_loot["quantity"] == 8
    assert apple_loot["quantity_total"] == 16
    assert apple_loot["is_duplicate_compensation"] is False

    compensation_loot = _loot_item(paid_payload["loot"], "essence_braise")
    assert compensation_loot["quantity"] == 5
    assert compensation_loot["quantity_total"] == 5
    assert compensation_loot["is_duplicate_compensation"] is True

    inventory = client.get(
        f"/api/v1/children/{child_id}/inventory",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert inventory.status_code == 200
    inventory_payload = inventory.json()["data"]
    assert inventory_payload["currencies"] == {"flammeches": flammeches_before, "crystals": 1}
    assert _item_quantity(inventory_payload["items"], "essence_braise") == 5
    assert paid_payload["inventory_after"] == inventory_payload

    scrolls = client.get(
        f"/api/v1/children/{child_id}/scrolls",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert scrolls.status_code == 200
    assert scrolls.json()["data"]["scrolls"] == []
    assert scrolls.json()["data"]["requests"] == []

    for _ in range(2):
        _create_and_complete_quest(client, parent_token, child_token, child_id)
    _open_unopened_rare_chests(client, child_token, child_id)

    eggs = client.get(
        f"/api/v1/children/{child_id}/eggs",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]["eggs"]
    egg_id = eggs[0]["id"]
    for expected_state in ["warm", "glowing", "cracked", "hatching"]:
        evolved_egg = client.post(
            f"/api/v1/children/{child_id}/eggs/{egg_id}/evolve",
            headers={"Authorization": f"Bearer {child_token}"},
        )
        assert evolved_egg.status_code == 200
        assert evolved_egg.json()["data"]["egg"]["state"] == expected_state

    hatched = client.post(
        f"/api/v1/children/{child_id}/eggs/{egg_id}/evolve",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert hatched.status_code == 200
    dragon_id = hatched.json()["data"]["dragon"]["id"]

    activated = client.post(
        f"/api/v1/children/{child_id}/dragons/{dragon_id}/activate",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert activated.status_code == 200
    assert activated.json()["data"]["active_companion"]["stage"] == "baby"
    assert activated.json()["data"]["active_companion"]["active_companion"] is True

    dragons = client.get(
        f"/api/v1/children/{child_id}/dragons",
        headers={"Authorization": f"Bearer {child_token}"},
    ).json()["data"]
    assert dragons["active_companion"]["id"] == dragon_id

    bestiary = client.get(
        f"/api/v1/children/{child_id}/bestiary",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert bestiary.status_code == 200
    families = bestiary.json()["data"]["families"]
    assert len(families) == 3
    braise = next(family for family in families if family["family_id"] == "braise")
    assert braise["egg_owned"] is True
    assert braise["dragon_owned"] is True
    assert braise["active_companion"] is True
    assert braise["current_dragon_stage"] == "baby"


def test_main_gamification_openapi_responses_have_schemas(client) -> None:
    openapi = client.app.openapi()
    endpoints = [
        ("get", "/api/v1/children/{child_id}/crystals"),
        ("get", "/api/v1/children/{child_id}/chests/catalog"),
        ("post", "/api/v1/children/{child_id}/chests/catalog/{catalog_id}/open"),
        ("get", "/api/v1/children/{child_id}/inventory"),
        ("get", "/api/v1/children/{child_id}/bestiary"),
        ("post", "/api/v1/children/{child_id}/eggs/{egg_id}/evolve"),
        ("post", "/api/v1/children/{child_id}/dragons/{dragon_id}/activate"),
        ("get", "/api/v1/children/{child_id}/scrolls"),
    ]

    for method, path in endpoints:
        schema = openapi["paths"][path][method]["responses"]["200"]["content"]["application/json"]["schema"]
        assert schema
