package com.example.taskoday.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.repository.AuthRepository
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

data class FamilyNotificationsUiState(
    val dailySummaryEnabled: Boolean = false,
    val hour: Int = DEFAULT_DAILY_SUMMARY_HOUR,
    val minute: Int = DEFAULT_DAILY_SUMMARY_MINUTE,
    val isBusy: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class FamilyNotificationsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val settingsController: FamilyNotificationSettingsController,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(settingsController.loadSettings().toUiState())
        val uiState: StateFlow<FamilyNotificationsUiState> = _uiState.asStateFlow()

        fun setDailySummaryEnabled(enabled: Boolean) {
            if (_uiState.value.isBusy) return
            if (!enabled) {
                val settings = settingsController.disableDailySummary()
                _uiState.value =
                    settings.toUiState(
                        successMessage = "Resume quotidien desactive.",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
                val parentCheck =
                    runCatching {
                        if (authRepository.getAccessToken().isNullOrBlank()) {
                            error("Aucune session parent active.")
                        }
                        authRepository.fetchMe()
                    }
                parentCheck
                    .onSuccess { me ->
                        if (!me.role.equals("PARENT", ignoreCase = true)) {
                            val disabled = settingsController.disableDailySummary()
                            _uiState.value =
                                disabled.toUiState(
                                    errorMessage = "Les notifications familiales sont reservees au parent.",
                                )
                            return@launch
                        }

                        val settings = settingsController.enableDailySummary()
                        _uiState.value =
                            settings.toUiState(
                                successMessage = "Resume quotidien planifie.",
                            )
                    }.onFailure { throwable ->
                        val disabled = settingsController.disableDailySummary()
                        _uiState.value =
                            disabled.toUiState(
                                errorMessage = throwable.toNotificationSettingsMessage(),
                            )
                    }
            }
        }

        fun updateDailySummaryTime(
            hour: Int,
            minute: Int,
        ) {
            val settings = settingsController.updateDailySummaryTime(hour = hour, minute = minute)
            _uiState.value =
                settings.toUiState(
                    successMessage =
                        if (settings.dailySummaryEnabled) {
                            "Resume quotidien replannifie a ${formatFamilyNotificationTime(settings.hour, settings.minute)}."
                        } else {
                            "Heure enregistree."
                        },
                )
        }

        fun onNotificationPermissionDenied() {
            val settings = settingsController.disableDailySummary()
            _uiState.value =
                settings.toUiState(
                    errorMessage = "Autorise les notifications Android pour activer le resume quotidien.",
                )
        }

        fun clearMessages() {
            _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

private fun FamilyNotificationSettings.toUiState(
    successMessage: String? = null,
    errorMessage: String? = null,
): FamilyNotificationsUiState =
    FamilyNotificationsUiState(
        dailySummaryEnabled = dailySummaryEnabled,
        hour = hour,
        minute = minute,
        isBusy = false,
        successMessage = successMessage,
        errorMessage = errorMessage,
    )

private fun Throwable.toNotificationSettingsMessage(): String =
    when (this) {
        is HttpException ->
            when (code()) {
                401 -> "Session expiree, reconnecte-toi avant d'activer les notifications."
                403 -> "Action reservee au parent."
                else -> "Erreur API (${code()})."
            }

        is UnknownHostException,
        is ConnectException,
        -> "Serveur indisponible, impossible de verifier la session parent."

        is SocketTimeoutException -> "Le serveur ne repond pas a temps."
        is IOException -> "Erreur reseau, impossible de verifier la session parent."
        else -> message ?: "Impossible d'activer les notifications."
    }
