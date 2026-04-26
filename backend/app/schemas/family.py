from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class FamilyCreateRequest(BaseModel):
    name: str = Field(min_length=2, max_length=120)


class FamilyResponse(BaseModel):
    id: int
    name: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class CurrentFamilyResponse(FamilyResponse):
    children_count: int

