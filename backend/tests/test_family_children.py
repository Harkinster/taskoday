from app.db.session import get_db
from app.models.family import FamilyMember, FamilyMemberRole


API = "/api/v1"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


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


def _me(client, token: str) -> dict:
    response = client.get(f"{API}/auth/me", headers=_headers(token))
    assert response.status_code == 200
    return response.json()


def _add_parent_membership_for_test(client, *, family_id: int, parent_user_id: int) -> None:
    db_generator = client.app.dependency_overrides[get_db]()
    db = next(db_generator)
    try:
        db.add(FamilyMember(family_id=family_id, user_id=parent_user_id, role=FamilyMemberRole.PARENT))
        db.commit()
    finally:
        db_generator.close()


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


def test_family_members_returns_parent_and_children(client) -> None:
    parent_token = _register_parent(client, "parent.members@example.com", "Famille Membres")
    parent_me = _me(client, parent_token)
    family_id = parent_me["family_ids"][0]

    child_token = _register_child(client, "child.members@example.com", "Mila")
    child_id = _attach_child(client, parent_token, child_token)

    response = client.get(f"{API}/families/{family_id}/members", headers=_headers(parent_token))
    assert response.status_code == 200
    members = response.json()["data"]
    assert {member["user_id"] for member in members} == {parent_me["id"], child_id}

    parent_member = next(member for member in members if member["user_id"] == parent_me["id"])
    child_member = next(member for member in members if member["user_id"] == child_id)
    assert parent_member["role"] == "PARENT"
    assert parent_member["display_name"] == "parent.members"
    assert parent_member["is_active"] is True
    assert child_member["role"] == "CHILD"
    assert child_member["display_name"] == "Mila"
    assert child_member["email"] == "child.members@example.com"


def test_family_members_returns_second_parent_supported_by_model(client) -> None:
    parent_a_token = _register_parent(client, "parent.members.a@example.com", "Famille Membres A")
    parent_a_me = _me(client, parent_a_token)
    family_id = parent_a_me["family_ids"][0]

    parent_b_token = _register_parent(client, "parent.members.b@example.com", "Famille Membres B")
    parent_b_me = _me(client, parent_b_token)
    _add_parent_membership_for_test(client, family_id=family_id, parent_user_id=parent_b_me["id"])

    response = client.get(f"{API}/families/{family_id}/members", headers=_headers(parent_a_token))
    assert response.status_code == 200
    members = response.json()["data"]
    parents = [member for member in members if member["role"] == "PARENT"]
    assert {member["user_id"] for member in parents} == {parent_a_me["id"], parent_b_me["id"]}

    response_as_second_parent = client.get(f"{API}/families/{family_id}/members", headers=_headers(parent_b_token))
    assert response_as_second_parent.status_code == 200
    assert {member["user_id"] for member in response_as_second_parent.json()["data"]} == {
        parent_a_me["id"],
        parent_b_me["id"],
    }


def test_family_members_are_isolated_between_families(client) -> None:
    parent_a_token = _register_parent(client, "parent.members.iso.a@example.com", "Famille Membres Iso A")
    parent_a_me = _me(client, parent_a_token)
    parent_b_token = _register_parent(client, "parent.members.iso.b@example.com", "Famille Membres Iso B")
    parent_b_me = _me(client, parent_b_token)

    child_token = _register_child(client, "child.members.iso@example.com", "Nina")
    child_id = _attach_child(client, parent_a_token, child_token)

    family_a_members = client.get(
        f"{API}/families/{parent_a_me['family_ids'][0]}/members",
        headers=_headers(parent_a_token),
    )
    assert family_a_members.status_code == 200
    assert {member["user_id"] for member in family_a_members.json()["data"]} == {parent_a_me["id"], child_id}

    family_b_members = client.get(
        f"{API}/families/{parent_b_me['family_ids'][0]}/members",
        headers=_headers(parent_b_token),
    )
    assert family_b_members.status_code == 200
    assert {member["user_id"] for member in family_b_members.json()["data"]} == {parent_b_me["id"]}


def test_family_members_refuses_external_user(client) -> None:
    parent_a_token = _register_parent(client, "parent.members.external.a@example.com", "Famille Membres External A")
    parent_a_me = _me(client, parent_a_token)
    parent_b_token = _register_parent(client, "parent.members.external.b@example.com", "Famille Membres External B")

    response = client.get(f"{API}/families/{parent_a_me['family_ids'][0]}/members", headers=_headers(parent_b_token))
    assert response.status_code == 404


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
