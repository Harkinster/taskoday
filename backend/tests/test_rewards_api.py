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


def _complete_routine_mission_and_quest(client, parent_token: str, child_token: str, child_id: int) -> None:
    routine = client.post(
        f"/api/v1/children/{child_id}/routines",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Se brosser les dents", "repeat_type": "daily", "can_child_delete": False},
    )
    assert routine.status_code == 200
    routine_id = routine.json()["data"]["id"]
    complete_routine = client.post(
        f"/api/v1/routines/{routine_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert complete_routine.status_code == 200

    mission = client.post(
        f"/api/v1/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Ranger le bureau"},
    )
    assert mission.status_code == 200
    mission_id = mission.json()["data"]["id"]
    complete_mission = client.post(
        f"/api/v1/missions/{mission_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert complete_mission.status_code == 200

    quest = client.post(
        f"/api/v1/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Lire 10 minutes", "xp_reward": 25},
    )
    assert quest.status_code == 200
    quest_id = quest.json()["data"]["id"]
    complete_quest = client.post(
        f"/api/v1/quests/{quest_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert complete_quest.status_code == 200


def test_external_reward_request_approval_creates_coupon_and_spends_scales(client) -> None:
    parent_token = _register_parent(client, "shop.parent@example.com", "Famille Shop")
    child_token = _register_child(client, "shop.child@example.com", "Yuna")
    child_id = _attach_child(client, parent_token, child_token)

    _complete_routine_mission_and_quest(client, parent_token, child_token, child_id)

    scales = client.get(f"/api/v1/children/{child_id}/scales", headers={"Authorization": f"Bearer {child_token}"})
    assert scales.status_code == 200
    assert scales.json()["data"]["balance"] == 20

    reward_create = client.post(
        f"/api/v1/children/{child_id}/rewards",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Choisir le dessert", "description": "Bonus du soir", "cost_scales": 5, "is_active": True},
    )
    assert reward_create.status_code == 201
    reward_id = reward_create.json()["data"]["id"]

    reward_request = client.post(
        f"/api/v1/rewards/{reward_id}/requests",
        headers={"Authorization": f"Bearer {child_token}"},
        json={"note": "Je voudrais ce soir."},
    )
    assert reward_request.status_code == 201
    request_payload = reward_request.json()["data"]
    assert request_payload["status"] == "pending"
    assert request_payload["coupon"] is None

    before_approval = client.get(f"/api/v1/children/{child_id}/scales", headers={"Authorization": f"Bearer {child_token}"})
    assert before_approval.json()["data"]["balance"] == 20

    approved = client.patch(
        f"/api/v1/reward-requests/{request_payload['id']}",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"status": "approved"},
    )
    assert approved.status_code == 200
    approved_payload = approved.json()["data"]
    assert approved_payload["status"] == "approved"
    assert approved_payload["coupon"]["code"].startswith("TASKO-")
    assert approved_payload["coupon"]["status"] == "available"

    after_approval = client.get(f"/api/v1/children/{child_id}/scales", headers={"Authorization": f"Bearer {child_token}"})
    assert after_approval.json()["data"]["balance"] == 15

    used = client.post(
        f"/api/v1/reward-coupons/{approved_payload['coupon']['id']}/use",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert used.status_code == 200
    assert used.json()["data"]["status"] == "used"
    assert used.json()["data"]["coupon"]["status"] == "used"


def test_reward_refusal_and_access_control_do_not_spend_scales(client) -> None:
    parent_a = _register_parent(client, "shop.parent.a@example.com", "Famille A")
    parent_b = _register_parent(client, "shop.parent.b@example.com", "Famille B")
    child_token = _register_child(client, "shop.child.a@example.com", "A")
    child_id = _attach_child(client, parent_a, child_token)
    _complete_routine_mission_and_quest(client, parent_a, child_token, child_id)

    reward = client.post(
        f"/api/v1/children/{child_id}/rewards",
        headers={"Authorization": f"Bearer {parent_a}"},
        json={"title": "Dessiner avant le coucher", "cost_scales": 3},
    )
    assert reward.status_code == 201

    request = client.post(
        f"/api/v1/rewards/{reward.json()['data']['id']}/requests",
        headers={"Authorization": f"Bearer {child_token}"},
        json={},
    )
    assert request.status_code == 201
    request_id = request.json()["data"]["id"]

    forbidden = client.patch(
        f"/api/v1/reward-requests/{request_id}",
        headers={"Authorization": f"Bearer {parent_b}"},
        json={"status": "approved"},
    )
    assert forbidden.status_code == 403

    refused = client.patch(
        f"/api/v1/reward-requests/{request_id}",
        headers={"Authorization": f"Bearer {parent_a}"},
        json={"status": "refused"},
    )
    assert refused.status_code == 200
    assert refused.json()["data"]["status"] == "refused"
    assert refused.json()["data"]["coupon"] is None

    scales = client.get(f"/api/v1/children/{child_id}/scales", headers={"Authorization": f"Bearer {child_token}"})
    assert scales.json()["data"]["balance"] == 20


def test_child_cannot_create_mission_or_request_without_scales(client) -> None:
    parent_token = _register_parent(client, "shop.parent.rules@example.com", "Famille Regles")
    child_token = _register_child(client, "shop.rules.child@example.com", "Rules")
    child_id = _attach_child(client, parent_token, child_token)

    child_mission = client.post(
        f"/api/v1/children/{child_id}/missions",
        headers={"Authorization": f"Bearer {child_token}"},
        json={"title": "Mission enfant interdite"},
    )
    assert child_mission.status_code == 403

    reward = client.post(
        f"/api/v1/children/{child_id}/rewards",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"title": "Cinema", "cost_scales": 1},
    )
    assert reward.status_code == 201

    request = client.post(
        f"/api/v1/rewards/{reward.json()['data']['id']}/requests",
        headers={"Authorization": f"Bearer {child_token}"},
        json={},
    )
    assert request.status_code == 400
    assert "Flammeches insuffisantes" in request.json()["error"]["message"]
