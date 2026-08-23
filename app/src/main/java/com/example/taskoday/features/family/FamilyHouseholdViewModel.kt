package com.example.taskoday.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.repository.FamilyRepository
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class FamilyHouseholdViewModel
    @Inject
    constructor(
        private val familyRepository: FamilyRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FamilyHouseholdUiState())
        val uiState: StateFlow<FamilyHouseholdUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                familyRepository
                    .fetchMembers()
                    .onSuccess { members -> publishMembers(members) }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                members = emptyList(),
                                errorMessage = throwable.toRemoteUserMessage("Impossible de charger le foyer."),
                            )
                        }
                    }
            }
        }

        fun updateJoinCode(value: String) {
            _uiState.update { it.copy(joinCode = value, errorMessage = null, message = null) }
        }

        fun clearMessages() {
            _uiState.update { it.copy(message = null, errorMessage = null) }
        }

        fun createInvite() {
            if (_uiState.value.isInviteBusy) return
            viewModelScope.launch {
                _uiState.update { it.copy(isInviteBusy = true, errorMessage = null, message = null) }
                familyRepository
                    .createParentInvite()
                    .onSuccess { invite ->
                        _uiState.update {
                            it.copy(
                                isInviteBusy = false,
                                invite = invite,
                                message = "Invitation prête.",
                                errorMessage = null,
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isInviteBusy = false,
                                errorMessage =
                                    throwable.toHouseholdMessage("Impossible de créer une invitation pour le moment."),
                            )
                        }
                    }
            }
        }

        fun markInviteCopied() {
            _uiState.update { it.copy(message = "Code copié.") }
        }

        fun acceptInvite() {
            val code = normalizeFamilyInviteCodeInput(_uiState.value.joinCode)
            if (code.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Code d'invitation requis.") }
                return
            }
            if (_uiState.value.isJoinBusy) return

            viewModelScope.launch {
                _uiState.update { it.copy(isJoinBusy = true, errorMessage = null, message = null) }
                familyRepository
                    .acceptParentInvite(code)
                    .onSuccess {
                        val members = familyRepository.fetchMembers().getOrElse { emptyList() }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isJoinBusy = false,
                                members = members,
                                joinCode = "",
                                message = "Foyer rejoint. La session est à jour.",
                                errorMessage = null,
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isJoinBusy = false,
                                errorMessage = throwable.toHouseholdMessage("Impossible de rejoindre ce foyer."),
                            )
                        }
                    }
            }
        }

        private fun publishMembers(members: List<FamilyMember>) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    members = members,
                    errorMessage = null,
                )
            }
        }
    }

private fun Throwable.toHouseholdMessage(fallback: String): String =
    if (this is HttpException) {
        familyInviteUserMessage(
            httpCode = code(),
            backendMessage = response()?.errorBody()?.string().extractBackendMessage(),
            fallback = fallback,
        )
    } else {
        toRemoteUserMessage(fallback)
    }

private fun String?.extractBackendMessage(): String? {
    val raw = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val element = JsonParser.parseString(raw)
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            obj.get("message")?.asString
                ?: obj.get("detail")?.asString
                ?: obj.get("error")?.asString
        } else {
            null
        }
    }.getOrNull() ?: raw
}
