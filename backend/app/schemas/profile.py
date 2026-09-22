from datetime import date

from pydantic import BaseModel, Field, field_validator, model_validator


class ProfileUpdateRequest(BaseModel):
    display_name: str | None = Field(default=None, min_length=1, max_length=120)
    birth_date: date | None = None

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
    def validate_birth_date(cls, value: date | None) -> date | None:
        if value is not None and value > date.today():
            raise ValueError("birth_date ne peut pas etre dans le futur.")
        return value

    @model_validator(mode="after")
    def reject_explicit_null_display_name(self) -> "ProfileUpdateRequest":
        if "display_name" in self.model_fields_set and self.display_name is None:
            raise ValueError("display_name ne peut pas etre vide.")
        return self
