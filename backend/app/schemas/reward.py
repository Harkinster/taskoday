from datetime import datetime

from pydantic import BaseModel, Field, field_validator

from app.models.reward import RewardRequestStatus, ScaleTransactionType, ScrollStatus


class RewardCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str | None = None
    cost_scales: int = Field(ge=0, le=100000)
    emoji: str = Field(default="gift", max_length=16)
    is_active: bool = True

    @field_validator("title", "emoji")
    @classmethod
    def strip_required_strings(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("ne peut pas etre vide.")
        return normalized


class RewardUpdateRequest(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=255)
    description: str | None = None
    cost_scales: int | None = Field(default=None, ge=0, le=100000)
    emoji: str | None = Field(default=None, max_length=16)
    is_active: bool | None = None


class RewardRequestCreate(BaseModel):
    note: str | None = Field(default=None, max_length=1000)


class RewardRequestDecision(BaseModel):
    status: RewardRequestStatus
    parent_note: str | None = Field(default=None, max_length=1000)
    expires_at: datetime | None = None


class ExternalRewardResponse(BaseModel):
    id: int
    child_id: int
    title: str
    description: str | None
    cost_scales: int
    emoji: str
    is_active: bool
    created_by_user_id: int
    created_at: datetime
    updated_at: datetime


class RewardCouponResponse(BaseModel):
    id: int
    request_id: int
    child_id: int
    reward_id: int | None
    code: str
    status: ScrollStatus
    created_at: datetime
    used_at: datetime | None


class RewardRequestResponse(BaseModel):
    id: int
    child_id: int
    reward_id: int | None
    reward_title: str
    cost_scales: int
    status: RewardRequestStatus
    requested_by_user_id: int
    decided_by_user_id: int | None
    requested_at: datetime
    decided_at: datetime | None
    expires_at: datetime | None
    note: str | None
    parent_note: str | None
    coupon: RewardCouponResponse | None = None


class ScaleBalanceResponse(BaseModel):
    child_id: int
    balance: int


class ScaleTransactionResponse(BaseModel):
    id: int
    child_id: int
    amount: int
    reason: str
    source_type: str
    source_id: int | None
    transaction_type: ScaleTransactionType
    event_key: str
    created_at: datetime
