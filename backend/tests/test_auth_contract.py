import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

os.environ.setdefault("DATABASE_URL", "sqlite:///./taskoday_test.db")
os.environ.setdefault("JWT_SECRET_KEY", "test_secret_key_change_me")

from app.db.base import Base
from app.db.session import SessionLocal
from app.db.session import engine
from app.main import app
from app.models.child import ChildProfile
from app.models.user import User


@pytest.fixture(autouse=True)
def reset_db():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield


@pytest.fixture()
def client():
    with TestClient(app) as test_client:
        yield test_client


def test_register_parent_returns_token_and_creates_family_membership(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/register-parent",
        json={
            "email": "parent@example.com",
            "password": "password123",
            "family_name": "Famille Martin",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201

    payload = response.json()
    assert payload["token_type"] == "bearer"
    assert payload["expires_in"] > 0
    assert payload["role"] == "PARENT"
    assert isinstance(payload["access_token"], str)

    me_response = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {payload['access_token']}"})
    assert me_response.status_code == 200
    me_payload = me_response.json()
    assert me_payload["email"] == "parent@example.com"
    assert me_payload["role"] == "PARENT"
    assert me_payload["is_active"] is True
    assert me_payload["family_ids"] == [1]

    db = SessionLocal()
    try:
        user = db.query(User).filter(User.email == "parent@example.com").one()
        assert str(user.birth_date) == "1988-01-20"
    finally:
        db.close()


def test_register_parent_missing_birth_date_returns_422(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/register-parent",
        json={
            "email": "parent@example.com",
            "password": "password123",
            "family_name": "Famille Martin",
        },
    )
    assert response.status_code == 422


def test_register_child_accepts_birth_date_and_returns_token_schema(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/register-child",
        json={
            "email": "enfant@example.com",
            "password": "password123",
            "display_name": "Lina",
            "birth_date": "2014-07-03",
        },
    )
    assert response.status_code == 201
    assert response.json().keys() == {"access_token", "token_type", "expires_in", "role"}
    assert response.json()["role"] == "CHILD"

    db = SessionLocal()
    try:
        profile = db.query(ChildProfile).one()
        assert str(profile.birth_date) == "2014-07-03"
    finally:
        db.close()


def test_register_child_invalid_birth_date_returns_422(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/register-child",
        json={
            "email": "enfant@example.com",
            "password": "password123",
            "display_name": "Lina",
            "birth_date": "03-07-2014",
        },
    )
    assert response.status_code == 422


def test_login_uses_same_token_response_shape(client: TestClient) -> None:
    register_response = client.post(
        "/api/v1/auth/register-child",
        json={
            "email": "child-login@example.com",
            "password": "password123",
            "display_name": "Lina",
        },
    )
    assert register_response.status_code == 201
    register_keys = set(register_response.json().keys())

    login_response = client.post(
        "/api/v1/auth/login",
        json={
            "email": "child-login@example.com",
            "password": "password123",
        },
    )
    assert login_response.status_code == 200
    login_payload = login_response.json()
    assert set(login_payload.keys()) == register_keys
    assert login_payload["role"] == "CHILD"


def test_register_conflicts_return_409(client: TestClient) -> None:
    first = client.post(
        "/api/v1/auth/register-parent",
        json={
            "email": "parent@example.com",
            "password": "password123",
            "family_name": "Famille Martin",
            "birth_date": "1988-01-20",
        },
    )
    assert first.status_code == 201

    duplicate_email = client.post(
        "/api/v1/auth/register-parent",
        json={
            "email": "parent@example.com",
            "password": "password123",
            "family_name": "Famille Dupont",
            "birth_date": "1990-05-10",
        },
    )
    assert duplicate_email.status_code == 409

    duplicate_family_name = client.post(
        "/api/v1/auth/register-parent",
        json={
            "email": "other-parent@example.com",
            "password": "password123",
            "family_name": "Famille Martin",
            "birth_date": "1992-03-15",
        },
    )
    assert duplicate_family_name.status_code == 409
