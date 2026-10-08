from tests.test_gamification_api import _register_child, _register_parent


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_eight_lineages_and_parent_child_personal_starters(client):
    parent = _register_parent(client, "companion.parent@example.com", "QA Companion")
    child = _register_child(client, "companion.child@example.com", "Child")

    assert client.get("/api/v1/gamification/lineages").status_code == 401
    lineages = client.get("/api/v1/gamification/lineages", headers=_headers(parent))
    assert lineages.status_code == 200
    entries = lineages.json()["data"]["lineages"]
    assert [entry["id"] for entry in entries] == [
        "FULMIO", "SYLVYN", "PHENOR", "LUNARYS", "PYRON", "CHRONYX", "AMBRIO", "CRISTAO"
    ]

    for index, (token, lineage_id) in enumerate(((parent, "FULMIO"), (child, "PYRON"))):
        response = client.post(
            "/api/v1/me/companions/starter",
            headers=_headers(token),
            json={"lineage_id": lineage_id, "display_name": f"Compagnon {chr(65 + index)}"},
        )
        assert response.status_code == 201, response.text
        companion = response.json()["data"]["companion"]
        assert companion["lineage_id"] == lineage_id
        assert companion["lineage_status"] == "OFFICIAL"
        assert companion["stage"] == "baby"
        assert companion["progress"] == 0
        assert companion["active"] is True

        current = client.get("/api/v1/me/companions/active", headers=_headers(token))
        assert current.status_code == 200
        assert current.json()["data"]["dragon_id"] == companion["dragon_id"]


def test_starter_retry_is_idempotent_and_different_choice_conflicts(client):
    token = _register_child(client, "starter.retry@example.com", "Child")
    body = {"lineage_id": "AMBRIO", "display_name": "  \u00c9toile  "}
    first = client.post("/api/v1/me/companions/starter", headers=_headers(token), json=body)
    assert first.status_code == 201, first.text
    normalized = first.json()["data"]["companion"]["display_name"]
    assert normalized == "\u00c9toile"

    retry = client.post("/api/v1/me/companions/starter", headers=_headers(token), json=body)
    assert retry.status_code == 201
    assert retry.json()["data"]["created"] is False
    assert retry.json()["data"]["companion"]["dragon_id"] == first.json()["data"]["companion"]["dragon_id"]

    conflict = client.post(
        "/api/v1/me/companions/starter",
        headers=_headers(token),
        json={"lineage_id": "FULMIO", "display_name": "Other"},
    )
    assert conflict.status_code == 409


def test_name_validation_and_foreign_companions_are_rejected(client):
    owner = _register_child(client, "companion.owner@example.com", "Owner")
    other = _register_child(client, "companion.other@example.com", "Other")
    for name in (" ", "bad\nname", "bad/name", "x" * 33):
        response = client.post(
            "/api/v1/me/companions/starter",
            headers=_headers(owner),
            json={"lineage_id": "SYLVYN", "display_name": name},
        )
        assert response.status_code == 422

    starter = client.post(
        "/api/v1/me/companions/starter",
        headers=_headers(owner),
        json={"lineage_id": "SYLVYN", "display_name": "Root"},
    ).json()["data"]["companion"]
    activate = client.post(
        f"/api/v1/me/companions/{starter['dragon_id']}/activate", headers=_headers(other)
    )
    rename = client.patch(
        f"/api/v1/me/companions/{starter['dragon_id']}/name",
        headers=_headers(other),
        json={"display_name": "Taken"},
    )
    assert activate.status_code == 404
    assert rename.status_code == 404
    assert client.get("/api/v1/me/companions", headers=_headers(other)).json()["data"]["companions"] == []


def test_activation_and_rename_preserve_identity_and_progress(client):
    token = _register_child(client, "companion.change@example.com", "Child")
    first = client.post(
        "/api/v1/me/companions/starter",
        headers=_headers(token),
        json={"lineage_id": "CRISTAO", "display_name": "Prisme"},
    ).json()["data"]["companion"]

    # Add a second owned dragon using the existing legacy egg-hatch model path.
    from app.db.session import get_db
    from app.models.gamification import ChildDragon, DragonDefinition, DragonStage
    from app.models.user import User
    from sqlalchemy import select

    db_generator = client.app.dependency_overrides[get_db]()
    db = next(db_generator)
    user = db.scalar(select(User).where(User.email == "companion.change@example.com"))
    db.add(DragonDefinition(key="dragon_braise", title="Dragon braise", is_active=True))
    db.add(ChildDragon(child_id=user.id, dragon_key="dragon_braise", stage=DragonStage.YOUNG, progress=37))
    db.commit()
    db.close()

    listing = client.get("/api/v1/me/companions", headers=_headers(token)).json()["data"]["companions"]
    second = next(item for item in listing if item["dragon_key"] == "dragon_braise")
    switched = client.post(f"/api/v1/me/companions/{second['dragon_id']}/activate", headers=_headers(token))
    assert switched.status_code == 200
    active = client.get("/api/v1/me/companions/active", headers=_headers(token)).json()["data"]
    assert active["dragon_id"] == second["dragon_id"]
    assert active["lineage_status"] == "LEGACY_UNKNOWN"
    assert active["lineage_id"] is None
    assert active["stage"] == "young" and active["progress"] == 37

    renamed = client.patch(
        f"/api/v1/me/companions/{first['dragon_id']}/name",
        headers=_headers(token),
        json={"display_name": "Cristal"},
    )
    assert renamed.status_code == 200
    assert renamed.json()["data"]["companion"]["lineage_id"] == "CRISTAO"
    assert renamed.json()["data"]["companion"]["display_name"] == "Cristal"


def test_same_lineage_and_same_name_are_independent_between_players(client):
    first_token = _register_child(client, "lineage.first@example.com", "First")
    second_token = _register_parent(client, "lineage.second@example.com", "Second")
    first = client.post(
        "/api/v1/me/companions/starter", headers=_headers(first_token),
        json={"lineage_id": "FULMIO", "display_name": "Azur"},
    )
    second = client.post(
        "/api/v1/me/companions/starter", headers=_headers(second_token),
        json={"lineage_id": "FULMIO", "display_name": "Azur"},
    )
    assert first.status_code == second.status_code == 201
    assert first.json()["data"]["companion"]["dragon_id"] != second.json()["data"]["companion"]["dragon_id"]
    assert first.json()["data"]["companion"]["owner_user_id"] != second.json()["data"]["companion"]["owner_user_id"]


def test_existing_legacy_companion_is_reused_without_official_lineage(client):
    token = _register_child(client, "legacy.existing@example.com", "Legacy")
    from sqlalchemy import select
    from app.db.session import get_db
    from app.models.gamification import ChildDragon, DragonDefinition, DragonStage
    from app.models.user import User

    db = next(client.app.dependency_overrides[get_db]())
    user = db.scalar(select(User).where(User.email == "legacy.existing@example.com"))
    db.add(DragonDefinition(key="dragon_racine", title="Dragon racine", is_active=True))
    db.add(ChildDragon(child_id=user.id, dragon_key="dragon_racine", stage=DragonStage.YOUNG, progress=55, active_companion=True))
    db.commit()
    db.close()

    response = client.post(
        "/api/v1/me/companions/starter", headers=_headers(token),
        json={"lineage_id": "PYRON", "display_name": "Flamme"},
    )
    assert response.status_code == 201
    data = response.json()["data"]
    assert data["created"] is False and data["reused_existing"] is True
    assert data["companion"]["dragon_key"] == "dragon_racine"
    assert data["companion"]["lineage_status"] == "LEGACY_UNKNOWN"
    assert data["companion"]["lineage_id"] is None
    assert data["companion"]["progress"] == 55


def test_parent_cannot_access_child_companion_as_its_own(client):
    parent = _register_parent(client, "owner.parent@example.com", "Owner")
    child = _register_child(client, "owner.child@example.com", "Child")
    child_starter = client.post(
        "/api/v1/me/companions/starter", headers=_headers(child),
        json={"lineage_id": "LUNARYS", "display_name": "Moon"},
    ).json()["data"]["companion"]

    assert client.get("/api/v1/me/companions", headers=_headers(parent)).json()["data"]["companions"] == []
    assert client.post(
        f"/api/v1/me/companions/{child_starter['dragon_id']}/activate", headers=_headers(parent)
    ).status_code == 404

    forged_owner = client.post(
        "/api/v1/me/companions/starter", headers=_headers(parent),
        json={"lineage_id": "FULMIO", "display_name": "Owned", "user_id": 99999},
    )
    assert forged_owner.status_code == 422


def test_every_official_lineage_can_be_selected(client):
    for index, lineage_id in enumerate(("FULMIO", "SYLVYN", "PHENOR", "LUNARYS", "PYRON", "CHRONYX", "AMBRIO", "CRISTAO")):
        token = _register_child(client, f"lineage.pick{index}@example.com", f"Player {chr(65 + index)}")
        response = client.post(
            "/api/v1/me/companions/starter", headers=_headers(token),
            json={"lineage_id": lineage_id, "display_name": f"Dragon {chr(65 + index)}"},
        )
        assert response.status_code == 201, response.text
        assert response.json()["data"]["companion"]["lineage_id"] == lineage_id


def test_concurrent_identical_starter_requests_create_one_dragon(client):
    from concurrent.futures import ThreadPoolExecutor

    token = _register_child(client, "starter.concurrent@example.com", "Concurrent")
    body = {"lineage_id": "CHRONYX", "display_name": "Moment"}

    def create():
        return client.post("/api/v1/me/companions/starter", headers=_headers(token), json=body)

    with ThreadPoolExecutor(max_workers=2) as executor:
        responses = list(executor.map(lambda _: create(), range(2)))
    assert all(response.status_code == 201 for response in responses), [response.text for response in responses]
    ids = {response.json()["data"]["companion"]["dragon_id"] for response in responses}
    assert len(ids) == 1
    listing = client.get("/api/v1/me/companions", headers=_headers(token)).json()["data"]["companions"]
    assert len(listing) == 1

    from sqlalchemy import select
    from app.db.session import get_db
    from app.models.gamification import ChildDragon, DragonDefinition, DragonStage
    from app.models.user import User

    db = next(client.app.dependency_overrides[get_db]())
    owner = db.scalar(select(User).where(User.email == "starter.concurrent@example.com"))
    db.add(DragonDefinition(key="dragon_braise", title="Legacy", is_active=True))
    other = ChildDragon(child_id=owner.id, dragon_key="dragon_braise", stage=DragonStage.BABY, progress=0)
    db.add(other)
    db.commit()
    other_id = other.id
    db.close()

    def activate(dragon_id: int):
        return client.post(f"/api/v1/me/companions/{dragon_id}/activate", headers=_headers(token))

    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(activate, (next(iter(ids)), other_id)))
    assert all(result.status_code == 200 for result in results), [result.text for result in results]
    listing_after = client.get("/api/v1/me/companions", headers=_headers(token)).json()["data"]["companions"]
    assert sum(item["active"] for item in listing_after) == 1
