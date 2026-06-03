package com.example.taskoday.features.shop

import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest

data class ShopUiState(
    val scalesBalance: Int = 0,
    val rewards: List<Reward> = emptyList(),
    val requests: List<RewardRedemptionRequest> = emptyList(),
    val localTransactions: List<PointsTransaction> = emptyList(),
    val hasRemoteSession: Boolean = false,
    val isParent: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val userMessage: String? = null,
)
