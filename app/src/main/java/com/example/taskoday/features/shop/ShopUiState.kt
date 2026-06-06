package com.example.taskoday.features.shop

import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.data.remote.dto.ChestCatalogDto
import com.example.taskoday.data.remote.dto.OpenCatalogChestDto

data class ShopUiState(
    val scalesBalance: Int = 0,
    val rewards: List<Reward> = emptyList(),
    val requests: List<RewardRedemptionRequest> = emptyList(),
    val localTransactions: List<PointsTransaction> = emptyList(),
    val chestCatalog: ChestCatalogDto? = null,
    val lastOpenedChest: OpenCatalogChestDto? = null,
    val hasRemoteSession: Boolean = false,
    val isParent: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val userMessage: String? = null,
)
