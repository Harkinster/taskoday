package com.example.taskoday.features.shop

import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus

data class WishRequestGuardResult(
    val allowed: Boolean,
    val message: String? = null,
)

fun wishRequestGuard(
    state: ShopUiState,
    reward: Reward,
    allowParentLocalChildMode: Boolean,
): WishRequestGuardResult {
    if (state.isParent && !allowParentLocalChildMode) {
        return WishRequestGuardResult(false, "Le parent gère le catalogue, l'enfant fait la demande.")
    }
    if (!state.hasRemoteSession) {
        return WishRequestGuardResult(false, "Connecte le compte enfant pour demander un Souhait.")
    }
    val activeChildId = state.activeChildId
        ?: return WishRequestGuardResult(false, "Aucun enfant sélectionné.")
    if (reward.childId != null && reward.childId != activeChildId) {
        return WishRequestGuardResult(false, "Ce souhait n'appartient pas à l'enfant sélectionné.")
    }
    if (state.requests.hasPendingRequestFor(reward.id)) {
        return WishRequestGuardResult(false, "Demande déjà en attente.")
    }
    if (state.scalesBalance < reward.cost) {
        return WishRequestGuardResult(false, missingScalesMessage(state.scalesBalance, reward.cost))
    }
    return WishRequestGuardResult(true)
}

private fun List<RewardRedemptionRequest>.hasPendingRequestFor(rewardId: Long): Boolean =
    any { request -> request.rewardId == rewardId && request.status == RewardRequestStatus.PENDING }
