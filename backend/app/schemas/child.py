from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


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
