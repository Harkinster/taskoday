from contextlib import contextmanager
from typing import Generator

from app.core.security import create_access_token
from app.db.session import get_db
from app.models.user import User, UserRole
from app.services.user_identity_service import display_name_for_user


API = "/api/v1"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@contextmanager
def _db_session(client) -> Generator:
    generator = client.app.dependency_overrides[get_db]()
    db = next(generator)
    try:
        yield db
    finally:
        generator.close()


def test_new_parent_identity_is_saved_and_returned_by_me(client) -> None:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "display_name": " Matthieu ",
            "email": "matthieu.identity@example.com",
            "password": "supersecret123",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201

    me = client.get(f"{API}/auth/me", headers=_headers(response.json()["access_token"]))
    assert me.status_code == 200
    assert me.json()["display_name"] == "Matthieu"
    assert me.json()["birth_date"] == "1988-01-20"
    assert me.json()["family_ids"] == []


def test_new_parent_rejects_blank_display_name(client) -> None:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "display_name": "   ",
            "email": "blank.identity@example.com",
            "password": "supersecret123",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 422


def test_legacy_parent_without_display_name_uses_email_fallback(client) -> None:
    with _db_session(client) as db:
        user = User(
            email="laurens.matthieu@example.com",
            password_hash="unused",
            role=UserRole.PARENT,
            display_name=None,
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        token = create_access_token(subject=str(user.id), extra_claims={"role": "PARENT"})

    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    assert me.json()["display_name"] == "laurens.matthieu"


def test_child_display_name_rule_is_unchanged(client) -> None:
    child = client.post(
        f"{API}/auth/register-child",
        json={
            "email": "child.identity@example.com",
            "password": "childsecret123",
            "display_name": "Naomy",
        },
    )
    assert child.status_code == 201

    with _db_session(client) as db:
        user = db.query(User).filter(User.email == "child.identity@example.com").one()
        assert user.display_name == "Naomy"
        assert display_name_for_user(user, user.child_profile) == "Naomy"


def test_beta02_register_parent_family_name_keeps_null_display_name(client) -> None:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "legacy.family@example.com",
            "password": "supersecret123",
            "family_name": "Famille legacy",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]

    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    assert len(me.json()["family_ids"]) == 1
    assert me.json()["display_name"] == "legacy.family"

    with _db_session(client) as db:
        user = db.query(User).filter(User.email == "legacy.family@example.com").one()
        assert user.display_name is None


def test_beta02_register_parent_invite_code_keeps_null_display_name(client) -> None:
    owner = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "legacy.owner@example.com",
            "password": "supersecret123",
            "family_name": "Famille invite legacy",
            "birth_date": "1988-01-20",
        },
    )
    assert owner.status_code == 201
    owner_token = owner.json()["access_token"]
    owner_me = client.get(f"{API}/auth/me", headers=_headers(owner_token)).json()
    family_id = owner_me["family_ids"][0]

    invite = client.post(f"{API}/families/{family_id}/parent-invites", headers=_headers(owner_token))
    assert invite.status_code == 201
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "legacy.invited@example.com",
            "password": "supersecret123",
            "invite_code": invite.json()["data"]["code"],
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    me = client.get(f"{API}/auth/me", headers=_headers(response.json()["access_token"]))
    assert me.status_code == 200
    assert me.json()["family_ids"] == [family_id]
    assert me.json()["display_name"] == "legacy.invited"

    with _db_session(client) as db:
        user = db.query(User).filter(User.email == "legacy.invited@example.com").one()
        assert user.display_name is None


def test_register_parent_requires_identity_or_legacy_family_flow(client) -> None:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "missing.registration.mode@example.com",
            "password": "supersecret123",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 422


def test_register_parent_rejects_ambiguous_identity_and_legacy_flow(client) -> None:
    with_family = client.post(
        f"{API}/auth/register-parent",
        json={
            "display_name": "Matthieu",
            "email": "ambiguous.family@example.com",
            "password": "supersecret123",
            "family_name": "Famille ambiguë",
            "birth_date": "1988-01-20",
        },
    )
    assert with_family.status_code == 422

    with_invite = client.post(
        f"{API}/auth/register-parent",
        json={
            "display_name": "Matthieu",
            "email": "ambiguous.invite@example.com",
            "password": "supersecret123",
            "invite_code": "not-a-valid-code",
            "birth_date": "1988-01-20",
        },
    )
    assert with_invite.status_code == 422
