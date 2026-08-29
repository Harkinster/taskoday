import hashlib
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from typing import Generator

import pytest
from fastapi import HTTPException
from sqlalchemy import func, select

from app.db.session import get_db
from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.services import refresh_token_service


API = "/api/v1"
PASSWORD = "supersecret123"


@contextmanager
def _db_session(client) -> Generator:
    override_get_db = client.app.dependency_overrides[get_db]
    generator = override_get_db()
    db = next(generator)
    try:
        yield db
    finally:
        generator.close()


def _register_parent(client, slug: str) -> dict:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": f"parent.refresh.{slug}@example.com",
            "password": PASSWORD,
            "family_name": f"Famille refresh {slug}",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    return response.json()


def _login(client, slug: str) -> dict:
    response = client.post(
        f"{API}/auth/login",
        json={"email": f"parent.refresh.{slug}@example.com", "password": PASSWORD},
    )
    assert response.status_code == 200
    return response.json()


def _token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _stored_refresh_token(db, token: str) -> RefreshToken:
    stored = db.scalar(select(RefreshToken).where(RefreshToken.token_hash == _token_hash(token)))
    assert stored is not None
    return stored


def test_login_emits_access_and_distinct_hashed_refresh_tokens(client) -> None:
    registered = _register_parent(client, "login")
    logged_in = _login(client, "login")

    assert registered["access_token"]
    assert registered["refresh_token"]
    assert registered["refresh_expires_in"] == 30 * 24 * 60 * 60
    assert logged_in["access_token"]
    assert logged_in["refresh_token"]
    assert logged_in["refresh_token"] != registered["refresh_token"]

    with _db_session(client) as db:
        user = db.scalar(select(User).where(User.email == "parent.refresh.login@example.com"))
        assert user is not None
        tokens = db.scalars(select(RefreshToken).where(RefreshToken.user_id == user.id)).all()
        assert len(tokens) == 2
        assert len({token.session_id for token in tokens}) == 2

        registered_record = _stored_refresh_token(db, registered["refresh_token"])
        assert registered_record.token_hash == _token_hash(registered["refresh_token"])
        assert registered_record.token_hash != registered["refresh_token"]


def test_refresh_rotates_token_and_old_token_cannot_be_reused(client) -> None:
    session = _register_parent(client, "rotation")
    old_refresh_token = session["refresh_token"]

    with _db_session(client) as db:
        old_id = _stored_refresh_token(db, old_refresh_token).id

    refreshed = client.post(f"{API}/auth/refresh", json={"refresh_token": old_refresh_token})
    assert refreshed.status_code == 200
    refreshed_payload = refreshed.json()
    assert refreshed_payload["access_token"]
    assert refreshed_payload["refresh_token"] != old_refresh_token
    assert refreshed_payload["role"] == "PARENT"

    me = client.get(
        f"{API}/auth/me",
        headers={"Authorization": f"Bearer {refreshed_payload['access_token']}"},
    )
    assert me.status_code == 200

    reused = client.post(f"{API}/auth/refresh", json={"refresh_token": old_refresh_token})
    assert reused.status_code == 401

    with _db_session(client) as db:
        old_record = db.get(RefreshToken, old_id)
        replacement = _stored_refresh_token(db, refreshed_payload["refresh_token"])
        assert old_record is not None
        assert old_record.revoked_at is not None
        assert old_record.last_used_at is not None
        assert old_record.replaced_by_token_id == replacement.id
        assert replacement.revoked_at is None
        assert replacement.session_id == old_record.session_id


def test_invalid_expired_revoked_and_inactive_user_refreshes_are_refused(client) -> None:
    invalid = client.post(f"{API}/auth/refresh", json={"refresh_token": "not-a-real-refresh-token"})
    assert invalid.status_code == 401

    registered = _register_parent(client, "refusals")
    with _db_session(client) as db:
        stored = _stored_refresh_token(db, registered["refresh_token"])
        stored.expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
        db.commit()
    expired = client.post(f"{API}/auth/refresh", json={"refresh_token": registered["refresh_token"]})
    assert expired.status_code == 401

    revoked_session = _login(client, "refusals")
    with _db_session(client) as db:
        stored = _stored_refresh_token(db, revoked_session["refresh_token"])
        stored.revoked_at = datetime.now(timezone.utc)
        db.commit()
    revoked = client.post(f"{API}/auth/refresh", json={"refresh_token": revoked_session["refresh_token"]})
    assert revoked.status_code == 401

    inactive_session = _login(client, "refusals")
    with _db_session(client) as db:
        stored = _stored_refresh_token(db, inactive_session["refresh_token"])
        user = db.get(User, stored.user_id)
        assert user is not None
        user.is_active = False
        db.commit()
    inactive = client.post(f"{API}/auth/refresh", json={"refresh_token": inactive_session["refresh_token"]})
    assert inactive.status_code == 401


def test_logout_revokes_only_the_supplied_device_session(client) -> None:
    _register_parent(client, "multi-device")
    phone_session = _login(client, "multi-device")
    tablet_session = _login(client, "multi-device")
    assert phone_session["refresh_token"] != tablet_session["refresh_token"]

    with _db_session(client) as db:
        phone_record = _stored_refresh_token(db, phone_session["refresh_token"])
        tablet_record = _stored_refresh_token(db, tablet_session["refresh_token"])
        assert phone_record.session_id != tablet_record.session_id

    logged_out = client.post(f"{API}/auth/logout", json={"refresh_token": phone_session["refresh_token"]})
    assert logged_out.status_code == 204
    assert logged_out.content == b""

    phone_refresh = client.post(f"{API}/auth/refresh", json={"refresh_token": phone_session["refresh_token"]})
    assert phone_refresh.status_code == 401

    tablet_refresh = client.post(f"{API}/auth/refresh", json={"refresh_token": tablet_session["refresh_token"]})
    assert tablet_refresh.status_code == 200


def test_child_account_uses_the_same_refresh_flow(client) -> None:
    registered = client.post(
        f"{API}/auth/register-child",
        json={
            "email": "child.refresh@example.com",
            "password": "childsecret123",
            "display_name": "Child Refresh",
        },
    )
    assert registered.status_code == 201
    assert registered.json()["refresh_token"]

    refreshed = client.post(
        f"{API}/auth/refresh",
        json={"refresh_token": registered.json()["refresh_token"]},
    )
    assert refreshed.status_code == 200
    assert refreshed.json()["role"] == "CHILD"


def test_atomic_refresh_consumption_rejects_a_stale_concurrent_reader(client, monkeypatch) -> None:
    session = _register_parent(client, "concurrency")
    raw_token = session["refresh_token"]

    with _db_session(client) as db:
        original = _stored_refresh_token(db, raw_token)
        stale_read = SimpleNamespace(
            id=original.id,
            user_id=original.user_id,
            session_id=original.session_id,
            expires_at=original.expires_at,
            revoked_at=None,
        )

        _, replacement, _ = refresh_token_service.rotate_refresh_token(db, token=raw_token)
        replacement_id = replacement.id
        db.commit()

        monkeypatch.setattr(
            refresh_token_service,
            "_get_refresh_token",
            lambda _db, *, token: stale_read,
        )

        with pytest.raises(HTTPException) as collision:
            refresh_token_service.rotate_refresh_token(db, token=raw_token)

        assert collision.value.status_code == 401
        assert db.scalar(select(func.count(RefreshToken.id))) == 2
        assert db.scalar(
            select(func.count(RefreshToken.id)).where(RefreshToken.revoked_at.is_(None))
        ) == 1
        assert db.get(RefreshToken, replacement_id) is not None
