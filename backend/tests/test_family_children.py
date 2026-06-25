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


def test_parent_can_manage_own_children(client) -> None:
    parent_token = _register_parent(client, "parent.family@example.com", "Famille Bernard")

    families = client.get(
        f"{API}/families/me",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert families.status_code == 200
    family_payload = families.json()["data"]
    assert len(family_payload) == 1
    assert family_payload[0]["name"] == "Famille Bernard"

    child_1_token = _register_child(client, "child.one@example.com", "Emma")
    child_2_token = _register_child(client, "child.two@example.com", "Leo")
    child_1_id = _attach_child(client, parent_token, child_1_token)
    _attach_child(client, parent_token, child_2_token)

    list_children = client.get(
        f"{API}/children",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert list_children.status_code == 200
    assert len(list_children.json()["data"]) == 2

    get_child = client.get(
        f"{API}/children/{child_1_id}",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert get_child.status_code == 200
    assert get_child.json()["data"]["display_name"] == "Emma"

    family_children = client.get(
        f"{API}/families/{family_payload[0]['id']}/children",
        headers={"Authorization": f"Bearer {parent_token}"},
    )
    assert family_children.status_code == 200
    assert len(family_children.json()["data"]) == 2


def test_parent_can_create_child_directly(client) -> None:
    parent_token = _register_parent(client, "parent.direct@example.com", "Famille Direct")

    created = client.post(
        f"{API}/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"display_name": "Zoe Test"},
    )
    assert created.status_code == 201
    created_payload = created.json()["data"]
    assert created_payload["display_name"] == "Zoe Test"
    assert created_payload["email"].endswith("@children.taskoday.app")
    assert created_payload["xp"] == 0
    assert created_payload["level"] == 1

    children = client.get(f"{API}/children", headers={"Authorization": f"Bearer {parent_token}"})
    assert children.status_code == 200
    assert [child["id"] for child in children.json()["data"]] == [created_payload["id"]]
    assert children.json()["data"][0] == created_payload

    child_token = _register_child(client, "direct.child@example.com", "Child")
    forbidden = client.post(
        f"{API}/children",
        headers={"Authorization": f"Bearer {child_token}"},
        json={"display_name": "Nope"},
    )
    assert forbidden.status_code == 403

    invalid = client.post(
        f"{API}/children",
        headers={"Authorization": f"Bearer {parent_token}"},
        json={"display_name": "   "},
    )
    assert invalid.status_code == 422


def test_parent_cannot_access_children_of_other_family(client) -> None:
    parent_a_token = _register_parent(client, "parent.a@example.com", "Famille A")
    parent_b_token = _register_parent(client, "parent.b@example.com", "Famille B")

    child_token = _register_child(client, "child.a@example.com", "Nina")
    child_id = _attach_child(client, parent_a_token, child_token)

    forbidden_child = client.get(
        f"{API}/children/{child_id}",
        headers={"Authorization": f"Bearer {parent_b_token}"},
    )
    assert forbidden_child.status_code == 403


def test_child_can_only_see_self(client) -> None:
    parent_token = _register_parent(client, "parent.child.scope@example.com", "Famille Scope")

    child_1_token = _register_child(client, "scope.child1@example.com", "Mila")
    child_2_token = _register_child(client, "scope.child2@example.com", "Noe")
    child_1_id = _attach_child(client, parent_token, child_1_token)
    child_2_id = _attach_child(client, parent_token, child_2_token)

    list_as_child = client.get(
        f"{API}/children",
        headers={"Authorization": f"Bearer {child_1_token}"},
    )
    assert list_as_child.status_code == 200
    payload = list_as_child.json()["data"]
    assert len(payload) == 1
    assert payload[0]["id"] == child_1_id

    own_child = client.get(
        f"{API}/children/{child_1_id}",
        headers={"Authorization": f"Bearer {child_1_token}"},
    )
    assert own_child.status_code == 200

    other_child = client.get(
        f"{API}/children/{child_2_id}",
        headers={"Authorization": f"Bearer {child_1_token}"},
    )
    assert other_child.status_code == 403

    family_for_child = client.get(
        f"{API}/families/me",
        headers={"Authorization": f"Bearer {child_1_token}"},
    )
    assert family_for_child.status_code == 200
    assert len(family_for_child.json()["data"]) == 1
