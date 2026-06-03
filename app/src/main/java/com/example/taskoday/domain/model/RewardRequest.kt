package com.example.taskoday.domain.model

data class RewardRedemptionRequest(
    val id: Long,
    val childId: Long,
    val rewardId: Long?,
    val rewardTitle: String,
    val costScales: Int,
    val status: RewardRequestStatus,
    val requestedAt: String?,
    val decidedAt: String?,
    val expiresAt: String?,
    val note: String?,
    val parentNote: String?,
    val coupon: RewardCoupon?,
)

data class RewardCoupon(
    val id: Long,
    val requestId: Long,
    val childId: Long,
    val rewardId: Long?,
    val code: String,
    val status: String,
    val createdAt: String?,
    val usedAt: String?,
)

enum class RewardRequestStatus(val apiValue: String, val label: String) {
    PENDING("pending", "En attente"),
    APPROVED("approved", "Approuvee"),
    REFUSED("refused", "Refusee"),
    USED("used", "Utilisee"),
    EXPIRED("expired", "Expiree");

    companion object {
        fun fromApi(value: String): RewardRequestStatus =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class RewardShopSnapshot(
    val childId: Long,
    val scalesBalance: Int,
    val rewards: List<Reward>,
    val requests: List<RewardRedemptionRequest>,
)
