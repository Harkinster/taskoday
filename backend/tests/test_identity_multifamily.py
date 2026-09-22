from contextlib import contextmanager
from datetime import date

from sqlalchemy import select

from app.core.security import create_access_token
from app.db.session import get_db
from app.models.child import ChildProfile
from app.models.family import FamilyMember
from app.models.user import User, UserRole


API = "/api/v1"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@contextmanager
def _db(client):
    generator = client.app.dependency_overrides[get_db]()
    session = next(generator)
    try:
        yield session
    finally:
        generator.close()


def _register_parent(client, slug: str) -> tuple[str, int]:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "display_name": f"Parent {slug}",
            "birth_date": "1988-01-20",
            "email": f"{slug}@example.com",
            "password": "supersecret123",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]
    me = client.get(f"{API}/auth/me", headers=_headers(token)).json()
    return token, me["id"]


def _create_family(client, token: str, name: str) -> int:
    response = client.post(f"{API}/families", headers=_headers(token), json={"name": name})
    assert response.status_code == 200
    return response.json()["data"]["id"]


def _join(client, owner_token: str, family_id: int, joining_token: str) -> None:
    invite = client.post(f"{API}/families/{family_id}/parent-invites", headers=_headers(owner_token))
    assert invite.status_code == 201
    accepted = client.post(
        f"{API}/family-invites/{invite.json()['data']['code']}/accept",
        headers=_headers(joining_token),
    )
    assert accepted.status_code == 200


def test_account_first_profile_and_no_stored_age(client) -> None:
    token, user_id = _register_parent(client, "account-first")
    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    assert me.json()["family_ids"] == []
    assert me.json()["display_name"] == "Parent account-first"
    assert me.json()["birth_date"] == "1988-01-20"

    updated = client.patch(
        f"{API}/profile/me",
        headers=_headers(token),
        json={"display_name": "  Mimi  ", "birth_date": "1990-05-10"},
    )
    assert updated.status_code == 200
    assert updated.json()["data"]["display_name"] == "Mimi"
    assert updated.json()["data"]["birth_date"] == "1990-05-10"

    future = client.patch(
        f"{API}/profile/me",
        headers=_headers(token),
        json={"birth_date": "2999-01-01"},
    )
    assert future.status_code == 422

    with _db(client) as db:
        user = db.get(User, user_id)
        assert user.display_name == "Mimi"
        assert user.birth_date == date(1990, 5, 10)
        assert "age" not in User.__table__.columns


def test_parent_can_create_and_join_multiple_families_without_losing_memberships(client) -> None:
    token_a, user_a = _register_parent(client, "multi-a")
    token_b, _ = _register_parent(client, "multi-b")

    family_a = _create_family(client, token_a, "Famille A")
    family_b = _create_family(client, token_a, "Famille B")
    family_c = _create_family(client, token_b, "Famille C")
    family_d = _create_family(client, token_b, "Famille D")

    _join(client, token_b, family_c, token_a)
    _join(client, token_b, family_d, token_a)

    me = client.get(f"{API}/auth/me", headers=_headers(token_a))
    assert me.status_code == 200
    assert me.json()["family_ids"] == [family_a, family_b, family_c, family_d]

    with _db(client) as db:
        memberships = db.scalars(
            select(FamilyMember).where(FamilyMember.user_id == user_a).order_by(FamilyMember.family_id)
        ).all()
        assert [item.family_id for item in memberships] == [family_a, family_b, family_c, family_d]

    duplicate_invite = client.post(f"{API}/families/{family_a}/parent-invites", headers=_headers(token_a))
    duplicate = client.post(
        f"{API}/family-invites/{duplicate_invite.json()['data']['code']}/accept",
        headers=_headers(token_a),
    )
    assert duplicate.status_code == 409


def test_existing_child_identity_fallback_and_new_child_user_identity(client) -> None:
    token, _ = _register_parent(client, "child-identity")
    family_id = _create_family(client, token, "Famille enfants")
    created = client.post(
        f"{API}/children",
        headers=_headers(token),
        json={"display_name": "Océane", "birth_date": "2014-07-03"},
    )
    assert created.status_code == 201
    child_id = created.json()["data"]["id"]

    with _db(client) as db:
        child = db.get(User, child_id)
        profile = db.scalar(select(ChildProfile).where(ChildProfile.user_id == child_id))
        assert child.display_name == profile.display_name == "Océane"
        assert child.birth_date == profile.birth_date == date(2014, 7, 3)
        assert db.scalar(
            select(FamilyMember.id).where(
                FamilyMember.family_id == family_id,
                FamilyMember.user_id == child_id,
            )
        ) is not None

        child.display_name = None
        db.commit()
        child_token = create_access_token(subject=str(child_id), extra_claims={"role": UserRole.CHILD.name})

    me = client.get(f"{API}/auth/me", headers=_headers(child_token))
    assert me.status_code == 200
    assert me.json()["display_name"] == "Océane"
