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


def test_planning_sections_and_items_for_date(client) -> None:
    parent_token = _register_parent(client, "planning.parent@example.com", "Famille Planning")
    child = _create_child(client, parent_token, "planning.child@example.com", "Lina")
    child_id = child["id"]
    target_date = date(2026, 4, 27)  # Monday

    routine_daily = client.post(
        f"/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Brosser les dents",
            "description": "Matin",
            "day_part": "MATIN",
            "frequency": "DAILY",
            "points_reward": 1,
            "is_active": True,
        },
    )
    assert routine_daily.status_code == 201

    routine_selected = client.post(
        f"/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Devoirs",
            "description": "Lundi/Jeudi",
            "day_part": "SOIREE",
            "frequency": "SELECTED_DAYS",
            "days_of_week": "1,4",
            "points_reward": 1,
            "is_active": True,
        },
    )
    assert routine_selected.status_code == 201

    mission = client.post(
        f"/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Piscine",
            "day_part": "MIDI",
            "scheduled_date": target_date.isoformat(),
            "points_reward": 2,
            "is_active": True,
        },
    )
    assert mission.status_code == 201

    quest_active = client.post(
        f"/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Lire 10 min",
            "day_part": "SOIR",
            "points_reward": 3,
            "is_active": True,
        },
    )
    assert quest_active.status_code == 201

    quest_scheduled = client.post(
        f"/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Bonne action du jour",
            "day_part": "APRES_MIDI",
            "scheduled_date": target_date.isoformat(),
            "points_reward": 3,
            "is_active": False,
        },
    )
    assert quest_scheduled.status_code == 201

    planning_response = client.get(
        f"/children/{child_id}/planning",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    assert planning_response.status_code == 200
    payload = planning_response.json()
    assert payload["child_id"] == child_id
    assert payload["planning_date"] == target_date.isoformat()
    assert [section["day_part"] for section in payload["sections"]] == [
        "MATIN",
        "MATINEE",
        "MIDI",
        "APRES_MIDI",
        "SOIR",
        "SOIREE",
    ]

    all_items = [item for section in payload["sections"] for item in section["items"]]
    names = {item["title"] for item in all_items}
    assert "Brosser les dents" in names
    assert "Devoirs" in names
    assert "Piscine" in names
    assert "Lire 10 min" in names
    assert "Bonne action du jour" in names


def test_complete_uncomplete_is_idempotent_for_routine(client) -> None:
    parent_token = _register_parent(client, "planning.complete@example.com", "Famille Complete")
    child = _create_child(client, parent_token, "planning.complete.child@example.com", "Noe")
    child_id = child["id"]
    target_date = date(2026, 4, 27)

    routine_response = client.post(
        f"/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Rangement",
            "day_part": "APRES_MIDI",
            "frequency": "DAILY",
            "points_reward": 1,
            "is_active": True,
        },
    )
    assert routine_response.status_code == 201
    routine_id = routine_response.json()["id"]

    complete_once = client.patch(
        f"/planning/routine/{routine_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    assert complete_once.status_code == 200
    assert complete_once.json()["completed"] is True

    complete_twice = client.patch(
        f"/planning/routine/{routine_id}/complete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    assert complete_twice.status_code == 200
    assert complete_twice.json()["completed"] is True

    planning_after_complete = client.get(
        f"/children/{child_id}/planning",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    assert planning_after_complete.status_code == 200
    items = [item for section in planning_after_complete.json()["sections"] for item in section["items"]]
    routine_item = next(item for item in items if item["item_id"] == routine_id and item["item_type"] == "routine")
    assert routine_item["is_completed"] is True

    uncomplete_once = client.patch(
        f"/planning/routine/{routine_id}/uncomplete",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    assert uncomplete_once.status_code == 200
    assert uncomplete_once.json()["completed"] is False

    planning_after_uncomplete = client.get(
        f"/children/{child_id}/planning",
        headers={"Authorization": f"Bearer {parent_token}"},
        params={"date": target_date.isoformat()},
    )
    items_after = [item for section in planning_after_uncomplete.json()["sections"] for item in section["items"]]
    routine_item_after = next(
        item for item in items_after if item["item_id"] == routine_id and item["item_type"] == "routine"
    )
    assert routine_item_after["is_completed"] is False


def test_planning_access_control_parent_and_child(client) -> None:
    parent_a_token = _register_parent(client, "planning.a@example.com", "Famille A")
    parent_b_token = _register_parent(client, "planning.b@example.com", "Famille B")

    child_a1 = _create_child(client, parent_a_token, "planning.a1@example.com", "Ana")
    child_a2 = _create_child(client, parent_a_token, "planning.a2@example.com", "Eli")
    child_b1 = _create_child(client, parent_b_token, "planning.b1@example.com", "Tom")

    # Parent A creates a routine for child A1.
    routine_a1 = client.post(
        f"/children/{child_a1['id']}/routines",
        headers={"Authorization": f"Bearer {parent_a_token}"},
        json={
            "title": "Routine A1",
            "day_part": "MATIN",
            "frequency": "DAILY",
            "points_reward": 1,
            "is_active": True,
        },
    )
    assert routine_a1.status_code == 201
    routine_a1_id = routine_a1.json()["id"]

    # Parent B cannot create/read planning of A family child.
    forbidden_create = client.post(
        f"/children/{child_a1['id']}/routines",
        headers={"Authorization": f"Bearer {parent_b_token}"},
        json={
            "title": "No Access",
            "day_part": "MATIN",
            "frequency": "DAILY",
            "points_reward": 1,
            "is_active": True,
        },
    )
    assert forbidden_create.status_code == 404

    forbidden_read = client.get(
        f"/children/{child_a1['id']}/planning",
        headers={"Authorization": f"Bearer {parent_b_token}"},
        params={"date": date(2026, 4, 27).isoformat()},
    )
    assert forbidden_read.status_code == 404

    # Child A1 can access own planning and complete own routine.
    child_a1_token = _login(client, "planning.a1@example.com", "childsecret123")
    own_planning = client.get(
        f"/children/{child_a1['id']}/planning",
        headers={"Authorization": f"Bearer {child_a1_token}"},
        params={"date": date(2026, 4, 27).isoformat()},
    )
    assert own_planning.status_code == 200

    own_complete = client.patch(
        f"/planning/routine/{routine_a1_id}/complete",
        headers={"Authorization": f"Bearer {child_a1_token}"},
        params={"date": date(2026, 4, 27).isoformat()},
    )
    assert own_complete.status_code == 200

    # Child A1 cannot access sibling A2 planning.
    sibling_planning = client.get(
        f"/children/{child_a2['id']}/planning",
        headers={"Authorization": f"Bearer {child_a1_token}"},
        params={"date": date(2026, 4, 27).isoformat()},
    )
    assert sibling_planning.status_code == 403

    # Child A1 cannot access other family child B1 planning.
    other_family_planning = client.get(
        f"/children/{child_b1['id']}/planning",
        headers={"Authorization": f"Bearer {child_a1_token}"},
        params={"date": date(2026, 4, 27).isoformat()},
    )
    assert other_family_planning.status_code == 403

