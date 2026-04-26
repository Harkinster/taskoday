from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class RewardCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: Optional[str] = None
    cost_points: int = Field(ge=1, le=10000)
    is_active: bool = True


class RewardUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=120)
    description: Optional[str] = None
    cost_points: Optional[int] = Field(default=None, ge=1, le=10000)
    is_active: Optional[bool] = None


class RewardResponse(BaseModel):
    id: int
    child_profile_id: int
    title: str
    description: Optional[str]
    cost_points: int
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class RewardsResponse(BaseModel):
    child_id: int
    rewards: List[RewardResponse]


class RewardPurchaseResponse(BaseModel):
    id: int
    child_profile_id: int
    reward_id: Optional[int]
    reward_title: str
    cost_points: int
    purchased_at: datetime

    model_config = ConfigDict(from_attributes=True)


class RewardPurchaseResult(BaseModel):
    purchase: RewardPurchaseResponse
    balance: int


class RewardPurchasesResponse(BaseModel):
    child_id: int
    balance: int
    purchases: List[RewardPurchaseResponse]
