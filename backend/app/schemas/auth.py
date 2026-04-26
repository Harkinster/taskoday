from __future__ import annotations

from typing import List

from pydantic import BaseModel, ConfigDict, EmailStr, Field

from app.models.enums import UserRole


class RegisterParentRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    family_name: str = Field(min_length=2, max_length=120)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    role: UserRole


class MeResponse(BaseModel):
    id: int
    email: str
    role: UserRole
    is_active: bool
    family_ids: List[int]

    model_config = ConfigDict(from_attributes=True)
