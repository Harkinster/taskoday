from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import DayPart, PlanningItemType, RoutineFrequency


class RoutineBase(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: Optional[str] = None
    day_part: DayPart
    frequency: RoutineFrequency = RoutineFrequency.DAILY
    # CSV ISO weekdays (1..7), required only when frequency = SELECTED_DAYS.
    days_of_week: Optional[str] = None
    points_reward: int = Field(default=1, ge=0, le=100)
    is_active: bool = True


class RoutineCreate(RoutineBase):
    child_profile_id: int


class RoutineResponse(RoutineBase):
    id: int
    child_profile_id: int
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class MissionBase(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: Optional[str] = None
    day_part: DayPart
    scheduled_date: date
    points_reward: int = Field(default=2, ge=0, le=100)
    is_active: bool = True


class MissionCreate(MissionBase):
    child_profile_id: int


class MissionResponse(MissionBase):
    id: int
    child_profile_id: int
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class QuestBase(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: Optional[str] = None
    day_part: DayPart
    scheduled_date: Optional[date] = None
    points_reward: int = Field(default=3, ge=0, le=100)
    is_active: bool = True


class QuestCreate(QuestBase):
    child_profile_id: int


class QuestResponse(QuestBase):
    id: int
    child_profile_id: int
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TaskCompletionBase(BaseModel):
    child_profile_id: int
    routine_id: Optional[int] = None
    mission_id: Optional[int] = None
    quest_id: Optional[int] = None
    completion_date: date
    points_awarded: int = Field(default=0, ge=0, le=100)


class TaskCompletionCreate(TaskCompletionBase):
    pass


class TaskCompletionResponse(TaskCompletionBase):
    id: int
    completed_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ChildRoutineCreateRequest(RoutineBase):
    pass


class ChildMissionCreateRequest(MissionBase):
    pass


class ChildQuestCreateRequest(QuestBase):
    pass


class PlanningItemResponse(BaseModel):
    item_type: PlanningItemType
    item_id: int
    title: str
    description: Optional[str]
    day_part: DayPart
    is_completed: bool
    points_reward: int
    is_optional: bool
    frequency: Optional[RoutineFrequency] = None
    days_of_week: Optional[str] = None
    scheduled_date: Optional[date] = None


class PlanningSectionResponse(BaseModel):
    day_part: DayPart
    items: List[PlanningItemResponse]


class PlanningResponse(BaseModel):
    child_id: int
    planning_date: date
    sections: List[PlanningSectionResponse]


class CompletionToggleResponse(BaseModel):
    item_type: PlanningItemType
    item_id: int
    child_id: int
    completion_date: date
    completed: bool
