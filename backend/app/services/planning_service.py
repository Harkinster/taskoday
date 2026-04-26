from datetime import date
from typing import List, Optional, Set, Tuple

from sqlalchemy import and_, or_, select
from sqlalchemy.orm import Session

from app.models.enums import DayPart, RoutineFrequency
from app.models.mission import Mission
from app.models.quest import Quest
from app.models.routine import Routine
from app.models.task_completion import TaskCompletion
from app.schemas.planning import (
    PlanningItemResponse,
    PlanningItemType,
    PlanningResponse,
    PlanningSectionResponse,
)

ITEM_ENTITY = Tuple[object, int, int]

DAY_PART_ORDER: List[DayPart] = [
    DayPart.MATIN,
    DayPart.MATINEE,
    DayPart.MIDI,
    DayPart.APRES_MIDI,
    DayPart.SOIR,
    DayPart.SOIREE,
]


def _parse_weekdays(days_of_week: Optional[str]) -> Set[int]:
    if not days_of_week:
        return set()
    values = set()
    for token in days_of_week.split(","):
        token = token.strip()
        if not token:
            continue
        try:
            day_val = int(token)
        except ValueError:
            continue
        if 1 <= day_val <= 7:
            values.add(day_val)
    return values


def is_routine_scheduled_for_date(routine: Routine, target_date: date) -> bool:
    if not routine.is_active:
        return False
    if routine.frequency == RoutineFrequency.DAILY:
        return True
    weekdays = _parse_weekdays(routine.days_of_week)
    return target_date.isoweekday() in weekdays


def fetch_item_entity(db: Session, item_type: PlanningItemType, item_id: int) -> Optional[ITEM_ENTITY]:
    if item_type == PlanningItemType.ROUTINE:
        entity = db.scalar(select(Routine).where(Routine.id == item_id))
        if entity is None:
            return None
        return entity, entity.child_profile_id, entity.points_reward

    if item_type == PlanningItemType.MISSION:
        entity = db.scalar(select(Mission).where(Mission.id == item_id))
        if entity is None:
            return None
        return entity, entity.child_profile_id, entity.points_reward

    entity = db.scalar(select(Quest).where(Quest.id == item_id))
    if entity is None:
        return None
    return entity, entity.child_profile_id, entity.points_reward


def get_completion_for_item(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    target_date: date,
) -> Optional[TaskCompletion]:
    base_filters = [
        TaskCompletion.child_profile_id == child_id,
        TaskCompletion.completion_date == target_date,
    ]
    if item_type == PlanningItemType.ROUTINE:
        base_filters.append(TaskCompletion.routine_id == item_id)
    elif item_type == PlanningItemType.MISSION:
        base_filters.append(TaskCompletion.mission_id == item_id)
    else:
        base_filters.append(TaskCompletion.quest_id == item_id)

    stmt = select(TaskCompletion).where(and_(*base_filters)).limit(1)
    return db.scalar(stmt)


def create_completion_if_missing(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    target_date: date,
    points_awarded: int,
) -> Tuple[TaskCompletion, bool]:
    existing = get_completion_for_item(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        target_date=target_date,
    )
    if existing is not None:
        return existing, False

    completion = TaskCompletion(
        child_profile_id=child_id,
        routine_id=item_id if item_type == PlanningItemType.ROUTINE else None,
        mission_id=item_id if item_type == PlanningItemType.MISSION else None,
        quest_id=item_id if item_type == PlanningItemType.QUEST else None,
        completion_date=target_date,
        points_awarded=points_awarded,
    )
    db.add(completion)
    db.flush()
    return completion, True


def delete_completion_if_exists(
    db: Session,
    *,
    child_id: int,
    item_type: PlanningItemType,
    item_id: int,
    target_date: date,
) -> bool:
    existing = get_completion_for_item(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        target_date=target_date,
    )
    if existing is None:
        return False
    db.delete(existing)
    return True


def build_child_planning_for_date(
    db: Session,
    *,
    child_id: int,
    target_date: date,
) -> PlanningResponse:
    routines = db.scalars(select(Routine).where(Routine.child_profile_id == child_id, Routine.is_active.is_(True))).all()
    routines_for_day = [routine for routine in routines if is_routine_scheduled_for_date(routine, target_date)]

    missions = db.scalars(
        select(Mission).where(
            Mission.child_profile_id == child_id,
            Mission.is_active.is_(True),
            Mission.scheduled_date == target_date,
        ),
    ).all()

    quests = db.scalars(
        select(Quest).where(
            Quest.child_profile_id == child_id,
            or_(
                Quest.is_active.is_(True),
                Quest.scheduled_date == target_date,
            ),
        ),
    ).all()

    completions = db.scalars(
        select(TaskCompletion).where(
            TaskCompletion.child_profile_id == child_id,
            TaskCompletion.completion_date == target_date,
        ),
    ).all()

    completed_routine_ids = {completion.routine_id for completion in completions if completion.routine_id is not None}
    completed_mission_ids = {completion.mission_id for completion in completions if completion.mission_id is not None}
    completed_quest_ids = {completion.quest_id for completion in completions if completion.quest_id is not None}

    sections_map = {day_part: [] for day_part in DAY_PART_ORDER}

    for routine in routines_for_day:
        sections_map[routine.day_part].append(
            PlanningItemResponse(
                item_type=PlanningItemType.ROUTINE,
                item_id=routine.id,
                title=routine.title,
                description=routine.description,
                day_part=routine.day_part,
                is_completed=routine.id in completed_routine_ids,
                points_reward=routine.points_reward,
                is_optional=False,
                frequency=routine.frequency,
                days_of_week=routine.days_of_week,
                scheduled_date=None,
            ),
        )

    for mission in missions:
        sections_map[mission.day_part].append(
            PlanningItemResponse(
                item_type=PlanningItemType.MISSION,
                item_id=mission.id,
                title=mission.title,
                description=mission.description,
                day_part=mission.day_part,
                is_completed=mission.id in completed_mission_ids,
                points_reward=mission.points_reward,
                is_optional=False,
                frequency=None,
                days_of_week=None,
                scheduled_date=mission.scheduled_date,
            ),
        )

    for quest in quests:
        sections_map[quest.day_part].append(
            PlanningItemResponse(
                item_type=PlanningItemType.QUEST,
                item_id=quest.id,
                title=quest.title,
                description=quest.description,
                day_part=quest.day_part,
                is_completed=quest.id in completed_quest_ids,
                points_reward=quest.points_reward,
                is_optional=True,
                frequency=None,
                days_of_week=None,
                scheduled_date=quest.scheduled_date,
            ),
        )

    sections = [
        PlanningSectionResponse(day_part=day_part, items=sections_map.get(day_part, []))
        for day_part in DAY_PART_ORDER
    ]
    return PlanningResponse(child_id=child_id, planning_date=target_date, sections=sections)
