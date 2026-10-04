"""Family membership mutations use isolated test databases, never production QA families."""

from contextlib import contextmanager
from datetime import date

from sqlalchemy import select

from app.db.session import get_db
from app.models.child import ChildProfile
from app.models.family import Family, FamilyMember, FamilyMemberRole
from app.models.family_task import FamilyTask, FamilyTaskAssignee, FamilyTaskOccurrence, FamilyTaskOccurrenceStatus
from app.models.family_invite import FamilyInvite
from app.models.user import User

API = "/api/v1"
TEST_PASSWORD = "only-unit-test-123"


def headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@contextmanager
def db_session(client):
    generator = client.app.dependency_overrides[get_db]()
    db = next(generator)
    try:
        yield db
    finally:
        generator.close()


def parent(client, slug: str) -> tuple[str, int]:
    response = client.post(
        f"{API}/auth/register-parent",
        json={
            "email": f"{slug}@example.com",
            "password": TEST_PASSWORD,
            "display_name": slug,
            "birth_date": "1985-01-15",
        },
    )
    assert response.status_code == 201
    token = response.json()["access_token"]
    user_id = client.get(f"{API}/auth/me", headers=headers(token)).json()["id"]
    return token, user_id


def family(client, token: str, slug: str) -> int:
    response = client.post(f"{API}/families", headers=headers(token), json={"name": f"Famille {slug}"})
    assert response.status_code == 200
    return response.json()["data"]["id"]


def join_parent(client, family_id: int, owner_token: str, joining_token: str) -> None:
    invite = client.post(f"{API}/families/{family_id}/parent-invites", headers=headers(owner_token))
    assert invite.status_code == 201
    accepted = client.post(
        f"{API}/family-invites/{invite.json()['data']['code']}/accept",
        headers=headers(joining_token),
    )
    assert accepted.status_code == 200


def child(client, family_id: int, parent_token: str, slug: str) -> tuple[str, int]:
    response = client.post(
        f"{API}/auth/register-child",
        json={"email": f"child.{slug}@example.com", "password": TEST_PASSWORD, "display_name": slug},
    )
    assert response.status_code == 201
    token = response.json()["access_token"]
    code = client.post(f"{API}/pairing/generate-code", headers=headers(token)).json()["data"]["code"]
    attached = client.post(
        f"{API}/pairing/attach-child",
        headers=headers(parent_token),
        json={"family_id": family_id, "code": code},
    )
    assert attached.status_code == 200
    return token, attached.json()["data"]["child_id"]


def member_ids(client, family_id: int, token: str) -> set[int]:
    response = client.get(f"{API}/families/{family_id}/members", headers=headers(token))
    assert response.status_code == 200
    return {item["user_id"] for item in response.json()["data"]}


def test_parent_detaches_child_only_from_target_family_and_preserves_user_profile(client) -> None:
    owner_token, owner_id = parent(client, "remove-child-owner")
    family_a = family(client, owner_token, "remove-child-a")
    family_b = family(client, owner_token, "remove-child-b")
    child_token, child_id = child(client, family_a, owner_token, "remove-child")
    with db_session(client) as db:
        db.add(FamilyMember(family_id=family_b, user_id=child_id, role=FamilyMemberRole.CHILD))
        task = FamilyTask(family_id=family_a, title="Test assignment", creator_user_id=owner_id)
        other_task = FamilyTask(family_id=family_b, title="Other family assignment", creator_user_id=owner_id)
        db.add(task)
        db.add(other_task)
        db.flush()
        db.add(FamilyTaskAssignee(task_id=task.id, user_id=child_id))
        db.add(FamilyTaskAssignee(task_id=other_task.id, user_id=child_id))
        other_task_id = other_task.id
        db.commit()

    response = client.delete(f"{API}/families/{family_a}/members/{child_id}", headers=headers(owner_token))
    assert response.status_code == 200
    assert child_id not in member_ids(client, family_a, owner_token)
    assert child_id in member_ids(client, family_b, owner_token)
    assert client.get(f"{API}/auth/me", headers=headers(child_token)).json()["family_ids"] == [family_b]
    with db_session(client) as db:
        assert db.get(User, child_id) is not None
        assert db.scalar(select(ChildProfile).where(ChildProfile.user_id == child_id)) is not None
        remaining_assignment = db.scalar(select(FamilyTaskAssignee).where(FamilyTaskAssignee.user_id == child_id))
        assert remaining_assignment is not None
        assert remaining_assignment.task_id == other_task_id


def test_parent_can_remove_another_parent_and_revokes_their_pending_invite(client) -> None:
    owner_token, owner_id = parent(client, "remove-parent-owner")
    other_token, other_id = parent(client, "remove-parent-other")
    family_id = family(client, owner_token, "remove-parent")
    join_parent(client, family_id, owner_token, other_token)
    pending = client.post(f"{API}/families/{family_id}/parent-invites", headers=headers(other_token))
    assert pending.status_code == 201

    response = client.delete(f"{API}/families/{family_id}/members/{other_id}", headers=headers(owner_token))
    assert response.status_code == 200
    assert member_ids(client, family_id, owner_token) == {owner_id}
    assert client.get(f"{API}/auth/me", headers=headers(other_token)).json()["family_ids"] == []
    with db_session(client) as db:
        assert db.get(User, other_id) is not None
    outsider_token, _ = parent(client, "remove-parent-outsider")
    reused = client.post(
        f"{API}/family-invites/{pending.json()['data']['code']}/accept",
        headers=headers(outsider_token),
    )
    assert reused.status_code == 409


def test_child_outsider_and_cross_family_idor_are_refused(client) -> None:
    owner_token, owner_id = parent(client, "idor-owner")
    outsider_token, outsider_id = parent(client, "idor-outsider")
    family_a = family(client, owner_token, "idor-a")
    family_b = family(client, outsider_token, "idor-b")
    child_token, child_id = child(client, family_a, owner_token, "idor")

    assert client.delete(f"{API}/families/{family_a}/members/{owner_id}", headers=headers(child_token)).status_code == 403
    assert client.post(f"{API}/families/{family_a}/leave", headers=headers(child_token)).status_code == 403
    assert client.delete(f"{API}/families/{family_a}/members/{child_id}", headers=headers(outsider_token)).status_code == 404
    assert client.delete(f"{API}/families/{family_b}/members/{outsider_id}", headers=headers(owner_token)).status_code == 404
    assert client.delete(f"{API}/families/{family_a}/members/{outsider_id}", headers=headers(owner_token)).status_code == 404
    assert client.post(f"{API}/families/{family_b}/leave", headers=headers(owner_token)).status_code == 404
    assert member_ids(client, family_a, owner_token) == {owner_id, child_id}

    # Even an inconsistent membership row must not grant an account-level CHILD administration rights.
    with db_session(client) as db:
        child_membership = db.scalar(
            select(FamilyMember).where(FamilyMember.family_id == family_a, FamilyMember.user_id == child_id)
        )
        child_membership.role = FamilyMemberRole.PARENT
        db.commit()
    assert client.delete(f"{API}/families/{family_a}/members/{owner_id}", headers=headers(child_token)).status_code == 403
    assert client.post(f"{API}/families/{family_a}/leave", headers=headers(child_token)).status_code == 403


def test_leave_only_one_family_keeps_other_memberships_and_user(client) -> None:
    owner_token, owner_id = parent(client, "leave-owner")
    other_token, other_id = parent(client, "leave-other")
    family_a = family(client, owner_token, "leave-a")
    family_b = family(client, owner_token, "leave-b")
    join_parent(client, family_a, owner_token, other_token)

    response = client.post(f"{API}/families/{family_a}/leave", headers=headers(owner_token))
    assert response.status_code == 200
    assert member_ids(client, family_a, other_token) == {other_id}
    assert member_ids(client, family_b, owner_token) == {owner_id}
    assert client.get(f"{API}/auth/me", headers=headers(owner_token)).json()["family_ids"] == [family_b]
    with db_session(client) as db:
        assert db.get(User, owner_id) is not None
        assert db.get(Family, family_a) is not None
        assert db.get(Family, family_b) is not None


def test_last_parent_protected_with_and_without_child(client) -> None:
    token, user_id = parent(client, "last-parent")
    family_id = family(client, token, "last-parent")
    assert client.post(f"{API}/families/{family_id}/leave", headers=headers(token)).status_code == 409
    assert client.delete(f"{API}/families/{family_id}/members/{user_id}", headers=headers(token)).status_code == 409
    _, child_id = child(client, family_id, token, "last-parent")
    assert client.post(f"{API}/families/{family_id}/leave", headers=headers(token)).status_code == 409
    assert member_ids(client, family_id, token) == {user_id, child_id}


def test_archive_requires_sole_parent_and_rejects_child_outsider_and_idor(client) -> None:
    owner_token, owner_id = parent(client, "archive-owner")
    outsider_token, _ = parent(client, "archive-outsider")
    family_id = family(client, owner_token, "archive-guarded")
    child_token, child_id = child(client, family_id, owner_token, "archive-guarded")

    assert client.post(f"{API}/families/{family_id}/archive", headers=headers(child_token)).status_code == 403
    assert client.post(f"{API}/families/{family_id}/archive", headers=headers(outsider_token)).status_code == 404
    refused = client.post(f"{API}/families/{family_id}/archive", headers=headers(owner_token))
    assert refused.status_code == 409
    assert "autres membres" in refused.json()["error"]["message"]
    assert member_ids(client, family_id, owner_token) == {owner_id, child_id}

    other_token, other_id = parent(client, "archive-other")
    join_parent(client, family_id, owner_token, other_token)
    assert client.post(f"{API}/families/{family_id}/archive", headers=headers(owner_token)).status_code == 409
    assert member_ids(client, family_id, owner_token) == {owner_id, child_id, other_id}


def test_archive_preserves_history_and_other_families_but_closes_all_active_paths(client) -> None:
    owner_token, owner_id = parent(client, "archive-history-owner")
    other_token, other_id = parent(client, "archive-history-other")
    archived_id = family(client, owner_token, "archive-history")
    active_id = family(client, owner_token, "archive-stays-active")
    child_token, child_id = child(client, archived_id, owner_token, "archive-history")
    with db_session(client) as db:
        task = FamilyTask(family_id=archived_id, title="Historique", creator_user_id=owner_id)
        db.add(task)
        db.flush()
        db.add(FamilyTaskOccurrence(task_id=task.id, scheduled_date=date(2026, 10, 3), status=FamilyTaskOccurrenceStatus.COMPLETED, completed_by_user_id=child_id))
        task_id = task.id
        db.commit()
    pending = client.post(f"{API}/families/{archived_id}/parent-invites", headers=headers(owner_token))
    assert pending.status_code == 201
    pending_code = pending.json()["data"]["code"]
    assert client.delete(f"{API}/families/{archived_id}/members/{child_id}", headers=headers(owner_token)).status_code == 200

    archived = client.post(f"{API}/families/{archived_id}/archive", headers=headers(owner_token))
    assert archived.status_code == 200
    assert client.get(f"{API}/auth/me", headers=headers(owner_token)).json()["family_ids"] == [active_id]
    assert [item["id"] for item in client.get(f"{API}/families/me", headers=headers(owner_token)).json()["data"]] == [active_id]
    assert client.get(f"{API}/families/{archived_id}/members", headers=headers(owner_token)).status_code == 404
    assert client.get(f"{API}/families/{archived_id}/children", headers=headers(owner_token)).status_code == 404
    assert client.get(f"{API}/families/{archived_id}/tasks", headers=headers(owner_token)).status_code == 404
    assert client.post(f"{API}/families/{archived_id}/tasks", headers=headers(owner_token), json={"title": "Non"}).status_code == 404
    assert client.post(f"{API}/families/{archived_id}/parent-invites", headers=headers(owner_token)).status_code == 404
    assert client.post(f"{API}/family-invites/{pending_code}/accept", headers=headers(other_token)).status_code in (404, 409)
    assert client.post(f"{API}/families/{archived_id}/leave", headers=headers(owner_token)).status_code == 404
    assert client.delete(f"{API}/families/{archived_id}/members/{owner_id}", headers=headers(other_token)).status_code == 404
    with db_session(client) as db:
        stored_family = db.get(Family, archived_id)
        assert stored_family is not None and stored_family.archived_at is not None
        assert db.get(User, owner_id) is not None
        assert db.get(User, child_id) is not None
        assert db.scalar(select(ChildProfile).where(ChildProfile.user_id == child_id)) is not None
        assert db.get(FamilyTask, task_id) is not None
        assert db.scalar(select(FamilyTaskOccurrence).where(FamilyTaskOccurrence.task_id == task_id)) is not None
        assert db.scalar(select(FamilyInvite).where(FamilyInvite.family_id == archived_id)) is not None
        assert db.scalar(select(FamilyMember).where(FamilyMember.family_id == archived_id, FamilyMember.user_id == owner_id)) is not None
    assert member_ids(client, active_id, owner_token) == {owner_id}
    assert client.get(f"{API}/auth/me", headers=headers(child_token)).json()["family_ids"] == []


def test_archive_only_family_keeps_account_and_blocks_pairing_to_archive(client) -> None:
    owner_token, owner_id = parent(client, "archive-only")
    archived_id = family(client, owner_token, "archive-only")
    child_token, child_id = child(client, archived_id, owner_token, "archive-pairing")
    assert client.delete(f"{API}/families/{archived_id}/members/{child_id}", headers=headers(owner_token)).status_code == 200
    assert client.post(f"{API}/families/{archived_id}/archive", headers=headers(owner_token)).status_code == 200
    assert client.get(f"{API}/auth/me", headers=headers(owner_token)).json()["family_ids"] == []
    assert client.get(f"{API}/families/me", headers=headers(owner_token)).json()["data"] == []
    code = client.post(f"{API}/pairing/generate-code", headers=headers(child_token)).json()["data"]["code"]
    attach = client.post(f"{API}/pairing/attach-child", headers=headers(owner_token), json={"family_id": archived_id, "code": code})
    assert attach.status_code == 404
    with db_session(client) as db:
        assert db.get(User, owner_id) is not None
        assert db.get(User, child_id) is not None
