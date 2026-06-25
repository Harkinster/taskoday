package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RewardsDataDto(
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("scales_balance")
    val scalesBalance: Int,
    @SerializedName("rewards")
    val rewards: List<ExternalRewardDto>,
)

data class RewardRequestsDataDto(
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("scales_balance")
    val scalesBalance: Int,
    @SerializedName("requests")
    val requests: List<RewardRequestDto>,
)

data class ExternalRewardDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("cost_scales")
    val costScales: Int,
    @SerializedName("emoji")
    val emoji: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class RewardRequestDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("reward_id")
    val rewardId: Long? = null,
    @SerializedName("reward_title")
    val rewardTitle: String,
    @SerializedName("cost_scales")
    val costScales: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("requested_at")
    val requestedAt: String? = null,
    @SerializedName("decided_at")
    val decidedAt: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("parent_note")
    val parentNote: String? = null,
    @SerializedName("coupon")
    val coupon: RewardCouponDto? = null,
)

data class RewardCouponDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("request_id")
    val requestId: Long,
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("reward_id")
    val rewardId: Long? = null,
    @SerializedName("code")
    val code: String,
    @SerializedName("status")
    val status: String = "available",
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("used_at")
    val usedAt: String? = null,
)

data class RewardCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("cost_scales")
    val costScales: Int,
    @SerializedName("emoji")
    val emoji: String = "gift",
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class RewardUpdateRequestDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("cost_scales")
    val costScales: Int? = null,
    @SerializedName("emoji")
    val emoji: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
)

data class RewardRequestCreateDto(
    @SerializedName("note")
    val note: String? = null,
)

data class RewardRequestDecisionDto(
    @SerializedName("status")
    val status: String,
    @SerializedName("parent_note")
    val parentNote: String? = null,
)
