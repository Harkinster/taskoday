from dataclasses import dataclass
import logging

from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.family_task import (
    FamilyTask,
    FamilyTaskAssignee,
    FamilyTaskOccurrence,
    FamilyTaskOccurrenceContributor,
    FamilyTaskOccurrenceEvent,
)
from app.models.reward_grant import RewardGrant


@dataclass(frozen=True)
class RewardPolicy:
    """Provisional effort-value units; these are not a Chronodria economy."""

    routine_base: int = 10
    mission_base: int = 20
    quest_base: int = 30
    quest_bonus: int = 10
    effort_modifier: float = 1.0

    def points_for(self, kind: str) -> int:
        base = {
            "ROUTINE": self.routine_base,
            "MISSION": self.mission_base,
            "QUEST": self.quest_base,
        }[kind]
        if kind == "QUEST":
            base += self.quest_bonus
        return round(base * self.effort_modifier)


REWARD_POLICY = RewardPolicy()
logger = logging.getLogger(__name__)


def grant_for_finalization(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    task: FamilyTask,
    trigger_event: FamilyTaskOccurrenceEvent,
    actor_user_id: int,
) -> int:
    """Create idempotent grants for reliable beneficiaries in this completed cycle.

    Returns only the points awarded to the requesting actor, for a restrained UI
    acknowledgement. Grants for other participants are never exposed by this value.
    """
    if trigger_event.event_type == "COMPLETE" and task.validation_required:
        return 0
    if trigger_event.event_type not in {"COMPLETE", "VALIDATE"}:
        return 0

    if occurrence.scope == "PERSONAL":
        # Personal actions have exactly one validated assignee. Never reward an
        # administrator who completed on somebody else's behalf.
        beneficiaries = list(db.scalars(select(FamilyTaskAssignee.user_id).where(
            FamilyTaskAssignee.task_id == task.id
        )).all())
        if len(beneficiaries) != 1:
            logger.warning(
                "Reward skipped: personal task has no unique beneficiary (task_id=%s occurrence_id=%s)",
                task.id,
                occurrence.id,
            )
            return 0
    else:
        beneficiaries = list(db.scalars(select(FamilyTaskOccurrenceContributor.user_id).where(
            FamilyTaskOccurrenceContributor.occurrence_id == occurrence.id,
            FamilyTaskOccurrenceContributor.cycle_number == trigger_event.cycle_number,
        ).order_by(FamilyTaskOccurrenceContributor.user_id)).all())
        if not beneficiaries:
            logger.warning(
                "Reward skipped: no reliable contributor snapshot (task_id=%s occurrence_id=%s cycle=%s)",
                task.id,
                occurrence.id,
                trigger_event.cycle_number,
            )
            return 0

    points = REWARD_POLICY.points_for(occurrence.kind)
    actor_points = 0
    for beneficiary_id in beneficiaries:
        existing = db.scalar(select(RewardGrant.id).where(
            RewardGrant.occurrence_id == occurrence.id,
            RewardGrant.cycle_number == trigger_event.cycle_number,
            RewardGrant.beneficiary_user_id == beneficiary_id,
        ))
        if existing is not None:
            continue
        try:
            with db.begin_nested():
                grant = RewardGrant(
                    family_id=task.family_id,
                    beneficiary_user_id=beneficiary_id,
                    action_id=task.id,
                    action_title=trigger_event.title,
                    occurrence_id=occurrence.id,
                    cycle_number=trigger_event.cycle_number,
                    scope=occurrence.scope,
                    kind=occurrence.kind,
                    points=points,
                    trigger_event_id=trigger_event.id,
                )
                db.add(grant)
                db.flush()
            if beneficiary_id == actor_user_id:
                actor_points += points
        except IntegrityError:
            # Concurrent duplicate finalization: the unique key is authoritative.
            continue
    return actor_points


def revoke_cycle_rewards(
    db: Session,
    *,
    occurrence: FamilyTaskOccurrence,
    cycle_number: int,
    reopen_event: FamilyTaskOccurrenceEvent,
) -> None:
    db.execute(update(RewardGrant).where(
        RewardGrant.occurrence_id == occurrence.id,
        RewardGrant.cycle_number == cycle_number,
        RewardGrant.revoked_at.is_(None),
    ).values(revoked_at=reopen_event.occurred_at, revoke_event_id=reopen_event.id))


def reward_summary(db: Session, *, family_id: int, user_id: int, limit: int = 20) -> dict:
    total = db.scalar(select(func.coalesce(func.sum(RewardGrant.points), 0)).where(
        RewardGrant.family_id == family_id,
        RewardGrant.beneficiary_user_id == user_id,
        RewardGrant.revoked_at.is_(None),
    )) or 0
    grants = db.scalars(select(RewardGrant).where(
        RewardGrant.family_id == family_id,
        RewardGrant.beneficiary_user_id == user_id,
    ).order_by(RewardGrant.created_at.desc(), RewardGrant.id.desc()).limit(limit)).all()
    return {"family_id": family_id, "user_id": user_id, "active_points": int(total), "grants": grants}
