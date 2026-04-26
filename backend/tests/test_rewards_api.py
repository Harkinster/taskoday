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


def test_rewards_purchase_flow_with_points_deduction(client) -> None:
    parent_token = _register_parent(client, "shop.parent@example.com", "Famille Shop")
    child = _create_child(client, parent_token, "shop.child@example.com", "Yuna")
    child_id = child["id"]
    child_token = _login(client, "shop.child@example.com", "childsecret123")
    target_date = date(2026, 4, 27)

    reward_create = client.post(
        f"/children/{child_id}/rewards",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Choisir le dessert",
            "description": "Bonus du soir",
            "cost_points": 3,
            "is_active": True,
        },
    )
    assert reward_create.status_code == 201
    reward_id = reward_create.json()["id"]

    rewards_for_child = client.get(
        f"/children/{child_id}/rewards",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert rewards_for_child.status_code == 200
    assert len(rewards_for_child.json()["rewards"]) == 1

    insufficient_purchase = client.post(
        f"/rewards/{reward_id}/purchase",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert insufficient_purchase.status_code == 400
    assert "Insufficient points" in insufficient_purchase.json()["detail"]

    quest = client.post(
        f"/children/{child_id}/quests",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "title": "Lire 10 minutes",
            "day_part": "SOIR",
            "is_active": True,
        },
    )
    assert quest.status_code == 201
    quest_id = quest.json()["id"]

    complete_quest = client.patch(
        f"/planning/quest/{quest_id}/complete",
        headers={"Authorization": f"Bearer {child_token}"},
        params={"date": target_date.isoformat()},
    )
    assert complete_quest.status_code == 200

    before_purchase_points = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert before_purchase_points.status_code == 200
    assert before_purchase_points.json()["balance"] == 3

    purchase = client.post(
        f"/rewards/{reward_id}/purchase",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert purchase.status_code == 200
    purchase_payload = purchase.json()
    assert purchase_payload["purchase"]["reward_id"] == reward_id
    assert purchase_payload["purchase"]["cost_points"] == 3
    assert purchase_payload["balance"] == 0

    after_purchase_points = client.get(
        f"/children/{child_id}/points",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert after_purchase_points.status_code == 200
    assert after_purchase_points.json()["balance"] == 0

    purchases_history = client.get(
        f"/children/{child_id}/reward-purchases",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert purchases_history.status_code == 200
    history_payload = purchases_history.json()
    assert history_payload["balance"] == 0
    assert len(history_payload["purchases"]) == 1
    assert history_payload["purchases"][0]["reward_title"] == "Choisir le dessert"


def test_rewards_access_control_and_patch_rules(client) -> None:
    parent_a = _register_parent(client, "shop.parent.a@example.com", "Famille A")
    parent_b = _register_parent(client, "shop.parent.b@example.com", "Famille B")
    child_a = _create_child(client, parent_a, "shop.a.child@example.com", "A")
    child_b = _create_child(client, parent_b, "shop.b.child@example.com", "B")
    child_a_token = _login(client, "shop.a.child@example.com", "childsecret123")
    child_b_token = _login(client, "shop.b.child@example.com", "childsecret123")

    reward_create = client.post(
        f"/children/{child_a['id']}/rewards",
        headers={"Authorization": f"Bearer {parent_a}"},
        json={
            "title": "Bonus A",
            "cost_points": 2,
            "is_active": True,
        },
    )
    assert reward_create.status_code == 201
    reward_id = reward_create.json()["id"]

    forbidden_parent_patch = client.patch(
        f"/rewards/{reward_id}",
        headers={"Authorization": f"Bearer {parent_b}"},
        json={"title": "No Access"},
    )
    assert forbidden_parent_patch.status_code == 404

    forbidden_child_patch = client.patch(
        f"/rewards/{reward_id}",
        headers={"Authorization": f"Bearer {child_a_token}"},
        json={"title": "No Access"},
    )
    assert forbidden_child_patch.status_code == 403

    forbidden_child_list = client.get(
        f"/children/{child_b['id']}/rewards",
        headers={"Authorization": f"Bearer {child_a_token}"},
    )
    assert forbidden_child_list.status_code == 403

    forbidden_child_purchase = client.post(
        f"/rewards/{reward_id}/purchase",
        headers={"Authorization": f"Bearer {child_b_token}"},
    )
    assert forbidden_child_purchase.status_code == 403

    updated_reward = client.patch(
        f"/rewards/{reward_id}",
        headers={"Authorization": f"Bearer {parent_a}"},
        json={
            "description": "Mise a jour",
            "cost_points": 4,
            "is_active": False,
        },
    )
    assert updated_reward.status_code == 200
    assert updated_reward.json()["cost_points"] == 4
    assert updated_reward.json()["is_active"] is False

    child_visible_rewards = client.get(
        f"/children/{child_a['id']}/rewards",
        headers={"Authorization": f"Bearer {child_a_token}"},
    )
    assert child_visible_rewards.status_code == 200
    assert child_visible_rewards.json()["rewards"] == []

    parent_all_rewards = client.get(
        f"/children/{child_a['id']}/rewards",
        headers={"Authorization": f"Bearer {parent_a}"},
        params={"include_inactive": "true"},
    )
    assert parent_all_rewards.status_code == 200
    assert len(parent_all_rewards.json()["rewards"]) == 1
