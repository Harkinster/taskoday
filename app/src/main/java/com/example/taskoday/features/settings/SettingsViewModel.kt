package com.example.taskoday.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.ChildProfileDashboard
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.PairingRepository
import com.example.taskoday.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val childrenRepository: ChildrenRepository,
        private val pairingRepository: PairingRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            refreshProfile()
        }

        fun setNotificationsEnabled(enabled: Boolean) {
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }

        fun setDynamicColors(enabled: Boolean) {
            _uiState.update { it.copy(useDynamicColors = enabled) }
        }

        fun selectFamily(familyId: Long) {
            _uiState.update { it.copy(selectedFamilyId = familyId, pairingErrorMessage = null, pairingSuccessMessage = null) }
        }

        fun fetchOrGeneratePairingCode() {
            if (_uiState.value.isParentUser) return
            if (authRepository.getAccessToken().isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        pairingErrorMessage = "Mode hors-ligne: code indisponible.",
                        pairingSuccessMessage = null,
                    )
                }
                return
            }

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isPairingBusy = true,
                        pairingErrorMessage = null,
                        pairingSuccessMessage = null,
                    )
                }

                val pairingResult =
                    runCatching { pairingRepository.getMyCode() }
                        .recoverCatching { throwable ->
                            if (throwable is HttpException && throwable.code() == 404) {
                                pairingRepository.generateCode()
                            } else {
                                throw throwable
                            }
                        }

                pairingResult
                    .onSuccess { code ->
                        _uiState.update {
                            it.copy(
                                isPairingBusy = false,
                                pairingCode = code.code,
                                pairingCodeExpiresAt = code.expiresAt,
                                pairingSuccessMessage = "Code parent genere.",
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isPairingBusy = false,
                                pairingErrorMessage = throwable.toPairingMessage(),
                            )
                        }
                    }
            }
        }

        fun attachChildWithCode(code: String) {
            if (!_uiState.value.isParentUser) {
                _uiState.update { it.copy(pairingErrorMessage = "Action non autorisee.", pairingSuccessMessage = null) }
                return
            }
            if (authRepository.getAccessToken().isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        pairingErrorMessage = "Mode hors-ligne: association indisponible.",
                        pairingSuccessMessage = null,
                    )
                }
                return
            }
            if (code.trim().isEmpty()) {
                _uiState.update { it.copy(pairingErrorMessage = "Saisis un code d association.", pairingSuccessMessage = null) }
                return
            }

            val familyIds = _uiState.value.familyIds
            val selectedFamilyId =
                when {
                    familyIds.isEmpty() -> null
                    familyIds.size == 1 -> familyIds.first()
                    else -> _uiState.value.selectedFamilyId
                }
            if (familyIds.size > 1 && selectedFamilyId == null) {
                _uiState.update {
                    it.copy(
                        pairingErrorMessage = "Selectionne une famille avant d associer l enfant.",
                        pairingSuccessMessage = null,
                    )
                }
                return
            }

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isPairingBusy = true,
                        pairingErrorMessage = null,
                        pairingSuccessMessage = null,
                    )
                }

                val attachResult = pairingRepository.attachChild(code, selectedFamilyId)
                if (attachResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isPairingBusy = false,
                            pairingErrorMessage = attachResult.exceptionOrNull().toPairingMessage(),
                        )
                    }
                    return@launch
                }

                val refreshedChildren = runCatching { childrenRepository.fetchChildren() }.getOrDefault(emptyList())
                if (refreshedChildren.isNotEmpty()) {
                    authRepository.setActiveChildId(refreshedChildren.first().id)
                }

                _uiState.update {
                    it.copy(
                        isPairingBusy = false,
                        pairedChildren = refreshedChildren,
                        pairingSuccessMessage = "Enfant associe avec succes.",
                        pairingErrorMessage = null,
                    )
                }
            }
        }

        fun clearPairingMessages() {
            _uiState.update { it.copy(pairingErrorMessage = null, pairingSuccessMessage = null) }
        }

        private fun refreshProfile() {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isParentUser = false,
                        familyIds = emptyList(),
                        selectedFamilyId = null,
                        profileName = "Profil local",
                        profileSubtitle = "Mode hors-ligne",
                        profileEmail = "",
                        profileInitials = "TL",
                        totalXp = 0,
                        level = 1,
                        levelXp = 0,
                        nextLevelXp = XP_PER_LEVEL,
                        missionsStat = "0",
                        questsStat = "0",
                        streakStat = "0 j",
                        successStat = "0%",
                        xpHistoryTokens = emptyList(),
                        pairedChildren = emptyList(),
                        pairingCode = null,
                        pairingCodeExpiresAt = null,
                        isPairingBusy = false,
                        pairingSuccessMessage = null,
                        pairingErrorMessage = null,
                        profileErrorMessage = null,
                    )
                }
                return
            }

            viewModelScope.launch {
                val meResult = runCatching { authRepository.fetchMe() }
                val me = meResult.getOrNull()
                val isParent = me?.role.equals("PARENT", ignoreCase = true)
                val familyIds = me?.familyIds?.distinct().orEmpty()
                val preferredFamilyId =
                    _uiState.value.selectedFamilyId
                        ?.takeIf { familyIds.contains(it) }
                        ?: if (familyIds.size == 1) familyIds.first() else familyIds.firstOrNull()

                val dashboardResult = runCatching { profileRepository.fetchActiveChildDashboard() }
                val dashboard = dashboardResult.getOrNull()
                val profile = dashboard?.profile
                val profileName = profile?.displayName?.ifBlank { null } ?: me?.email?.substringBefore('@') ?: "Profil local"
                val profileSubtitle =
                    when {
                        profile != null && !profile.birthDate.isNullOrBlank() -> "Naissance: ${profile.birthDate}"
                        profile != null && !profile.email.isNullOrBlank() -> profile.email
                        me != null -> me.email
                        else -> "Mode hors-ligne"
                    }
                val xp = dashboard?.stats?.totalXp ?: 0
                val level = (xp / XP_PER_LEVEL) + 1
                val levelXp = xp % XP_PER_LEVEL
                val children =
                    if (isParent) {
                        runCatching { childrenRepository.fetchChildren() }.getOrDefault(_uiState.value.pairedChildren)
                    } else {
                        emptyList()
                    }

                _uiState.update {
                    it.copy(
                        isParentUser = isParent,
                        familyIds = familyIds,
                        selectedFamilyId = preferredFamilyId,
                        profileName = profileName,
                        profileSubtitle = profileSubtitle,
                        profileEmail = profile?.email ?: me?.email.orEmpty(),
                        profileInitials = initialsFrom(profileName),
                        totalXp = xp,
                        level = level,
                        levelXp = levelXp,
                        nextLevelXp = XP_PER_LEVEL,
                        missionsStat = missionStatFrom(dashboard),
                        questsStat = questStatFrom(dashboard),
                        streakStat = "${dashboard?.stats?.streakDays ?: 0} j",
                        successStat = "${dashboard?.stats?.successRatePercent ?: 0}%",
                        xpHistoryTokens = xpHistoryTokensFrom(dashboard),
                        pairedChildren = children,
                        profileErrorMessage = (dashboardResult.exceptionOrNull() ?: meResult.exceptionOrNull())?.toMessage(),
                    )
                }
            }
        }
    }

private const val XP_PER_LEVEL: Int = 1000

private fun missionStatFrom(dashboard: ChildProfileDashboard?): String =
    dashboard?.stats?.let { "${it.missionsCompleted}/${it.missionsTotal}" } ?: "0/0"

private fun questStatFrom(dashboard: ChildProfileDashboard?): String =
    dashboard?.stats?.let { "${it.questsCompleted}/${it.questsTotal}" } ?: "0/0"

private fun xpHistoryTokensFrom(dashboard: ChildProfileDashboard?): List<String> =
    dashboard
        ?.xpHistory
        ?.take(4)
        ?.map { entry ->
            val amount = if (entry.amount >= 0) "+${entry.amount}" else entry.amount.toString()
            "$amount XP"
        } ?: emptyList()

private fun initialsFrom(value: String): String =
    value
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { token -> token.first().uppercase() }
        .ifBlank { "TT" }

private fun Throwable.toMessage(): String? =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible, profil local affiche."
        is SocketTimeoutException -> "Serveur lent, profil local affiche."
        is HttpException -> if (code() == 401) "Session expiree, reconnexion requise." else "Erreur API (${code()})."
        is IOException -> "Erreur reseau, profil local affiche."
        else -> null
    }

private fun Throwable?.toPairingMessage(): String =
    when (this) {
        is HttpException ->
            when (code()) {
                400 -> "Code invalide ou expire."
                403 -> "Action non autorisee."
                401 -> "Session expiree, reconnecte-toi."
                else -> "Erreur API (${code()})."
            }

        is UnknownHostException, is ConnectException -> "Reseau indisponible, impossible de contacter le serveur."
        is SocketTimeoutException -> "Le serveur ne repond pas a temps."
        is IOException -> "Erreur reseau, reessaie plus tard."
        else -> this?.message ?: "Erreur inconnue."
    }
