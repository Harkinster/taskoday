from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, EmailStr, Field

from app.models.enums import UserRole


class ChildCreateRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    display_name: str = Field(min_length=1, max_length=120)
    birth_date: Optional[date] = None


class ChildResponse(BaseModel):
    id: int
    family_id: int
    user_id: Optional[int]
    email: str
    display_name: str
    birth_date: Optional[date]
    role: UserRole
    created_at: datetime
    updated_at: datetime

