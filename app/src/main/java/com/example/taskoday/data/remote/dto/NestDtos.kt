package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NestProgressDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("guardian") val guardian: GuardianProgressDto,
    @SerializedName("wallet") val wallet: NestWalletDto,
    @SerializedName("nest") val nest: NestInfoDto,
    @SerializedName("chest_progress") val chestProgress: ChestProgressDto,
)

data class GuardianProgressDto(
    @SerializedName("xp") val xp: Int,
    @SerializedName("level") val level: Int,
)

data class NestWalletDto(
    @SerializedName("flammeches") val flammeches: Int,
    @SerializedName("crystals") val crystals: Int,
)

data class NestInfoDto(
    @SerializedName("level") val level: Int,
    @SerializedName("name") val name: String,
)

data class ChestProgressDto(
    @SerializedName("points") val points: Int,
    @SerializedName("points_required") val pointsRequired: Int,
    @SerializedName("opened_chests") val openedChests: Int,
    @SerializedName("unopened_chests") val unopenedChests: Int,
)

data class CrystalBalanceDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("balance") val balance: Int,
    @SerializedName("currency") val currency: String,
)

data class InventoryItemDto(
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("rarity") val rarity: String,
    @SerializedName("category") val category: String,
    @SerializedName("quantity") val quantity: Int,
)

data class LootGainDto(
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("name") val name: String,
    @SerializedName("rarity") val rarity: String,
    @SerializedName("category") val category: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("quantity_total") val quantityTotal: Int,
    @SerializedName("is_duplicate_compensation") val isDuplicateCompensation: Boolean,
)

data class ChestDto(
    @SerializedName("id") val id: Long,
    @SerializedName("child_id") val childId: Long,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("rarity") val rarity: String,
    @SerializedName("crystal_cost") val crystalCost: Int,
    @SerializedName("description") val description: String,
    @SerializedName("possible_rewards") val possibleRewards: List<String>,
    @SerializedName("status") val status: String,
    @SerializedName("source_type") val sourceType: String? = null,
    @SerializedName("source_id") val sourceId: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("opened_at") val openedAt: String? = null,
)

data class InventoryDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("currencies") val currencies: Map<String, Int>,
    @SerializedName("items") val items: List<InventoryItemDto>,
    @SerializedName("chests") val chests: List<ChestDto>,
)

data class ChestCatalogEntryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("rarity") val rarity: String,
    @SerializedName("crystal_cost") val crystalCost: Int,
    @SerializedName("description") val description: String,
    @SerializedName("possible_rewards") val possibleRewards: List<String>,
)

data class ChestCatalogDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("crystals_balance") val crystalsBalance: Int,
    @SerializedName("chests") val chests: List<ChestCatalogEntryDto>,
)

data class EggDto(
    @SerializedName("id") val id: Long,
    @SerializedName("child_id") val childId: Long,
    @SerializedName("egg_key") val eggKey: String,
    @SerializedName("title") val title: String,
    @SerializedName("status") val status: String,
    @SerializedName("state") val state: String,
    @SerializedName("progress_percent") val progressPercent: Int,
    @SerializedName("next_state") val nextState: String? = null,
    @SerializedName("asset_key") val assetKey: String,
    @SerializedName("obtained_at") val obtainedAt: String? = null,
    @SerializedName("hatched_at") val hatchedAt: String? = null,
    @SerializedName("requirements") val requirements: Map<String, Int>,
)

data class DragonDto(
    @SerializedName("id") val id: Long,
    @SerializedName("child_id") val childId: Long,
    @SerializedName("dragon_key") val dragonKey: String,
    @SerializedName("title") val title: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("progress_percent") val progressPercent: Int,
    @SerializedName("active_companion") val activeCompanion: Boolean,
    @SerializedName("asset_key") val assetKey: String,
    @SerializedName("next_evolution") val nextEvolution: Map<String, Any>? = null,
)

data class EggsDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("eggs") val eggs: List<EggDto>,
)

data class DragonsDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("dragons") val dragons: List<DragonDto>,
    @SerializedName("active_companion") val activeCompanion: DragonDto? = null,
)

data class DuplicateCompensationDto(
    @SerializedName("reason") val reason: String,
    @SerializedName("family_id") val familyId: String,
    @SerializedName("item") val item: InventoryItemDto,
    @SerializedName("quantity_awarded") val quantityAwarded: Int,
)

data class OpenCatalogChestDto(
    @SerializedName("chest") val chest: ChestDto,
    @SerializedName("loot") val loot: List<LootGainDto>,
    @SerializedName("granted_egg") val grantedEgg: EggDto? = null,
    @SerializedName("duplicate_compensation") val duplicateCompensation: DuplicateCompensationDto? = null,
    @SerializedName("crystals_spent") val crystalsSpent: Int,
    @SerializedName("crystals_balance") val crystalsBalance: Int,
    @SerializedName("inventory_after") val inventoryAfter: InventoryDto,
    @SerializedName("catalog_chest") val catalogChest: ChestCatalogEntryDto,
    @SerializedName("progress") val progress: Map<String, Any>,
)

data class EggEvolutionDto(
    @SerializedName("egg") val egg: EggDto,
    @SerializedName("hatched") val hatched: Boolean,
    @SerializedName("dragon") val dragon: DragonDto? = null,
    @SerializedName("inventory") val inventory: InventoryDto,
)

data class ActiveCompanionDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("active_companion") val activeCompanion: DragonDto,
)

data class DragonEvolutionDto(
    @SerializedName("dragon") val dragon: DragonDto,
    @SerializedName("inventory") val inventory: InventoryDto,
)

data class StateUnlockDto(
    @SerializedName("state") val state: String,
    @SerializedName("unlocked") val unlocked: Boolean,
)

data class LegendaryArtifactDto(
    @SerializedName("item_key") val itemKey: String,
    @SerializedName("required") val required: Int,
    @SerializedName("owned") val owned: Int,
)

data class BestiaryFamilyDto(
    @SerializedName("family_id") val familyId: String,
    @SerializedName("family_name") val familyName: String,
    @SerializedName("element") val element: String,
    @SerializedName("discovered") val discovered: Boolean,
    @SerializedName("egg_owned") val eggOwned: Boolean,
    @SerializedName("dragon_owned") val dragonOwned: Boolean,
    @SerializedName("current_egg_state") val currentEggState: String? = null,
    @SerializedName("current_dragon_stage") val currentDragonStage: String? = null,
    @SerializedName("active_companion") val activeCompanion: Boolean,
    @SerializedName("legendary_unlocked") val legendaryUnlocked: Boolean,
    @SerializedName("progress_percent") val progressPercent: Int,
    @SerializedName("egg_asset_key") val eggAssetKey: String,
    @SerializedName("dragon_asset_key") val dragonAssetKey: String,
    @SerializedName("egg_states") val eggStates: List<StateUnlockDto>,
    @SerializedName("dragon_stages") val dragonStages: List<StateUnlockDto>,
    @SerializedName("legendary_artifact") val legendaryArtifact: LegendaryArtifactDto,
)

data class BestiaryDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("families") val families: List<BestiaryFamilyDto>,
)

data class ScrollsDto(
    @SerializedName("child_id") val childId: Long,
    @SerializedName("scrolls") val scrolls: List<RewardCouponDto>,
    @SerializedName("requests") val requests: List<RewardRequestDto>,
)
