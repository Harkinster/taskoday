from contextlib import contextmanager
from datetime import date, datetime, timedelta, timezone
from typing import Generator

from sqlalchemy import func, select

from app.core.security import get_password_hash
from app.db.session import get_db
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_invite import FamilyInvite
from app.models.user import User, UserRole


API = "/api/v1"
PASSWORD = "supersecret123"


def _headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@contextmanager
def _db_session(client) -> Generator:
    override_get_db = client.app.dependency_overrides[get_db]
    generator = override_get_db()
    db = next(generator)
    try:
        yield db
    finally:
        generator.close()


def _register_parent(client, slug: str) -> tuple[str, int, int]:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": f"parent.{slug}@example.com",
            "password": PASSWORD,
            "family_name": f"Famille {slug}",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]

    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    return token, me.json()["id"], me.json()["family_ids"][0]


def _register_child_and_attach(client, parent_token: str, slug: str) -> tuple[str, int]:
    child = client.post(
        f"{API}/auth/register-child",
        json={
            "email": f"child.{slug}@example.com",
            "password": "childsecret123",
            "display_name": f"Child {slug}",
        },
    )
    assert child.status_code == 201
    child_token = child.json()["access_token"]

    pairing = client.post(f"{API}/pairing/generate-code", headers=_headers(child_token))
    assert pairing.status_code == 200
    attached = client.post(
        f"{API}/pairing/attach-child",
        headers=_headers(parent_token),
        json={"code": pairing.json()["data"]["code"]},
    )
    assert attached.status_code == 200
    return child_token, attached.json()["data"]["child_id"]


def _create_orphan_parent(client, slug: str) -> tuple[str, int]:
    email = f"orphan.parent.{slug}@example.com"
    with _db_session(client) as db:
        user = User(
            email=email,
            password_hash=get_password_hash(PASSWORD),
            role=UserRole.PARENT,
            birth_date=date(1988, 1, 20),
        )
        db.add(user)
        db.commit()
        user_id = user.id

    login = client.post(f"{API}/auth/login", json={"email": email, "password": PASSWORD})
    assert login.status_code == 200
    return login.json()["access_token"], user_id


def _create_invite(client, parent_token: str, family_id: int) -> dict:
    response = client.post(f"{API}/families/{family_id}/parent-invites", headers=_headers(parent_token))
    assert response.status_code == 201
    return response.json()["data"]


def _family_count(client) -> int:
    with _db_session(client) as db:
        return int(db.scalar(select(func.count(Family.id))) or 0)


def test_parent_invite_creation_permissions_and_replacement(client) -> None:
    parent_token, parent_id, family_id = _register_parent(client, "invite-create")
    child_token, _ = _register_child_and_attach(client, parent_token, "invite-create")
    other_parent_token, _, _ = _register_parent(client, "invite-create-other")

    first_invite = _create_invite(client, parent_token, family_id)
    assert first_invite["family_id"] == family_id
    assert first_invite["family_name"] == "Famille invite-create"
    assert first_invite["code"]
    assert len(first_invite["code"]) >= 16
    assert first_invite["expires_at"]

    second_invite = _create_invite(client, parent_token, family_id)
    assert second_invite["code"] != first_invite["code"]

    with _db_session(client) as db:
        active_invites = db.scalars(
            select(FamilyInvite).where(
                FamilyInvite.family_id == family_id,
                FamilyInvite.created_by_user_id == parent_id,
                FamilyInvite.accepted_at.is_(None),
                FamilyInvite.expires_at > datetime.now(timezone.utc),
            )
        ).all()
        assert len(active_invites) == 1
        assert active_invites[0].id == second_invite["invite_id"]

    child_create = client.post(f"{API}/families/{family_id}/parent-invites", headers=_headers(child_token))
    assert child_create.status_code == 403

    other_family_create = client.post(
        f"{API}/families/{family_id}/parent-invites",
        headers=_headers(other_parent_token),
    )
    assert other_family_create.status_code == 404


def test_existing_parent_accepts_invite_once(client) -> None:
    parent_token, _, family_id = _register_parent(client, "invite-accept")
    invite = _create_invite(client, parent_token, family_id)
    parent_b_token, parent_b_id = _create_orphan_parent(client, "invite-accept")

    accepted = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(parent_b_token))
    assert accepted.status_code == 200
    assert accepted.json()["data"]["family_id"] == family_id

    me = client.get(f"{API}/auth/me", headers=_headers(parent_b_token))
    assert me.status_code == 200
    assert me.json()["family_ids"] == [family_id]

    with _db_session(client) as db:
        membership = db.scalar(
            select(FamilyMember).where(
                FamilyMember.family_id == family_id,
                FamilyMember.user_id == parent_b_id,
                FamilyMember.role == FamilyMemberRole.PARENT,
            )
        )
        stored_invite = db.get(FamilyInvite, invite["invite_id"])
        assert membership is not None
        assert stored_invite.accepted_at is not None
        assert stored_invite.accepted_by_user_id == parent_b_id

    parent_c_token, _ = _create_orphan_parent(client, "invite-reuse")
    reused = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(parent_c_token))
    assert reused.status_code == 409


def test_invite_acceptance_refuses_invalid_accounts_and_codes(client) -> None:
    parent_token, _, family_id = _register_parent(client, "invite-refusals")
    child_token, _ = _register_child_and_attach(client, parent_token, "invite-refusals")
    invite = _create_invite(client, parent_token, family_id)
    orphan_token, _ = _create_orphan_parent(client, "invite-refusals")

    bad_code = client.post(f"{API}/family-invites/not-a-real-token/accept", headers=_headers(orphan_token))
    assert bad_code.status_code == 404

    child_accept = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(child_token))
    assert child_accept.status_code == 403

    same_family = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(parent_token))
    assert same_family.status_code == 409
    assert same_family.json()["error"]["message"] == "Ce compte appartient deja a ce foyer."

    other_parent_token, _, _ = _register_parent(client, "invite-refusals-other")
    other_family = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(other_parent_token))
    assert other_family.status_code == 409
    assert other_family.json()["error"]["message"] == "Ce compte appartient deja a un foyer."

    expired_invite = _create_invite(client, parent_token, family_id)
    with _db_session(client) as db:
        stored_invite = db.get(FamilyInvite, expired_invite["invite_id"])
        stored_invite.expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
        db.commit()

    expired = client.post(f"{API}/family-invites/{expired_invite['code']}/accept", headers=_headers(orphan_token))
    assert expired.status_code == 409
    assert expired.json()["error"]["message"] == "Invitation expiree."


def test_register_parent_with_invite_joins_existing_family_without_creating_family(client) -> None:
    parent_token, _, family_id = _register_parent(client, "invite-register")
    invite = _create_invite(client, parent_token, family_id)
    family_count_before = _family_count(client)

    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "invited.parent@example.com",
            "password": PASSWORD,
            "birth_date": "1988-01-20",
            "invite_code": invite["code"],
        },
    )
    assert response.status_code == 201
    invited_token = response.json()["access_token"]

    me = client.get(f"{API}/auth/me", headers=_headers(invited_token))
    assert me.status_code == 200
    assert me.json()["family_ids"] == [family_id]
    assert _family_count(client) == family_count_before

    with _db_session(client) as db:
        stored_invite = db.get(FamilyInvite, invite["invite_id"])
        assert stored_invite.accepted_at is not None


def test_register_parent_without_invite_keeps_existing_behavior(client) -> None:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": "normal.parent@example.com",
            "password": PASSWORD,
            "family_name": "Famille normale",
            "birth_date": "1988-01-20",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]

    me = client.get(f"{API}/auth/me", headers=_headers(token))
    assert me.status_code == 200
    assert len(me.json()["family_ids"]) == 1


def test_second_parent_gets_family_permissions_after_acceptance(client) -> None:
    parent_token, _, family_id = _register_parent(client, "invite-permissions")
    _, child_id = _register_child_and_attach(client, parent_token, "invite-permissions")
    today = date.today()

    task = client.post(
        f"{API}/families/{family_id}/tasks",
        headers=_headers(parent_token),
        json={
            "title": "Tache partagee foyer",
            "due_date": today.isoformat(),
            "assignee_user_ids": [child_id],
        },
    )
    assert task.status_code == 201

    invite = _create_invite(client, parent_token, family_id)
    parent_b_token, _ = _create_orphan_parent(client, "invite-permissions")
    accepted = client.post(f"{API}/family-invites/{invite['code']}/accept", headers=_headers(parent_b_token))
    assert accepted.status_code == 200

    members = client.get(f"{API}/families/{family_id}/members", headers=_headers(parent_b_token))
    assert members.status_code == 200
    roles = [member["role"] for member in members.json()["data"]]
    assert roles.count("PARENT") == 2
    assert "CHILD" in roles

    children = client.get(f"{API}/families/{family_id}/children", headers=_headers(parent_b_token))
    assert children.status_code == 200
    assert [child["id"] for child in children.json()["data"]] == [child_id]

    tasks = client.get(f"{API}/families/{family_id}/tasks", headers=_headers(parent_b_token))
    assert tasks.status_code == 200
    assert [item["title"] for item in tasks.json()["data"]] == ["Tache partagee foyer"]

    today_response = client.get(
        f"{API}/families/{family_id}/tasks/today",
        headers=_headers(parent_b_token),
        params={"date": today.isoformat()},
    )
    assert today_response.status_code == 200
    assert [item["title"] for item in today_response.json()["data"]["items"]] == ["Tache partagee foyer"]

    range_response = client.get(
        f"{API}/families/{family_id}/task-occurrences",
        headers=_headers(parent_b_token),
        params={"start_date": today.isoformat(), "end_date": today.isoformat()},
    )
    assert range_response.status_code == 200
    assert [item["title"] for item in range_response.json()["data"]["items"]] == ["Tache partagee foyer"]
