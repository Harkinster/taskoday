from datetime import datetime

from pydantic import BaseModel, Field


class RoutineCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    period: str | None = Field(default=None, max_length=100)
    repeat_type: str = Field(default="daily", pattern="^(daily|weekly|custom)$")
    can_child_delete: bool = False


class RoutineUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    period: str | None = Field(default=None, max_length=100)
    repeat_type: str | None = Field(default=None, pattern="^(daily|weekly|custom)$")
    can_child_delete: bool | None = None
    is_active: bool | None = None


class MissionCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    due_date: datetime | None = None


class MissionUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    due_date: datetime | None = None
    status: str | None = Field(default=None, pattern="^(open|completed)$")


class QuestCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    xp_reward: int = Field(default=25, ge=1, le=10000)


class QuestUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    xp_reward: int | None = Field(default=None, ge=1, le=10000)
    status: str | None = Field(default=None, pattern="^(open|completed)$")
