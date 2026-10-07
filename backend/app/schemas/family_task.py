from datetime import date, datetime, time
from typing import Any, Literal
from app.models.family_task import FamilyTaskOccurrenceStatus

from pydantic import BaseModel, Field, field_validator, model_validator


class FamilyTaskCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    category: str | None = Field(default=None, max_length=100)
    scope: Literal["PERSONAL", "HOUSE"] | None = None
    kind: Literal["ROUTINE", "MISSION", "QUEST"] | None = None
    priority: str = Field(default="NORMAL", pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    due_date: date | None = None
    end_date: date | None = None
    due_time: time | None = None
    recurrence: str = Field(default="NONE", pattern="^(NONE|DAILY|WEEKLY|SELECTED_WEEKDAYS)$")
    recurrence_interval: int = Field(default=1, ge=1)
    selected_weekdays: list[int | str] | None = None
    assignee_user_ids: list[int] = Field(default_factory=list)
    validation_required: bool = False
    gamification_enabled: bool = False

    @field_validator("title")
    @classmethod
    def validate_title(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("title ne peut pas etre vide.")
        return normalized

    @field_validator("category")
    @classmethod
    def validate_category(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @model_validator(mode="after")
    def validate_due_fields(self) -> "FamilyTaskCreateRequest":
        if self.due_at is not None and (self.due_date is not None or self.due_time is not None):
            raise ValueError("due_at ne peut pas etre combine avec due_date/due_time.")
        if self.due_time is not None and self.due_date is None and self.due_at is None:
            raise ValueError("due_date est requis quand due_time est fourni.")
        return self


class FamilyTaskUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    category: str | None = Field(default=None, max_length=100)
    scope: Literal["PERSONAL", "HOUSE"] | None = None
    kind: Literal["ROUTINE", "MISSION", "QUEST"] | None = None
    priority: str | None = Field(default=None, pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    due_date: date | None = None
    end_date: date | None = None
    due_time: time | None = None
    recurrence: str | None = Field(default=None, pattern="^(NONE|DAILY|WEEKLY|SELECTED_WEEKDAYS)$")
    recurrence_interval: int | None = Field(default=None, ge=1)
    selected_weekdays: list[int | str] | None = None
    assignee_user_ids: list[int] | None = None
    validation_required: bool | None = None
    gamification_enabled: bool | None = None
    active: bool | None = None

    @field_validator("title")
    @classmethod
    def validate_title(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("title ne peut pas etre vide.")
        return normalized

    @field_validator("category")
    @classmethod
    def validate_category(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @model_validator(mode="after")
    def validate_due_fields(self) -> "FamilyTaskUpdateRequest":
        if self.due_at is not None and (self.due_date is not None or self.due_time is not None):
            raise ValueError("due_at ne peut pas etre combine avec due_date/due_time.")
        return self


FamilyActionCategory = Literal[
    "TASKODAY_HOUSE_QUEST",
    "TASKODAY_PERSONAL_ROUTINE",
    "TASKODAY_PERSONAL_MISSION",
    "TASKODAY_HOUSE_ROUTINE",
    "TASKODAY_HOUSE_MISSION",
]


class FamilyTaskAssigneeResponse(BaseModel):
    user_id: int
    role: str
    email: str
    display_name: str


class FamilyTaskDefinitionResponse(BaseModel):
    id: int
    family_id: int
    title: str
    description: str | None
    creator_user_id: int
    category: str | None
    scope: Literal["PERSONAL", "HOUSE"]
    kind: Literal["ROUTINE", "MISSION", "QUEST"]
    priority: str
    due_at: datetime | None
    due_date: date | None
    due_time: time | None
    has_due_time: bool
    end_date: date | None
    recurrence: str
    recurrence_interval: int
    selected_weekdays: list[int]
    assignees: list[FamilyTaskAssigneeResponse]
    validation_required: bool
    gamification_enabled: bool
    active: bool
    created_at: datetime
    updated_at: datetime


class FamilyTaskActorResponse(BaseModel):
    user_id: int
    display_name: str


class FamilyTaskOccurrenceResponse(BaseModel):
    task_id: int
    occurrence_id: int
    category: FamilyActionCategory
    scope: Literal["PERSONAL", "HOUSE"]
    kind: Literal["ROUTINE", "MISSION", "QUEST"]
    title: str
    description: str | None
    assignees: list[FamilyTaskAssigneeResponse]
    scheduled_date: date | None
    due_at: datetime | None
    due_date: date | None
    end_date: date | None
    has_due_time: bool
    due_time: time | None
    recurrence: str
    recurrence_interval: int
    status: FamilyTaskOccurrenceStatus
    reward_points_awarded_to_me: int | None = None
    reward_bundle_awarded_to_me: dict[str, int] | None = None
    cycle_number: int
    contributors: list[FamilyTaskActorResponse]
    validation_required: bool
    gamification_enabled: bool
    priority: str
    completed_at: datetime | None
    completed_by: int | None
    completed_by_user: FamilyTaskActorResponse | None
    validated_at: datetime | None
    validated_by: int | None
    validated_by_user: FamilyTaskActorResponse | None


class FamilyTaskOccurrenceEventResponse(BaseModel):
    id: int
    family_id: int
    task_id: int
    occurrence_id: int
    category: FamilyActionCategory
    scope: Literal["PERSONAL", "HOUSE"]
    kind: Literal["ROUTINE", "MISSION", "QUEST"]
    title: str
    scheduled_date: date | None
    event_type: str
    cycle_number: int | None
    metadata: dict[str, Any] | None
    contributor_user_ids: list[int]
    contributors: list[FamilyTaskActorResponse]
    status_from: str
    status_to: str
    actor_user_id: int
    actor_user: FamilyTaskActorResponse | None
    completed_by_user_id: int | None
    participant_user_ids: list[int]
    occurred_at: datetime
    legacy_inferred: bool


class FamilyTaskOccurrenceEventsResponse(BaseModel):
    family_id: int
    items: list[FamilyTaskOccurrenceEventResponse]
    limit: int
    offset: int


class FamilyTaskMissionRescheduleRequest(BaseModel):
    due_date: date


class FamilyTaskOccurrenceMemberGroupResponse(BaseModel):
    assignee: FamilyTaskAssigneeResponse | None
    items: list[FamilyTaskOccurrenceResponse]


class FamilyTasksTodayResponse(BaseModel):
    family_id: int
    date: date
    items: list[FamilyTaskOccurrenceResponse]
    by_member: list[FamilyTaskOccurrenceMemberGroupResponse]


class FamilyTaskOccurrencesRangeResponse(BaseModel):
    family_id: int
    start_date: date
    end_date: date
    items: list[FamilyTaskOccurrenceResponse]
