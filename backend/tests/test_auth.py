def test_register_login_and_me_flow(client) -> None:
    register_payload = {
        "email": "parent@example.com",
        "password": "supersecret123",
        "family_name": "Famille Martin",
    }

    register_response = client.post("/auth/register-parent", json=register_payload)
    assert register_response.status_code == 201
    register_data = register_response.json()
    assert register_data["token_type"] == "bearer"
    assert register_data["role"] == "PARENT"
    assert register_data["access_token"]

    login_response = client.post(
        "/auth/login",
        json={"email": "parent@example.com", "password": "supersecret123"},
    )
    assert login_response.status_code == 200
    token = login_response.json()["access_token"]
    assert token

    me_response = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert me_response.status_code == 200
    me_data = me_response.json()
    assert me_data["email"] == "parent@example.com"
    assert me_data["role"] == "PARENT"
    assert len(me_data["family_ids"]) == 1


def test_login_invalid_credentials(client) -> None:
    client.post(
        "/auth/register-parent",
        json={
            "email": "other.parent@example.com",
            "password": "supersecret123",
            "family_name": "Famille Dupond",
        },
    )

    response = client.post(
        "/auth/login",
        json={"email": "other.parent@example.com", "password": "wrong-password"},
    )
    assert response.status_code == 401


def test_me_requires_authentication(client) -> None:
    response = client.get("/auth/me")
    assert response.status_code == 401

