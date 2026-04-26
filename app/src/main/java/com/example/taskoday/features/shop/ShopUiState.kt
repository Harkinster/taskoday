package com.example.taskoday.features.shop

import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Reward

data class ShopUiState(
    val pointsBalance: Int = 0,
    val rewards: List<Reward> = emptyList(),
    val purchases: List<PointsTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
)
