package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ResourceBalanceDto(
    @SerializedName("family_id") val familyId: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("taskoday_points") val taskodayPoints: Int,
    val flames: Int,
    val crystals: Int,
)

data class WishOfferDto(
    val id: Long,
    @SerializedName("family_id") val familyId: Long,
    val title: String,
    val description: String? = null,
    @SerializedName("flame_cost") val flameCost: Int,
    val active: Boolean,
)

data class WishOfferListDto(val items: List<WishOfferDto>)

data class WishRequestDto(
    val id: Long,
    @SerializedName("offer_id") val offerId: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("requester_name") val requesterName: String,
    val title: String,
    @SerializedName("flame_cost") val flameCost: Int,
    val status: String,
    @SerializedName("requested_at") val requestedAt: String,
    @SerializedName("decided_by") val decidedBy: Long? = null,
    @SerializedName("decided_at") val decidedAt: String? = null,
)

data class WishRequestListDto(val items: List<WishRequestDto>)

data class WishRequestCreateDto(@SerializedName("offer_id") val offerId: Long)
data class WishOfferCreateDto(val title: String, val description: String?, @SerializedName("flame_cost") val flameCost: Int)
data class WishOfferUpdateDto(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("flame_cost") val flameCost: Int? = null,
    val active: Boolean? = null,
)
data class WishObtainDto(@SerializedName("offer_id") val offerId: Long, @SerializedName("idempotency_key") val idempotencyKey: String)

data class ChestPolicyDto(
    @SerializedName("chest_type") val chestType: String,
    val title: String,
    @SerializedName("crystal_cost") val crystalCost: Int,
    @SerializedName("drop_count") val dropCount: Int,
)

data class ResourceChestCatalogDto(
    @SerializedName("family_id") val familyId: Long,
    @SerializedName("user_id") val userId: Long,
    val crystals: Int,
    val chests: List<ChestPolicyDto>,
)

data class ChestOpenRequestDto(@SerializedName("chest_type") val chestType: String, @SerializedName("idempotency_key") val idempotencyKey: String)
data class ChestDropDto(@SerializedName("collectible_key") val collectibleKey: String, val title: String, val quantity: Int)
data class ChestOpenDto(val id: Long, @SerializedName("chest_type") val chestType: String, @SerializedName("crystal_cost") val crystalCost: Int, @SerializedName("created_at") val createdAt: String, val drops: List<ChestDropDto>)
data class ChestOpenListDto(val items: List<ChestOpenDto>)

data class CollectionItemDto(@SerializedName("collectible_key") val collectibleKey: String, val title: String, val quantity: Int)
data class CollectionDto(val items: List<CollectionItemDto>)
