from typing import Any

from pydantic import BaseModel


class ChestCatalogEntryResponse(BaseModel):
    id: str
    name: str
    rarity: str
    crystal_cost: int
    description: str
    possible_rewards: list[str]


class ChestResponse(BaseModel):
    id: int
    child_id: int
    type: str
    name: str
    rarity: str
    crystal_cost: int
    description: str
    possible_rewards: list[str]
    status: str
    source_type: str | None
    source_id: int | None
    created_at: str | None
    opened_at: str | None


class InventoryItemResponse(BaseModel):
    key: str
    title: str
    rarity: str
    category: str
    quantity: int


class LootGainResponse(InventoryItemResponse):
    name: str
    quantity_total: int
    is_duplicate_compensation: bool


class InventoryResponse(BaseModel):
    child_id: int
    currencies: dict[str, int]
    items: list[InventoryItemResponse]
    chests: list[ChestResponse]


class RequiredResourceResponse(BaseModel):
    item_key: str
    title: str
    owned_quantity: int
    required_quantity: int
    is_satisfied: bool


class EggResponse(BaseModel):
    id: int
    child_id: int
    egg_key: str
    title: str
    status: str
    state: str
    current_state: str
    current_state_label: str
    progress_percent: int
    next_state: str | None
    next_state_label: str | None
    required_resources: list[RequiredResourceResponse]
    can_evolve: bool
    asset_key: str
    obtained_at: str | None
    hatched_at: str | None
    requirements: dict[str, int]


class EggsResponse(BaseModel):
    child_id: int
    eggs: list[EggResponse]


class DragonResponse(BaseModel):
    id: int
    child_id: int
    dragon_key: str
    title: str
    stage: str
    progress_percent: int
    active_companion: bool
    asset_key: str
    next_evolution: dict[str, Any] | None


class DuplicateCompensationResponse(BaseModel):
    reason: str
    family_id: str
    item: InventoryItemResponse
    quantity_awarded: int


class CrystalBalanceResponse(BaseModel):
    child_id: int
    balance: int
    currency: str


class ChestCatalogResponse(BaseModel):
    child_id: int
    crystals_balance: int
    chests: list[ChestCatalogEntryResponse]


class OpenCatalogChestResponse(BaseModel):
    chest: ChestResponse
    loot: list[LootGainResponse]
    granted_egg: EggResponse | None
    duplicate_compensation: DuplicateCompensationResponse | None
    crystals_spent: int
    crystals_balance: int
    inventory_after: InventoryResponse
    catalog_chest: ChestCatalogEntryResponse
    progress: dict[str, Any]


class EggEvolutionResponse(BaseModel):
    egg: EggResponse
    hatched: bool
    dragon: DragonResponse | None
    inventory: InventoryResponse


class ActiveCompanionResponse(BaseModel):
    child_id: int
    active_companion: DragonResponse


class StateUnlockResponse(BaseModel):
    state: str
    unlocked: bool


class LegendaryArtifactResponse(BaseModel):
    item_key: str
    required: int
    owned: int


class BestiaryFamilyResponse(BaseModel):
    family_id: str
    family_name: str
    element: str
    discovered: bool
    egg_owned: bool
    dragon_owned: bool
    current_egg_state: str | None
    current_dragon_stage: str | None
    active_companion: bool
    legendary_unlocked: bool
    progress_percent: int
    egg_progress_percent: int | None
    next_egg_state: str | None
    required_resources: list[RequiredResourceResponse]
    can_evolve: bool
    egg: EggResponse | None
    egg_asset_key: str
    dragon_asset_key: str
    egg_states: list[StateUnlockResponse]
    dragon_stages: list[StateUnlockResponse]
    legendary_artifact: LegendaryArtifactResponse


class BestiaryResponse(BaseModel):
    child_id: int
    families: list[BestiaryFamilyResponse]
