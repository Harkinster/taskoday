from datetime import datetime

from pydantic import BaseModel, Field, field_validator


class FamilyTaskCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    category: str | None = Field(default=None, max_length=100)
    priority: str = Field(default="NORMAL", pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    recurrence: str = Field(default="NONE", pattern="^(NONE|DAILY|WEEKLY|SELECTED_WEEKDAYS)$")
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


class FamilyTaskUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    category: str | None = Field(default=None, max_length=100)
    priority: str | None = Field(default=None, pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    recurrence: str | None = Field(default=None, pattern="^(NONE|DAILY|WEEKLY|SELECTED_WEEKDAYS)$")
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
