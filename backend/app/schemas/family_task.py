from datetime import date, datetime, time

from pydantic import BaseModel, Field, field_validator, model_validator


class FamilyTaskCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    category: str | None = Field(default=None, max_length=100)
    priority: str = Field(default="NORMAL", pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    due_date: date | None = None
    due_time: time | None = None
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
    priority: str | None = Field(default=None, pattern="^(LOW|NORMAL|HIGH|URGENT)$")
    due_at: datetime | None = None
    due_date: date | None = None
    due_time: time | None = None
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

    @model_validator(mode="after")
    def validate_due_fields(self) -> "FamilyTaskUpdateRequest":
        if self.due_at is not None and (self.due_date is not None or self.due_time is not None):
            raise ValueError("due_at ne peut pas etre combine avec due_date/due_time.")
        return self
