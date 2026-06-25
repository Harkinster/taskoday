from datetime import date, datetime

from pydantic import BaseModel, EmailStr, Field, field_validator


class ChildOut(BaseModel):
    id: int
    email: EmailStr
    display_name: str
    avatar_url: str | None
    xp: int
    level: int
    created_at: datetime


class ChildUpdateRequest(BaseModel):
    display_name: str | None = Field(default=None, min_length=1, max_length=120)
    avatar_url: str | None = Field(default=None, max_length=500)


class ChildCreateRequest(BaseModel):
    display_name: str = Field(min_length=1, max_length=120)
    email: EmailStr | None = None
    birth_date: date | None = None

    @field_validator("display_name")
    @classmethod
    def validate_display_name(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("display_name ne peut pas etre vide.")
        return normalized
