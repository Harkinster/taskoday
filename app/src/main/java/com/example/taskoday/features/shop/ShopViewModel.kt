package com.example.taskoday.features.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.repository.PointsRepository
import com.example.taskoday.domain.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ShopViewModel
    @Inject
    constructor(
        private val rewardRepository: RewardRepository,
        private val pointsRepository: PointsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ShopUiState())
        val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

        init {
            observeData()
        }

        private fun observeData() {
            viewModelScope.launch {
                combine(
                    rewardRepository.observeActiveRewards(),
                    pointsRepository.observeBalance(),
                    pointsRepository.observeRecentTransactions(limit = 30),
                ) { rewards, pointsBalance, transactions ->
                    Triple(
                        rewards,
                        pointsBalance,
                        transactions.filter { it.sourceType == PointsSourceType.REWARD_PURCHASE },
                    )
                }.collect { (rewards, pointsBalance, purchases) ->
                    _uiState.update {
                        it.copy(
                            rewards = rewards,
                            pointsBalance = pointsBalance,
                            purchases = purchases,
                            isLoading = false,
                        )
                    }
                }
            }
        }

        fun buyReward(reward: Reward) {
            viewModelScope.launch {
                val success =
                    pointsRepository.spendForReward(
                        rewardId = reward.id,
                        cost = reward.cost,
                        reason = "Achat boutique: ${reward.title}",
                    )
                _uiState.update {
                    it.copy(
                        userMessage =
                            if (success) {
                                "Récompense achetée: ${reward.title}"
                            } else {
                                "Pas assez de points pour ${reward.title}"
                            },
                    )
                }
            }
        }

        fun consumeMessage() {
            _uiState.update { it.copy(userMessage = null) }
        }
    }
