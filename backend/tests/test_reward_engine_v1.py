from datetime import date, timedelta

from tests.test_family_tasks_api import API, _create_task, _headers, _register_child_and_attach, _register_parent, _today


def _post(client, token, occurrence_id, action):
    return client.post(f"{API}/task-occurrences/{occurrence_id}/{action}", headers=_headers(token))


def _rewards(client, token, family_id):
    response = client.get(f"{API}/families/{family_id}/rewards/me", headers=_headers(token))
    assert response.status_code == 200, response.text
    return response.json()["data"]


def _occurrence(client, token, family_id, task, target=None):
    day = target or date.today()
    items = _today(client, token, family_id, day)["items"]
    return next(item for item in items if item["task_id"] == task["id"])


def test_personal_reward_goes_to_assignee_not_parent_actor_and_is_idempotent(client):
    parent, _, family_id = _register_parent(client, "reward-personal")
    child, child_id = _register_child_and_attach(client, parent, "reward-personal")
    task = _create_task(client, parent, family_id, {
        "title": "Mission personnelle", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id],
    })
    occurrence = _occurrence(client, parent, family_id, task)

    # A Parent can complete the child's action, but receives no personal grant.
    completed = _post(client, parent, occurrence["occurrence_id"], "complete")
    assert completed.status_code == 200
    assert completed.json()["data"]["reward_points_awarded_to_me"] == 0
    assert _post(client, parent, occurrence["occurrence_id"], "complete").status_code == 200
    assert _rewards(client, parent, family_id)["active_points"] == 0
    child_rewards = _rewards(client, child, family_id)
    assert child_rewards["active_points"] == 30
    assert len(child_rewards["grants"]) == 1
    assert child_rewards["grants"][0]["kind"] == "MISSION"
    assert child_rewards["grants"][0]["policy_version"] == 2
    assert (child_rewards["flames"], child_rewards["crystals"]) == (3, 4)
    assert child_rewards["grants"][0]["policy_version"] == 2
    assert child_rewards["grants"][0]["mission_bonus_crystals"] in (0, 2)


def test_personal_reward_waits_for_parent_validation(client):
    parent, _, family_id = _register_parent(client, "reward-personal-validate")
    child, child_id = _register_child_and_attach(client, parent, "reward-personal-validate")
    task = _create_task(client, parent, family_id, {
        "title": "Mission validÃ©e", "scope": "PERSONAL", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id], "validation_required": True,
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
    pending = _post(client, child, occurrence_id, "complete")
    assert pending.status_code == 200 and pending.json()["data"]["status"] == "PENDING_VALIDATION"
    pending_rewards = _rewards(client, child, family_id)
    assert (pending_rewards["taskoday_points"], pending_rewards["flames"], pending_rewards["crystals"]) == (0, 0, 0)
    assert _post(client, parent, occurrence_id, "validate").status_code == 200
    child_rewards = _rewards(client, child, family_id)
    assert (child_rewards["taskoday_points"], child_rewards["flames"]) == (30, 3)
    assert child_rewards["crystals"] in (4, 6)
    parent_rewards = _rewards(client, parent, family_id)
    assert (parent_rewards["taskoday_points"], parent_rewards["flames"], parent_rewards["crystals"]) == (0, 0, 0)


def test_house_rewards_all_cycle_contributors_only_after_parent_validation(client):
    parent, parent_id, family_id = _register_parent(client, "reward-house")
    child_a, child_a_id = _register_child_and_attach(client, parent, "reward-house-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "reward-house-b")
    task = _create_task(client, parent, family_id, {
        "title": "Garage", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(), "validation_required": True,
        "assignee_user_ids": [child_a_id, child_b_id],
    })
    occurrence = _occurrence(client, parent, family_id, task)
    occurrence_id = occurrence["occurrence_id"]
    assert _post(client, child_a, occurrence_id, "start").status_code == 200
    assert _post(client, child_b, occurrence_id, "join").status_code == 200
    completed = _post(client, child_b, occurrence_id, "complete")
    assert completed.status_code == 200
    assert completed.json()["data"]["status"] == "PENDING_VALIDATION"
    for token in (child_a, child_b):
        pending_rewards = _rewards(client, token, family_id)
        assert (pending_rewards["taskoday_points"], pending_rewards["flames"], pending_rewards["crystals"]) == (0, 0, 0)

    validated = _post(client, parent, occurrence_id, "validate")
    assert validated.status_code == 200
    assert validated.json()["data"]["reward_points_awarded_to_me"] == 0
    assert _post(client, parent, occurrence_id, "validate").status_code == 200
    for token in (child_a, child_b):
        awarded = _rewards(client, token, family_id)
        assert (awarded["taskoday_points"], awarded["flames"]) == (30, 3)
        assert awarded["crystals"] in (4, 6)
    assert len(_rewards(client, child_a, family_id)["grants"]) == 1
    assert len(_rewards(client, child_b, family_id)["grants"]) == 1
    parent_rewards = _rewards(client, parent, family_id)
    assert (parent_rewards["taskoday_points"], parent_rewards["flames"], parent_rewards["crystals"]) == (0, 0, 0)
    events = client.get(f"{API}/families/{family_id}/task-events", headers=_headers(parent)).json()["data"]["items"]
    assert {event["event_type"]: event["actor_user_id"] for event in events if event["occurrence_id"] == occurrence_id}["VALIDATE"] == parent_id


def test_house_rewards_parent_contributor_not_assignees(client):
    parent, parent_id, family_id = _register_parent(client, "reward-house-parent-contributor")
    child_a, child_a_id = _register_child_and_attach(client, parent, "reward-house-parent-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "reward-house-parent-b")
    task = _create_task(client, parent, family_id, {
        "title": "Garage termine par Parent", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_a_id, child_b_id],
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]

    assert _post(client, parent, occurrence_id, "start").status_code == 200
    completed = _post(client, parent, occurrence_id, "complete")
    assert completed.status_code == 200
    assert completed.json()["data"]["reward_points_awarded_to_me"] == 30
    assert _rewards(client, parent, family_id)["active_points"] == 30
    assert _rewards(client, child_a, family_id)["active_points"] == 0
    assert _rewards(client, child_b, family_id)["active_points"] == 0
    parent_rewards = _rewards(client, parent, family_id)
    assert parent_rewards["user_id"] == parent_id
    grant = parent_rewards["grants"][0]
    assert grant["cycle_number"] == 1


def test_reopen_revokes_prior_cycle_and_second_cycle_gets_new_grant(client):
    parent, parent_id, family_id = _register_parent(client, "reward-reopen")
    task = _create_task(client, parent, family_id, {
        "title": "QuÃªte Maison", "scope": "HOUSE", "kind": "QUEST", "due_date": date.today().isoformat(),
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
    assert _post(client, parent, occurrence_id, "start").status_code == 200
    first_complete = _post(client, parent, occurrence_id, "complete")
    assert first_complete.status_code == 200
    assert first_complete.json()["data"]["reward_points_awarded_to_me"] == 25
    first_bundle = _rewards(client, parent, family_id)
    assert (first_bundle["taskoday_points"], first_bundle["flames"], first_bundle["crystals"]) == (25, 2, 3)

    assert _post(client, parent, occurrence_id, "reopen").status_code == 200
    after_reopen = _rewards(client, parent, family_id)
    assert (after_reopen["taskoday_points"], after_reopen["flames"], after_reopen["crystals"]) == (0, 0, 0)
    assert len(after_reopen["grants"]) == 1 and after_reopen["grants"][0]["revoked_at"] is not None
    assert (after_reopen["grants"][0]["points"], after_reopen["grants"][0]["flames"], after_reopen["grants"][0]["crystals"]) == (25, 2, 3)

    assert _post(client, parent, occurrence_id, "start").json()["data"]["cycle_number"] == 2
    second_complete = _post(client, parent, occurrence_id, "complete")
    assert second_complete.status_code == 200
    current = _rewards(client, parent, family_id)
    assert (current["taskoday_points"], current["flames"], current["crystals"]) == (25, 2, 3)
    assert len(current["grants"]) == 2
    assert {grant["cycle_number"] for grant in current["grants"]} == {1, 2}
    assert len([grant for grant in current["grants"] if grant["revoked_at"] is None]) == 1
    assert next(grant for grant in current["grants"] if grant["cycle_number"] == 2)["crystals"] == 3


def test_failed_mission_gets_no_reward_and_policy_values_are_centralized():
    from app.services.reward_engine import REWARD_POLICY

    assert REWARD_POLICY.points_for("ROUTINE") == 10
    assert REWARD_POLICY.amounts_for("ROUTINE") == (10, 1, 1)
    assert REWARD_POLICY.amounts_for("MISSION") == (30, 3, 4)
    assert REWARD_POLICY.amounts_for("QUEST") == (25, 2, 3)
    assert REWARD_POLICY.mission_bonus_chance_percent == 25
    assert REWARD_POLICY.effort_modifier == 1.0


def test_failed_mission_and_reschedule_do_not_create_rewards(client):
    parent, _, family_id = _register_parent(client, "reward-failed")
    yesterday = date.today() - timedelta(days=1)
    task = _create_task(client, parent, family_id, {
        "title": "Mission Ã©chue", "scope": "HOUSE", "kind": "MISSION", "due_date": yesterday.isoformat(),
    })
    occurrence = _occurrence(client, parent, family_id, task, yesterday)
    # Use the official reschedule contract first; it affects schedule only.
    rescheduled = client.post(
        f"{API}/task-occurrences/{occurrence['occurrence_id']}/reschedule",
        headers=_headers(parent), json={"due_date": date.today().isoformat()},
    )
    assert rescheduled.status_code == 200
    empty = _rewards(client, parent, family_id)
    assert (empty["taskoday_points"], empty["flames"], empty["crystals"]) == (0, 0, 0)

    failed_task = _create_task(client, parent, family_id, {
        "title": "Mission Ã  rater", "scope": "HOUSE", "kind": "MISSION", "due_date": yesterday.isoformat(),
    })
    failed_occurrence = _occurrence(client, parent, family_id, failed_task, yesterday)
    failed = _post(client, parent, failed_occurrence["occurrence_id"], "fail")
    assert failed.status_code == 200 and failed.json()["data"]["status"] == "FAILED"
    empty = _rewards(client, parent, family_id)
    assert (empty["taskoday_points"], empty["flames"], empty["crystals"]) == (0, 0, 0)


def test_each_recurring_routine_occurrence_has_its_own_reward(client):
    parent, _, family_id = _register_parent(client, "reward-routine-occurrences")
    child, child_id = _register_child_and_attach(client, parent, "reward-routine-occurrences")
    yesterday = date.today() - timedelta(days=1)
    task = _create_task(client, parent, family_id, {
        "title": "Routine quotidienne", "scope": "PERSONAL", "kind": "ROUTINE",
        "due_date": yesterday.isoformat(), "recurrence": "DAILY", "assignee_user_ids": [child_id],
    })
    for day in (yesterday, date.today()):
        occurrence = _occurrence(client, parent, family_id, task, day)
        complete = _post(client, child, occurrence["occurrence_id"], "complete")
        assert complete.status_code == 200
        assert complete.json()["data"]["reward_points_awarded_to_me"] == 10
    summary = _rewards(client, child, family_id)
    assert summary["active_points"] == 20
    assert len(summary["grants"]) == 2
    assert len({grant["occurrence_id"] for grant in summary["grants"]}) == 2


def test_reward_summary_is_private_to_current_member(client):
    parent, _, family_id = _register_parent(client, "reward-private")
    child_a, _ = _register_child_and_attach(client, parent, "reward-private-a")
    child_b, _ = _register_child_and_attach(client, parent, "reward-private-b")
    assert _rewards(client, child_a, family_id)["user_id"] != _rewards(client, child_b, family_id)["user_id"]
    assert client.get(f"{API}/families/{family_id + 1000}/rewards/me", headers=_headers(child_a)).status_code == 404


def test_mission_bonus_is_deterministic_persisted_and_revoked_with_bundle(client, monkeypatch):
    from app.services import reward_engine

    class WinningRoll:
        calls = 0
        def roll_percent(self):
            self.calls += 1
            return 0

    winning_roll = WinningRoll()
    monkeypatch.setattr(reward_engine, "REWARD_RANDOM", winning_roll)
    parent, _, family_id = _register_parent(client, "reward-bonus")
    task = _create_task(client, parent, family_id, {
        "title": "Mission bonus", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(),
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
    assert _post(client, parent, occurrence_id, "start").status_code == 200
    result = _post(client, parent, occurrence_id, "complete")
    bundle = result.json()["data"]["reward_bundle_awarded_to_me"]
    assert bundle == {"taskoday_points": 30, "flames": 3, "crystals": 6, "mission_bonus_crystals": 2}
    summary = _rewards(client, parent, family_id)
    assert (summary["taskoday_points"], summary["flames"], summary["crystals"]) == (30, 3, 6)
    assert len(summary["grants"]) == 1 and summary["grants"][0]["mission_bonus_crystals"] == 2
    # A retry does not call the random source or create another component.
    _post(client, parent, occurrence_id, "complete")
    assert _rewards(client, parent, family_id)["crystals"] == 6
    assert winning_roll.calls == 1
    assert _post(client, parent, occurrence_id, "reopen").status_code == 200
    revoked = _rewards(client, parent, family_id)
    assert (revoked["taskoday_points"], revoked["flames"], revoked["crystals"]) == (0, 0, 0)
    assert revoked["grants"][0]["revoked_at"] is not None

    class LosingRoll:
        def roll_percent(self): return 99

    monkeypatch.setattr(reward_engine, "REWARD_RANDOM", LosingRoll())
    no_bonus_task = _create_task(client, parent, family_id, {
        "title": "Mission sans bonus", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(),
    })
    no_bonus_occurrence = _occurrence(client, parent, family_id, no_bonus_task)["occurrence_id"]
    assert _post(client, parent, no_bonus_occurrence, "start").status_code == 200
    assert _post(client, parent, no_bonus_occurrence, "complete").status_code == 200
    losing_grant = next(g for g in _rewards(client, parent, family_id)["grants"] if g["occurrence_id"] == no_bonus_occurrence)
    assert losing_grant["mission_bonus_crystals"] == 0
    assert losing_grant["crystals"] == 4


def test_routine_and_quest_resource_bundles(client):
    parent, _, family_id = _register_parent(client, "reward-resource-values")
    for kind, expected in (("ROUTINE", (10, 1, 1)), ("QUEST", (25, 2, 3))):
        task = _create_task(client, parent, family_id, {
            "title": f"{kind} bundle", "scope": "HOUSE", "kind": kind,
            "due_date": date.today().isoformat(),
            "recurrence": "DAILY" if kind == "ROUTINE" else "NONE",
        })
        occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
        assert _post(client, parent, occurrence_id, "start").status_code == 200
        response = _post(client, parent, occurrence_id, "complete")
        assert response.status_code == 200
        bundle = response.json()["data"]["reward_bundle_awarded_to_me"]
        assert (bundle["taskoday_points"], bundle["flames"], bundle["crystals"]) == expected


def test_house_mission_rolls_bonus_independently_for_each_contributor(client, monkeypatch):
    from app.services import reward_engine

    class SplitRoll:
        rolls = iter((0, 99))
        def roll_percent(self): return next(self.rolls)

    monkeypatch.setattr(reward_engine, "REWARD_RANDOM", SplitRoll())
    parent, _, family_id = _register_parent(client, "reward-multi-roll")
    child_a, child_a_id = _register_child_and_attach(client, parent, "reward-multi-roll-a")
    child_b, child_b_id = _register_child_and_attach(client, parent, "reward-multi-roll-b")
    task = _create_task(client, parent, family_id, {
        "title": "Mission multi roll", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_a_id, child_b_id],
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
    assert _post(client, child_a, occurrence_id, "start").status_code == 200
    assert _post(client, child_b, occurrence_id, "join").status_code == 200
    assert _post(client, child_b, occurrence_id, "complete").status_code == 200
    a = _rewards(client, child_a, family_id)["grants"][0]
    b = _rewards(client, child_b, family_id)["grants"][0]
    assert (a["points"], a["flames"], a["crystals"], a["mission_bonus_crystals"]) == (30, 3, 6, 2)
    assert (b["points"], b["flames"], b["crystals"], b["mission_bonus_crystals"]) == (30, 3, 4, 0)
    assert a["occurrence_id"] == b["occurrence_id"] == occurrence_id
    assert a["cycle_number"] == b["cycle_number"] == 1


def test_start_and_join_do_not_award_resources_before_completion(client):
    parent, _, family_id = _register_parent(client, "reward-no-early-bundle")
    child, child_id = _register_child_and_attach(client, parent, "reward-no-early-bundle")
    task = _create_task(client, parent, family_id, {
        "title": "Start Join sans reward", "scope": "HOUSE", "kind": "MISSION",
        "due_date": date.today().isoformat(), "assignee_user_ids": [child_id],
    })
    occurrence_id = _occurrence(client, parent, family_id, task)["occurrence_id"]
    assert _post(client, child, occurrence_id, "start").status_code == 200
    assert _rewards(client, child, family_id)["grants"] == []
    assert _rewards(client, parent, family_id)["grants"] == []
    assert _post(client, parent, occurrence_id, "join").status_code == 200
    for token in (child, parent):
        summary = _rewards(client, token, family_id)
        assert (summary["taskoday_points"], summary["flames"], summary["crystals"]) == (0, 0, 0)
        assert summary["grants"] == []
    assert _post(client, child, occurrence_id, "complete").status_code == 200
    for token in (child, parent):
        summary = _rewards(client, token, family_id)
        if summary["grants"]:
            assert (summary["taskoday_points"], summary["flames"]) == (30, 3)
            assert summary["crystals"] in (4, 6)
