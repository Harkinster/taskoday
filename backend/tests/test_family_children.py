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


def test_parent_can_manage_own_children(client) -> None:
    parent_token = _register_parent(client, "parent.family@example.com", "Famille Bernard")

    current_family = client.get(
        "/families/current",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert current_family.status_code == 200
    family_payload = current_family.json()
    assert family_payload["name"] == "Famille Bernard"
    assert family_payload["children_count"] == 0

    create_child = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "email": "child.one@example.com",
            "password": "childsecret123",
            "display_name": "Emma",
        },
    )
    assert create_child.status_code == 201
    child_1 = create_child.json()

    create_child_2 = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "email": "child.two@example.com",
            "password": "childsecret123",
            "display_name": "Leo",
        },
    )
    assert create_child_2.status_code == 201

    list_children = client.get(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert list_children.status_code == 200
    assert len(list_children.json()) == 2

    get_child = client.get(
        f"/children/{child_1['id']}",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert get_child.status_code == 200
    assert get_child.json()["display_name"] == "Emma"

    create_family_again = client.post(
        "/families",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"name": "Autre famille"},
    )
    assert create_family_again.status_code == 409


def test_parent_cannot_access_children_of_other_family(client) -> None:
    parent_a_token = _register_parent(client, "parent.a@example.com", "Famille A")
    parent_b_token = _register_parent(client, "parent.b@example.com", "Famille B")

    child_response = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_a_token}"},
        json={
            "email": "child.a@example.com",
            "password": "childsecret123",
            "display_name": "Nina",
        },
    )
    assert child_response.status_code == 201
    child_id = child_response.json()["id"]

    forbidden_child = client.get(
        f"/children/{child_id}",
        headers={"Authorization": f"Bearer {parent_b_token}"},
    )
    assert forbidden_child.status_code == 404


def test_child_can_only_see_self(client) -> None:
    parent_token = _register_parent(client, "parent.child.scope@example.com", "Famille Scope")

    child_1 = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "email": "scope.child1@example.com",
            "password": "childsecret123",
            "display_name": "Mila",
        },
    )
    assert child_1.status_code == 201
    child_1_id = child_1.json()["id"]

    child_2 = client.post(
        "/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={
            "email": "scope.child2@example.com",
            "password": "childsecret123",
            "display_name": "Noe",
        },
    )
    assert child_2.status_code == 201
    child_2_id = child_2.json()["id"]

    child_token = _login(client, "scope.child1@example.com", "childsecret123")

    list_as_child = client.get(
        "/children",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert list_as_child.status_code == 200
    payload = list_as_child.json()
    assert len(payload) == 1
    assert payload[0]["id"] == child_1_id

    own_child = client.get(
        f"/children/{child_1_id}",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert own_child.status_code == 200

    other_child = client.get(
        f"/children/{child_2_id}",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert other_child.status_code == 403

    family_for_child = client.get(
        "/families/current",
        headers={"Authorization": f"Bearer {child_token}"},
    )
    assert family_for_child.status_code == 403

