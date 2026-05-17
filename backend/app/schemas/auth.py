from datetime import date

from pydantic import BaseModel, EmailStr, Field, field_validator


class RegisterParentRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    family_name: str = Field(min_length=1, max_length=255)
    birth_date: date

    @field_validator("family_name")
    @classmethod
    def validate_family_name(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("family_name ne peut pas etre vide.")
        return normalized


class RegisterChildRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    display_name: str = Field(min_length=1, max_length=120)
    birth_date: date | None = None

    @field_validator("display_name")
    @classmethod
    def validate_display_name(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("display_name ne peut pas etre vide.")
        return normalized


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    role: str


class AuthMeResponse(BaseModel):
    id: int
    email: EmailStr
    role: str
    is_active: bool
    family_ids: list[int]
