package com.example.taskoday.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus
import com.example.taskoday.domain.model.XpHistoryEntry
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ProfileRepository
import com.example.taskoday.domain.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ActivityJournalViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val rewardRepository: RewardRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ActivityJournalUiState())
        val uiState: StateFlow<ActivityJournalUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val isParent =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)

                val dashboardResult = runCatching { profileRepository.fetchActiveChildDashboard() }
                val rewardSnapshotResult = rewardRepository.fetchRemoteShopSnapshot(includeInactiveRewards = isParent)

                val dashboard = dashboardResult.getOrNull()
                val rewardSnapshot = rewardSnapshotResult.getOrNull()

                val actionEvents =
                    dashboard
                        ?.xpHistory
                        .orEmpty()
                        .filter { entry -> entry.amount > 0 }
                        .mapIndexed { index, entry -> entry.toActivityItem(index) }

                val wishEvents =
                    rewardSnapshot
                        ?.requests
                        .orEmpty()
                        .flatMap { request -> request.toActivityItems() }

                val errorMessage =
                    listOfNotNull(
                        dashboardResult.exceptionOrNull()?.message?.let { "Historique XP indisponible." },
                        rewardSnapshotResult.exceptionOrNull()?.message?.let { "Souhaits indisponibles." },
                    ).joinToString(" ").takeIf { it.isNotBlank() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isParent = isParent,
                        childLabel = dashboard?.profile?.displayName,
                        events =
                            (actionEvents + wishEvents)
                                .sortedWith(compareByDescending<ActivityJournalItem> { event -> event.sortKey }.thenBy { event -> event.id })
                                .take(MAX_EVENTS),
                        errorMessage = errorMessage,
                    )
                }
            }
        }

        private companion object {
            const val MAX_EVENTS = 40
        }
    }

private fun XpHistoryEntry.toActivityItem(index: Int): ActivityJournalItem {
    val reason = reason.orEmpty().trim()
    return ActivityJournalItem(
        id = "xp-$index-${dateLabel.hashCode()}-${amount.hashCode()}",
        sortKey = dateLabel,
        dateLabel = formatDateLabel(dateLabel),
        typeLabel = "Action terminee",
        title = reason.ifBlank { "Reussite du jour" },
        detail = "+$amount XP",
        kind = ActivityJournalKind.ACTION,
    )
}

private fun RewardRedemptionRequest.toActivityItems(): List<ActivityJournalItem> {
    val requested =
        ActivityJournalItem(
            id = "wish-requested-$id",
            sortKey = requestedAt.orEmpty(),
            dateLabel = formatDateLabel(requestedAt),
            typeLabel = "Souhait demandé",
            title = rewardTitle,
            detail = "${costScales} Flammèches - En attente parent",
            kind = ActivityJournalKind.WISH_PENDING,
        )

    val decided =
        when (status) {
            RewardRequestStatus.PENDING -> null
            RewardRequestStatus.APPROVED ->
                ActivityJournalItem(
                    id = "wish-approved-$id",
                    sortKey = decidedAt ?: coupon?.createdAt ?: requestedAt.orEmpty(),
                    dateLabel = formatDateLabel(decidedAt ?: coupon?.createdAt ?: requestedAt),
                    typeLabel = "Souhait accepté",
                    title = rewardTitle,
                    detail = coupon?.code?.let { "Parchemin disponible - $it" } ?: "Parchemin disponible",
                    kind = ActivityJournalKind.WISH_APPROVED,
                )
            RewardRequestStatus.REFUSED ->
                ActivityJournalItem(
                    id = "wish-refused-$id",
                    sortKey = decidedAt ?: requestedAt.orEmpty(),
                    dateLabel = formatDateLabel(decidedAt ?: requestedAt),
                    typeLabel = "Souhait refusé",
                    title = rewardTitle,
                    detail = parentNote?.takeIf { it.isNotBlank() } ?: "Refusé par le parent",
                    kind = ActivityJournalKind.WISH_REFUSED,
                )
            RewardRequestStatus.USED ->
                ActivityJournalItem(
                    id = "wish-used-$id",
                    sortKey = coupon?.usedAt ?: decidedAt ?: requestedAt.orEmpty(),
                    dateLabel = formatDateLabel(coupon?.usedAt ?: decidedAt ?: requestedAt),
                    typeLabel = "Souhait utilisé",
                    title = rewardTitle,
                    detail = "Parchemin utilisé",
                    kind = ActivityJournalKind.WISH_USED,
                )
            RewardRequestStatus.EXPIRED ->
                ActivityJournalItem(
                    id = "wish-expired-$id",
                    sortKey = expiresAt ?: decidedAt ?: requestedAt.orEmpty(),
                    dateLabel = formatDateLabel(expiresAt ?: decidedAt ?: requestedAt),
                    typeLabel = "Souhait expiré",
                    title = rewardTitle,
                    detail = "Demande expirée",
                    kind = ActivityJournalKind.WISH_REFUSED,
                )
        }

    return listOfNotNull(requested, decided)
}

private fun formatDateLabel(raw: String?): String? =
    raw
        ?.takeIf { it.isNotBlank() && !it.equals("Date inconnue", ignoreCase = true) }
        ?.substringBefore(".")
        ?.removeSuffix("Z")
        ?.replace("T", " ")
