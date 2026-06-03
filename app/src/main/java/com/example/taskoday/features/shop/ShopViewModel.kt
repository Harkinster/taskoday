package com.example.taskoday.features.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRequestStatus
import com.example.taskoday.domain.repository.AuthRepository
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
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ShopUiState())
        val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

        init {
            observeLocalData()
            refreshRemoteData()
        }

        private fun observeLocalData() {
            viewModelScope.launch {
                combine(
                    rewardRepository.observeActiveRewards(),
                    pointsRepository.observeBalance(),
                    pointsRepository.observeRecentTransactions(limit = 30),
                ) { rewards, scalesBalance, transactions ->
                    Triple(
                        rewards,
                        scalesBalance,
                        transactions.filter { it.sourceType == PointsSourceType.REWARD_PURCHASE },
                    )
                }.collect { (rewards, scalesBalance, purchases) ->
                    _uiState.update {
                        if (it.hasRemoteSession) {
                            it.copy(localTransactions = purchases)
                        } else {
                            it.copy(
                                rewards = rewards,
                                scalesBalance = scalesBalance,
                                localTransactions = purchases,
                                isLoading = false,
                            )
                        }
                    }
                }
            }
        }

        fun refreshRemoteData() {
            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(hasRemoteSession = false, isParent = false, isLoading = false) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(hasRemoteSession = true, isLoading = true) }
                val isParent =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                rewardRepository
                    .fetchRemoteShopSnapshot()
                    .onSuccess { snapshot ->
                        _uiState.update {
                            it.copy(
                                rewards = snapshot.rewards,
                                scalesBalance = snapshot.scalesBalance,
                                requests = snapshot.requests,
                                hasRemoteSession = true,
                                isParent = isParent,
                                isLoading = false,
                                userMessage = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                hasRemoteSession = true,
                                isParent = isParent,
                                isLoading = false,
                                userMessage = error.toUserMessage(),
                            )
                        }
                    }
            }
        }

        fun requestReward(reward: Reward) {
            viewModelScope.launch {
                if (!uiState.value.hasRemoteSession) {
                    _uiState.update { it.copy(userMessage = "Connecte le compte enfant pour demander un Souhait.") }
                    return@launch
                }

                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .requestRemoteReward(reward.id)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isSubmitting = false,
                                userMessage = "Demande envoyee au parent.",
                            )
                        }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun createReward(
            title: String,
            description: String?,
            costScales: Int,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .createRemoteReward(title = title, description = description, costScales = costScales)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Souhait créé.") }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun approveRequest(requestId: Long) {
            decideRequest(requestId = requestId, status = RewardRequestStatus.APPROVED)
        }

        fun refuseRequest(requestId: Long) {
            decideRequest(requestId = requestId, status = RewardRequestStatus.REFUSED)
        }

        private fun decideRequest(
            requestId: Long,
            status: RewardRequestStatus,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .decideRemoteRequest(requestId = requestId, status = status)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Demande mise a jour.") }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun useCoupon(couponId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .useRemoteCoupon(couponId)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Parchemin utilisé.") }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun consumeMessage() {
            _uiState.update { it.copy(userMessage = null) }
        }
    }

private fun Throwable.toUserMessage(): String = message ?: "Erreur Caverne aux Souhaits."
