from datetime import date

from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator


class RegisterParentRequest(BaseModel):
    # Optional only for the beta02 wire contract. New clients must collect it
    # in their UI; legacy clients never sent this field.
    display_name: str | None = Field(default=None, max_length=120)
    birth_date: date
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    # Transitional compatibility for beta01/beta02 clients. New clients create
    # the account first and call the family endpoints afterwards.
    family_name: str | None = Field(default=None, min_length=1, max_length=255)
    invite_code: str | None = Field(default=None, min_length=16, max_length=256)

    @field_validator("display_name")
    @classmethod
    def validate_display_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("display_name ne peut pas etre vide.")
        return normalized

    @field_validator("birth_date")
    @classmethod
    def validate_birth_date(cls, value: date) -> date:
        if value > date.today():
            raise ValueError("birth_date ne peut pas etre dans le futur.")
        return value

    @field_validator("family_name", "invite_code")
    @classmethod
    def normalize_optional_text(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("La valeur ne peut pas etre vide.")
        return normalized

    @model_validator(mode="after")
    def validate_registration_mode(self) -> "RegisterParentRequest":
        has_legacy_family_flow = self.family_name is not None or self.invite_code is not None
        if self.family_name is not None and self.invite_code is not None:
            raise ValueError("family_name et invite_code ne peuvent pas etre utilises ensemble.")
        if self.display_name is None and not has_legacy_family_flow:
            raise ValueError("display_name est requis hors parcours beta02 legacy.")
        if self.display_name is not None and has_legacy_family_flow:
            raise ValueError("display_name ne peut pas etre combine avec le parcours famille legacy.")
        return self


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

    @field_validator("birth_date")
    @classmethod
    def validate_birth_date(cls, value: date | None) -> date | None:
        if value is not None and value > date.today():
            raise ValueError("birth_date ne peut pas etre dans le futur.")
        return value


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    role: str
    refresh_token: str | None = None
    refresh_expires_in: int | None = None


class RefreshTokenRequest(BaseModel):
    refresh_token: str = Field(min_length=1, max_length=512)


class AuthMeResponse(BaseModel):
    id: int
    email: EmailStr
    role: str
    is_active: bool
    family_ids: list[int]
    display_name: str
    birth_date: date | None = None
