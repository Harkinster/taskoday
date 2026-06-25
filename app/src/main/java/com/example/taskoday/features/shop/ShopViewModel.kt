package com.example.taskoday.features.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.repository.toRemoteUserMessage
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
import retrofit2.HttpException

@HiltViewModel
class ShopViewModel
    @Inject
    constructor(
        private val rewardRepository: RewardRepository,
        private val pointsRepository: PointsRepository,
        private val authRepository: AuthRepository,
        private val nestRepository: NestRepository,
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
                    .fetchRemoteShopSnapshot(includeInactiveRewards = isParent)
                    .onSuccess { snapshot ->
                        val chestCatalog = nestRepository.getChestCatalog().getOrNull()
                        _uiState.update {
                            it.copy(
                                rewards = snapshot.rewards,
                                scalesBalance = snapshot.scalesBalance,
                                requests = snapshot.requests,
                                chestCatalog = chestCatalog,
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

        fun openChest(catalogId: String) {
            val currentState = _uiState.value
            if (currentState.isSubmitting) return

            val chestCatalog = currentState.chestCatalog
            val selectedChest = chestCatalog?.chests?.firstOrNull { chest -> chest.id == catalogId }
            if (selectedChest != null && chestCatalog.crystalsBalance < selectedChest.crystalCost) {
                _uiState.update {
                    it.copy(
                        userMessage =
                            missingCrystalsMessage(
                                crystalsBalance = chestCatalog.crystalsBalance,
                                crystalCost = selectedChest.crystalCost,
                            ),
                    )
                }
                return
            }

            _uiState.update { it.copy(isSubmitting = true, lastOpenedChest = null) }
            viewModelScope.launch {
                nestRepository
                    .openCatalogChest(catalogId)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                lastOpenedChest = result,
                                userMessage = "${result.chest.name} ouvert : ${result.loot.sumOf { loot -> loot.quantity }} objets gagnes.",
                            )
                        }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = error.toChestOpenUserMessage(),
                            )
                        }
                    }
            }
        }

        fun requestReward(reward: Reward) {
            val currentState = uiState.value
            if (currentState.isSubmitting) return
            if (currentState.isParent) {
                _uiState.update { it.copy(userMessage = "Le parent gere le catalogue, l'enfant fait la demande.") }
                return
            }
            if (currentState.requests.any { it.rewardId == reward.id && it.status == RewardRequestStatus.PENDING }) {
                _uiState.update { it.copy(userMessage = "Demande deja en attente.") }
                return
            }
            if (currentState.scalesBalance < reward.cost) {
                _uiState.update { it.copy(userMessage = missingScalesMessage(currentState.scalesBalance, reward.cost)) }
                return
            }

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
                                userMessage = "Demande envoyee.",
                            )
                        }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toWishUserMessage()) }
                    }
            }
        }

        fun createReward(
            title: String,
            description: String?,
            costScales: Int,
            isActive: Boolean = true,
        ) {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .createRemoteReward(
                        title = title,
                        description = description,
                        costScales = costScales,
                        isActive = isActive,
                    )
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Souhait cree.") }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun updateReward(
            rewardId: Long,
            title: String,
            description: String?,
            costScales: Int,
            isActive: Boolean,
        ) {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .updateRemoteReward(
                        rewardId = rewardId,
                        title = title,
                        description = description,
                        costScales = costScales,
                        isActive = isActive,
                    ).onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Souhait mis a jour.") }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toUserMessage()) }
                    }
            }
        }

        fun deactivateReward(rewardId: Long) {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .deactivateRemoteReward(rewardId)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Souhait desactive.") }
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
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                rewardRepository
                    .decideRemoteRequest(requestId = requestId, status = status)
                    .onSuccess {
                        val message =
                            when (status) {
                                RewardRequestStatus.APPROVED -> "Souhait valide."
                                RewardRequestStatus.REFUSED -> "Souhait refuse."
                                else -> "Demande mise a jour."
                            }
                        _uiState.update { it.copy(isSubmitting = false, userMessage = message) }
                        refreshRemoteData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, userMessage = error.toWishUserMessage()) }
                    }
            }
        }

        fun useCoupon(couponId: Long) {
            val currentState = _uiState.value
            if (currentState.isSubmitting) return
            if (!currentState.isParent) {
                _uiState.update { it.copy(userMessage = "Seul le parent peut marquer un souhait comme utilise.") }
                return
            }
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                rewardRepository
                    .useRemoteCoupon(couponId)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, userMessage = "Souhait utilise.") }
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

private fun Throwable.toUserMessage(): String = toRemoteUserMessage("Erreur Caverne.")

private fun Throwable.toWishUserMessage(): String {
    if (this is HttpException && code() == 400) {
        val backendBody = response()?.errorBody()?.string().orEmpty()
        if (backendBody.contains("Flammeches insuffisantes", ignoreCase = true)) {
            val balances =
                Regex("""balance=(\d+),\s*required=(\d+)""")
                    .find(backendBody)
                    ?.groupValues
                    ?.drop(1)
                    ?.mapNotNull(String::toIntOrNull)
            if (balances?.size == 2) {
                return missingScalesMessage(scalesBalance = balances[0], requiredScales = balances[1])
            }
            return "Flammeches insuffisantes."
        }
    }
    return toUserMessage()
}

private fun missingScalesMessage(
    scalesBalance: Int,
    requiredScales: Int,
): String {
    val missing = (requiredScales - scalesBalance).coerceAtLeast(0)
    return if (missing == 1) {
        "Il te manque 1 Flammeche pour ce souhait."
    } else {
        "Il te manque $missing Flammeches pour ce souhait."
    }
}
