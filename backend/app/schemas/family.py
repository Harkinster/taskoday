from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class FamilyCreateRequest(BaseModel):
    name: str = Field(min_length=1, max_length=255)


class FamilyOut(BaseModel):
    id: int
    name: str
    created_by_user_id: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class PairingAttachRequest(BaseModel):
    code: str = Field(min_length=4, max_length=12)
    family_id: int | None = None
