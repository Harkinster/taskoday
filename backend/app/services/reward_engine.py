from dataclasses import dataclass
import logging
import secrets

from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.family_task import FamilyTask, FamilyTaskAssignee, FamilyTaskOccurrence, FamilyTaskOccurrenceContributor, FamilyTaskOccurrenceEvent
from app.models.reward_grant import RewardGrant, RewardResourceGrant


class RewardRandomSource:
    """Injectable source for the per-beneficiary Mission bonus roll."""
    def roll_percent(self) -> int:
        return secrets.randbelow(100)


@dataclass(frozen=True)
class RewardPolicy:
    """Provisional economy values live here; not the final Chronodria economy."""
    version: int = 2
    routine_points: int = 10
    routine_flames: int = 1
    routine_crystals: int = 1
    mission_points: int = 30
    mission_flames: int = 3
    mission_crystals: int = 4
    quest_points: int = 25
    quest_flames: int = 2
    quest_crystals: int = 3
    mission_bonus_chance_percent: int = 25
    mission_bonus_crystals: int = 2
    effort_modifier: float = 1.0

    def amounts_for(self, kind: str) -> tuple[int, int, int]:
        values = {
            "ROUTINE": (self.routine_points, self.routine_flames, self.routine_crystals),
            "MISSION": (self.mission_points, self.mission_flames, self.mission_crystals),
            "QUEST": (self.quest_points, self.quest_flames, self.quest_crystals),
        }[kind]
        return (round(values[0] * self.effort_modifier), values[1], values[2])

    def points_for(self, kind: str) -> int:
        return self.amounts_for(kind)[0]


REWARD_POLICY = RewardPolicy()
REWARD_RANDOM = RewardRandomSource()
logger = logging.getLogger(__name__)


def grant_for_finalization(db: Session, *, occurrence: FamilyTaskOccurrence, task: FamilyTask,
                           trigger_event: FamilyTaskOccurrenceEvent, actor_user_id: int,
                           random_source: RewardRandomSource | None = None) -> int:
    """Atomically create an idempotent points + resource bundle for each beneficiary."""
    if trigger_event.event_type == "COMPLETE" and task.validation_required:
        return 0
    if trigger_event.event_type not in {"COMPLETE", "VALIDATE"}:
        return 0
    if occurrence.scope == "PERSONAL":
        beneficiaries = list(db.scalars(select(FamilyTaskAssignee.user_id).where(FamilyTaskAssignee.task_id == task.id)).all())
        if len(beneficiaries) != 1:
            logger.warning("Reward skipped: personal task has no unique beneficiary (task_id=%s occurrence_id=%s)", task.id, occurrence.id)
            return 0
    else:
        beneficiaries = list(db.scalars(select(FamilyTaskOccurrenceContributor.user_id).where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == trigger_event.cycle_number,
        ).order_by(FamilyTaskOccurrenceContributor.user_id)).all())
        if not beneficiaries:
            logger.warning("Reward skipped: no reliable contributor snapshot (task_id=%s occurrence_id=%s cycle=%s)", task.id, occurrence.id, trigger_event.cycle_number)
            return 0

    points, flames, crystals = REWARD_POLICY.amounts_for(occurrence.kind)
    actor_bundle = {"taskoday_points": 0, "flames": 0, "crystals": 0, "mission_bonus_crystals": 0}
    source = random_source or REWARD_RANDOM
    for beneficiary_id in beneficiaries:
        if db.scalar(select(RewardGrant.id).where(
            RewardGrant.occurrence_id == occurrence.id,
            RewardGrant.cycle_number == trigger_event.cycle_number,
            RewardGrant.beneficiary_user_id == beneficiary_id,
        )) is not None:
            continue
        try:
            # One savepoint contains all bundle rows. No partial award can survive.
            with db.begin_nested():
                bonus = 0
                if occurrence.kind == "MISSION" and source.roll_percent() < REWARD_POLICY.mission_bonus_chance_percent:
                    bonus = REWARD_POLICY.mission_bonus_crystals
                grant = RewardGrant(
                    family_id=task.family_id, beneficiary_user_id=beneficiary_id,
                    action_id=task.id, action_title=trigger_event.title,
                    occurrence_id=occurrence.id, cycle_number=trigger_event.cycle_number,
                    scope=occurrence.scope, kind=occurrence.kind, points=points,
                    policy_version=REWARD_POLICY.version, mission_bonus_crystals=bonus,
                    trigger_event_id=trigger_event.id,
                )
                db.add(grant)
                db.flush()
                rows = [
                    RewardResourceGrant(family_id=task.family_id, beneficiary_user_id=beneficiary_id,
                        reward_grant_id=grant.id, action_id=task.id, occurrence_id=occurrence.id,
                        cycle_number=trigger_event.cycle_number, resource_type="FLAME", component="BASE", amount=flames),
                    RewardResourceGrant(family_id=task.family_id, beneficiary_user_id=beneficiary_id,
                        reward_grant_id=grant.id, action_id=task.id, occurrence_id=occurrence.id,
                        cycle_number=trigger_event.cycle_number, resource_type="CRYSTAL", component="BASE", amount=crystals),
                ]
                if bonus:
                    rows.append(RewardResourceGrant(family_id=task.family_id, beneficiary_user_id=beneficiary_id,
                        reward_grant_id=grant.id, action_id=task.id, occurrence_id=occurrence.id,
                        cycle_number=trigger_event.cycle_number, resource_type="CRYSTAL", component="MISSION_BONUS", amount=bonus))
                db.add_all(rows)
                db.flush()
            if beneficiary_id == actor_user_id:
                actor_bundle = {"taskoday_points": points, "flames": flames, "crystals": crystals + bonus,
                                "mission_bonus_crystals": bonus}
        except IntegrityError:
            # Unique point key arbitrates concurrent duplicate finalization.
            continue
    occurrence._reward_bundle_awarded_to_me = actor_bundle
    occurrence._reward_points_awarded_to_me = actor_bundle["taskoday_points"]
    return actor_bundle["taskoday_points"]


def revoke_cycle_rewards(db: Session, *, occurrence: FamilyTaskOccurrence, cycle_number: int,
                         reopen_event: FamilyTaskOccurrenceEvent) -> None:
    values = {"revoked_at": reopen_event.occurred_at, "revoke_event_id": reopen_event.id}
    db.execute(update(RewardGrant).where(RewardGrant.occurrence_id == occurrence.id,
        RewardGrant.cycle_number == cycle_number, RewardGrant.revoked_at.is_(None)).values(**values))
    db.execute(update(RewardResourceGrant).where(RewardResourceGrant.occurrence_id == occurrence.id,
        RewardResourceGrant.cycle_number == cycle_number, RewardResourceGrant.revoked_at.is_(None)).values(**values))


def reward_summary(db: Session, *, family_id: int, user_id: int, limit: int = 20) -> dict:
    points = db.scalar(select(func.coalesce(func.sum(RewardGrant.points), 0)).where(
        RewardGrant.family_id == family_id, RewardGrant.beneficiary_user_id == user_id,
        RewardGrant.revoked_at.is_(None))) or 0
    balances = {row[0]: int(row[1]) for row in db.execute(select(
        RewardResourceGrant.resource_type, func.coalesce(func.sum(RewardResourceGrant.amount), 0)
    ).where(RewardResourceGrant.family_id == family_id,
        RewardResourceGrant.beneficiary_user_id == user_id,
        RewardResourceGrant.revoked_at.is_(None)).group_by(RewardResourceGrant.resource_type)).all()}
    grants = db.scalars(select(RewardGrant).where(RewardGrant.family_id == family_id,
        RewardGrant.beneficiary_user_id == user_id).order_by(RewardGrant.created_at.desc(),
        RewardGrant.id.desc()).limit(limit)).all()
    grant_ids = [grant.id for grant in grants]
    amounts: dict[int, dict] = {grant_id: {"flames": 0, "crystals": 0, "mission_bonus_crystals": 0} for grant_id in grant_ids}
    if grant_ids:
        rows = db.execute(select(RewardResourceGrant.reward_grant_id, RewardResourceGrant.resource_type,
            RewardResourceGrant.component, func.sum(RewardResourceGrant.amount)).where(
            RewardResourceGrant.reward_grant_id.in_(grant_ids)).group_by(
            RewardResourceGrant.reward_grant_id, RewardResourceGrant.resource_type, RewardResourceGrant.component)).all()
        for grant_id, resource_type, component, amount in rows:
            key = "flames" if resource_type == "FLAME" else "crystals"
            amounts[grant_id][key] += int(amount)
            if component == "MISSION_BONUS": amounts[grant_id]["mission_bonus_crystals"] += int(amount)
    for grant in grants:
        grant.resource_bundle = amounts[grant.id]
        grant.resource_bundle["mission_bonus_crystals"] = grant.mission_bonus_crystals
    return {"family_id": family_id, "user_id": user_id, "taskoday_points": int(points),
            "flames": balances.get("FLAME", 0), "crystals": balances.get("CRYSTAL", 0),
            "active_points": int(points), "grants": grants}
