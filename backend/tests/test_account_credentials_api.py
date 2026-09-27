from contextlib import contextmanager
from typing import Generator

from sqlalchemy import select

from app.db.session import get_db
from app.models.child import ChildProfile
from app.models.family import FamilyMember
from app.models.user import User


API = "/api/v1"


@contextmanager
def _db_session(client) -> Generator:
    override_get_db = client.app.dependency_overrides[get_db]
    generator = override_get_db()
    db = next(generator)
    try:
        yield db
    finally:
        generator.close()


def _register_parent(client, email: str, password: str = "parentsecret123") -> dict:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": email,
            "password": password,
            "family_name": f"Famille {email}",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    return response.json()


def _register_child(client, email: str, password: str = "childsecret123") -> dict:
    response = client.post(
        f"{API}/auth/register-child",
        json={
            "email": email,
            "password": password,
            "display_name": "QA Child",
            "birth_date": "2015-05-10",
        },
    )
    assert response.status_code == 201
    return response.json()


def _auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_email_change_updates_login_without_changing_identity_or_memberships(client) -> None:
    parent = _register_parent(client, "email-change-parent@example.com")
    old_email = "email-change-parent@example.com"
    new_email = "Email-Change-Parent-Updated@Example.COM"

    before = client.get(f"{API}/auth/me", headers=_auth(parent["access_token"])).json()
    family_before = client.get(f"{API}/families/me", headers=_auth(parent["access_token"])).json()

    changed = client.patch(
        f"{API}/profile/me",
        headers=_auth(parent["access_token"]),
        json={"email": new_email},
    )
    assert changed.status_code == 200
    assert changed.json()["data"]["email"] == new_email.lower()

    old_login = client.post(
        f"{API}/auth/login",
        json={"email": old_email, "password": "parentsecret123"},
    )
    assert old_login.status_code == 401

    new_login = client.post(
        f"{API}/auth/login",
        json={"email": new_email, "password": "parentsecret123"},
    )
    assert new_login.status_code == 200

    after = client.get(f"{API}/auth/me", headers=_auth(new_login.json()["access_token"])).json()
    family_after = client.get(f"{API}/families/me", headers=_auth(new_login.json()["access_token"])).json()
    assert after["id"] == before["id"]
    assert after["role"] == before["role"]
    assert after["family_ids"] == before["family_ids"]
    assert family_after == family_before


def test_email_change_rejects_duplicate_invalid_and_unauthenticated_requests(client) -> None:
    first = _register_parent(client, "email-duplicate-first@example.com")
    _register_parent(client, "email-duplicate-second@example.com")

    duplicate = client.patch(
        f"{API}/profile/me",
        headers=_auth(first["access_token"]),
        json={"email": "email-duplicate-second@example.com"},
    )
    assert duplicate.status_code == 409

    invalid = client.patch(
        f"{API}/profile/me",
        headers=_auth(first["access_token"]),
        json={"email": "not-an-email"},
    )
    assert invalid.status_code == 422

    unauthenticated = client.patch(
        f"{API}/profile/me",
        json={"email": "anonymous@example.com"},
    )
    assert unauthenticated.status_code == 401


def test_child_email_change_preserves_role_and_child_profile(client) -> None:
    child = _register_child(client, "child-email-before@example.com")

    with _db_session(client) as db:
        user = db.scalar(select(User).where(User.email == "child-email-before@example.com"))
        assert user is not None
        profile = db.scalar(select(ChildProfile).where(ChildProfile.user_id == user.id))
        assert profile is not None
        original_user_id = user.id
        original_role = user.role
        original_display_name = profile.display_name
        original_birth_date = profile.birth_date

    changed = client.patch(
        f"{API}/profile/me",
        headers=_auth(child["access_token"]),
        json={"email": "child-email-after@example.com"},
    )
    assert changed.status_code == 200

    with _db_session(client) as db:
        user = db.get(User, original_user_id)
        profile = db.scalar(select(ChildProfile).where(ChildProfile.user_id == original_user_id))
        assert user is not None
        assert profile is not None
        assert user.email == "child-email-after@example.com"
        assert user.role == original_role
        assert profile.display_name == original_display_name
        assert profile.birth_date == original_birth_date


def test_change_password_requires_current_password_and_changes_login(client) -> None:
    old_password = "parentsecret123"
    new_password = "newparentsecret123"
    parent = _register_parent(client, "password-change-parent@example.com", old_password)

    changed = client.post(
        f"{API}/auth/change-password",
        headers=_auth(parent["access_token"]),
        json={"current_password": old_password, "new_password": new_password},
    )
    assert changed.status_code == 204

    old_login = client.post(
        f"{API}/auth/login",
        json={"email": "password-change-parent@example.com", "password": old_password},
    )
    assert old_login.status_code == 401

    new_login = client.post(
        f"{API}/auth/login",
        json={"email": "password-change-parent@example.com", "password": new_password},
    )
    assert new_login.status_code == 200


def test_change_password_rejects_wrong_current_invalid_new_and_unauthenticated(client) -> None:
    parent = _register_parent(client, "password-validation-parent@example.com")

    wrong_current = client.post(
        f"{API}/auth/change-password",
        headers=_auth(parent["access_token"]),
        json={"current_password": "wrongsecret123", "new_password": "newparentsecret123"},
    )
    assert wrong_current.status_code == 401

    invalid_new = client.post(
        f"{API}/auth/change-password",
        headers=_auth(parent["access_token"]),
        json={"current_password": "parentsecret123", "new_password": "short"},
    )
    assert invalid_new.status_code == 422

    unauthenticated = client.post(
        f"{API}/auth/change-password",
        json={"current_password": "parentsecret123", "new_password": "newparentsecret123"},
    )
    assert unauthenticated.status_code == 401


def test_password_change_keeps_refresh_sessions_usable(client) -> None:
    parent = _register_parent(client, "password-refresh-parent@example.com")

    changed = client.post(
        f"{API}/auth/change-password",
        headers=_auth(parent["access_token"]),
        json={"current_password": "parentsecret123", "new_password": "newparentsecret123"},
    )
    assert changed.status_code == 204

    refreshed = client.post(
        f"{API}/auth/refresh",
        json={"refresh_token": parent["refresh_token"]},
    )
    assert refreshed.status_code == 200
