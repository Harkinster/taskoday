from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session
from typing_extensions import Annotated

from app.api.deps.auth import require_roles
from app.db.session import get_db
from app.models.child_profile import ChildProfile
from app.models.enums import RoutineFrequency, UserRole
from app.models.mission import Mission
from app.models.quest import Quest
from app.models.routine import Routine
from app.models.user import User
from app.schemas.planning import (
    ChildMissionCreateRequest,
    ChildQuestCreateRequest,
    ChildRoutineCreateRequest,
    CompletionToggleResponse,
    MissionResponse,
    PlanningItemType,
    PlanningResponse,
    QuestResponse,
    RoutineResponse,
)
from app.services.child_service import get_child_by_user_id
from app.services.family_service import get_parent_family
from app.services.planning_service import (
    build_child_planning_for_date,
    create_completion_if_missing,
    delete_completion_if_exists,
    fetch_item_entity,
)
from app.services.points_service import create_award_if_missing, create_revoke_if_missing, points_for_item_type

router = APIRouter(tags=["Planning"])


def _resolve_accessible_child(
    db: Session,
    *,
    current_user: User,
    child_id: int,
) -> ChildProfile:
    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Family not found for this parent",
            )
        child = db.scalar(
            select(ChildProfile).where(
                ChildProfile.id == child_id,
                ChildProfile.family_id == family.id,
            ),
        )
        if child is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Child not found",
            )
        return child

    own_child = get_child_by_user_id(db, current_user.id)
    if own_child is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Child profile not found",
        )
    if own_child[0].id != child_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Children can only access their own profile",
        )
    return own_child[0]


def _ensure_user_can_access_item_child(
    db: Session,
    *,
    current_user: User,
    item_child_id: int,
) -> None:
    if current_user.role == UserRole.PARENT:
        family = get_parent_family(db, current_user.id)
        if family is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Family not found for this parent",
            )
        child = db.scalar(
            select(ChildProfile.id).where(
                ChildProfile.id == item_child_id,
                ChildProfile.family_id == family.id,
            ),
        )
        if child is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Item not found",
            )
        return

    own_child = get_child_by_user_id(db, current_user.id)
    if own_child is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Child profile not found",
        )
    if own_child[0].id != item_child_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Children can only access their own planning items",
        )


@router.get("/children/{child_id}/planning", response_model=PlanningResponse)
def get_child_planning(
    child_id: int,
    planning_date: Annotated[date, Query(alias="date")],
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
) -> PlanningResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    return build_child_planning_for_date(db, child_id=child_id, target_date=planning_date)


@router.post("/children/{child_id}/routines", status_code=status.HTTP_201_CREATED, response_model=RoutineResponse)
def create_child_routine(
    child_id: int,
    payload: ChildRoutineCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> RoutineResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    if payload.frequency == RoutineFrequency.SELECTED_DAYS and not payload.days_of_week:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="days_of_week is required for SELECTED_DAYS routines",
        )
    days_of_week = payload.days_of_week if payload.frequency == RoutineFrequency.SELECTED_DAYS else None
    routine = Routine(
        child_profile_id=child_id,
        title=payload.title.strip(),
        description=payload.description,
        day_part=payload.day_part,
        frequency=payload.frequency,
        days_of_week=days_of_week,
        points_reward=1,
        is_active=payload.is_active,
    )
    db.add(routine)
    db.commit()
    db.refresh(routine)
    return RoutineResponse.model_validate(routine)


@router.post("/children/{child_id}/missions", status_code=status.HTTP_201_CREATED, response_model=MissionResponse)
def create_child_mission(
    child_id: int,
    payload: ChildMissionCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> MissionResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    mission = Mission(
        child_profile_id=child_id,
        title=payload.title.strip(),
        description=payload.description,
        day_part=payload.day_part,
        scheduled_date=payload.scheduled_date,
        points_reward=2,
        is_active=payload.is_active,
    )
    db.add(mission)
    db.commit()
    db.refresh(mission)
    return MissionResponse.model_validate(mission)


@router.post("/children/{child_id}/quests", status_code=status.HTTP_201_CREATED, response_model=QuestResponse)
def create_child_quest(
    child_id: int,
    payload: ChildQuestCreateRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT))],
) -> QuestResponse:
    _resolve_accessible_child(db, current_user=current_user, child_id=child_id)
    quest = Quest(
        child_profile_id=child_id,
        title=payload.title.strip(),
        description=payload.description,
        day_part=payload.day_part,
        scheduled_date=payload.scheduled_date,
        points_reward=3,
        is_active=payload.is_active,
    )
    db.add(quest)
    db.commit()
    db.refresh(quest)
    return QuestResponse.model_validate(quest)


@router.patch("/planning/{item_type}/{item_id}/complete", response_model=CompletionToggleResponse)
def complete_item(
    item_type: PlanningItemType,
    item_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
    completion_date: Annotated[Optional[date], Query(alias="date")] = None,
) -> CompletionToggleResponse:
    item_data = fetch_item_entity(db, item_type, item_id)
    if item_data is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Planning item not found")

    item_entity, child_id, _points_reward = item_data
    _ensure_user_can_access_item_child(db, current_user=current_user, item_child_id=child_id)

    target_date = completion_date or date.today()
    if item_type == PlanningItemType.MISSION:
        mission = item_entity
        if completion_date is None:
            target_date = mission.scheduled_date
        if target_date != mission.scheduled_date:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Mission can only be completed on its scheduled date",
            )

    points_amount = points_for_item_type(item_type)
    _completion, created = create_completion_if_missing(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        target_date=target_date,
        points_awarded=points_amount,
    )
    if created:
        create_award_if_missing(
            db,
            child_id=child_id,
            item_type=item_type,
            item_id=item_id,
            completion_date=target_date,
            amount=points_amount,
        )
    db.commit()
    return CompletionToggleResponse(
        item_type=item_type,
        item_id=item_id,
        child_id=child_id,
        completion_date=target_date,
        completed=True,
    )


@router.patch("/planning/{item_type}/{item_id}/uncomplete", response_model=CompletionToggleResponse)
def uncomplete_item(
    item_type: PlanningItemType,
    item_id: int,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(require_roles(UserRole.PARENT, UserRole.CHILD))],
    completion_date: Annotated[Optional[date], Query(alias="date")] = None,
) -> CompletionToggleResponse:
    item_data = fetch_item_entity(db, item_type, item_id)
    if item_data is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Planning item not found")

    item_entity, child_id, _points_reward = item_data
    _ensure_user_can_access_item_child(db, current_user=current_user, item_child_id=child_id)

    target_date = completion_date or date.today()
    if item_type == PlanningItemType.MISSION:
        mission = item_entity
        if completion_date is None:
            target_date = mission.scheduled_date
        if target_date != mission.scheduled_date:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Mission can only be uncompleted on its scheduled date",
            )

    deleted = delete_completion_if_exists(
        db,
        child_id=child_id,
        item_type=item_type,
        item_id=item_id,
        target_date=target_date,
    )
    if deleted:
        create_revoke_if_missing(
            db,
            child_id=child_id,
            item_type=item_type,
            item_id=item_id,
            completion_date=target_date,
            amount=points_for_item_type(item_type),
        )
        db.commit()
    return CompletionToggleResponse(
        item_type=item_type,
        item_id=item_id,
        child_id=child_id,
        completion_date=target_date,
        completed=False,
    )
