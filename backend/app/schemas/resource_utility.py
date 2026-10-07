from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, field_validator


class ResourceBalanceResponse(BaseModel):
    family_id: int
    user_id: int
    taskoday_points: int
    flames: int
    crystals: int


class WishOfferCreate(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=500)
    flame_cost: int = Field(ge=1)

    @field_validator("title")
    @classmethod
    def nonblank_title(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Le titre du souhait est requis.")
        return value


class WishOfferUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=500)
    flame_cost: int | None = Field(default=None, ge=1)
    active: bool | None = None


class WishOfferResponse(BaseModel):
    id: int
    family_id: int
    title: str
    description: str | None
    flame_cost: int
    active: bool

    model_config = {"from_attributes": True}


class WishOfferListResponse(BaseModel):
    items: list[WishOfferResponse]


class WishRequestCreate(BaseModel):
    offer_id: int


class WishRequestResponse(BaseModel):
    id: int
    offer_id: int
    user_id: int
    requester_name: str
    title: str
    flame_cost: int
    status: Literal["PENDING", "APPROVED", "REJECTED", "CANCELLED"]
    requested_at: datetime
    decided_by: int | None
    decided_at: datetime | None


class WishRequestListResponse(BaseModel):
    items: list[WishRequestResponse]


class WishObtainRequest(BaseModel):
    offer_id: int
    idempotency_key: str = Field(min_length=8, max_length=128)


class ChestOpenRequest(BaseModel):
    chest_type: Literal["COMMON", "RARE", "EPIC"]
    idempotency_key: str = Field(min_length=8, max_length=128)


class ChestDropResponse(BaseModel):
    collectible_key: str
    title: str
    quantity: int


class ChestOpenResponse(BaseModel):
    id: int
    chest_type: Literal["COMMON", "RARE", "EPIC"]
    crystal_cost: int
    created_at: datetime
    drops: list[ChestDropResponse]


class ChestOpenListResponse(BaseModel):
    items: list[ChestOpenResponse]


class ChestPolicyEntry(BaseModel):
    chest_type: Literal["COMMON", "RARE", "EPIC"]
    title: str
    crystal_cost: int
    drop_count: int


class ChestCatalogResponse(BaseModel):
    family_id: int
    user_id: int
    crystals: int
    chests: list[ChestPolicyEntry]


class CollectionItemResponse(BaseModel):
    collectible_key: str
    title: str
    quantity: int


class CollectionResponse(BaseModel):
    family_id: int
    user_id: int
    items: list[CollectionItemResponse]
