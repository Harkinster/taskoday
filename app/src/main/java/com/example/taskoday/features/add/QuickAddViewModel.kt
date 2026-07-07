package com.example.taskoday.features.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAddUiState(
    val isLoading: Boolean = true,
    val hasRemoteSession: Boolean = false,
    val isParent: Boolean = false,
) {
    val canOpenQuickAdd: Boolean = !isLoading && hasRemoteSession && isParent
    val canCreateRoutine: Boolean = canOpenQuickAdd
    val canCreateMission: Boolean = canOpenQuickAdd
    val canCreateQuest: Boolean = canOpenQuickAdd
    val denialMessage: String?
        get() =
            if (!isLoading && !isParent) {
                "Action réservée au parent."
            } else {
                null
            }
}

@HiltViewModel
class QuickAddViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(QuickAddUiState())
        val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value =
                    QuickAddUiState(
                        isLoading = false,
                        hasRemoteSession = false,
                        isParent = false,
                    )
                return
            }

            _uiState.update { it.copy(isLoading = true, hasRemoteSession = true) }
            viewModelScope.launch {
                val isParent =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                _uiState.value =
                    QuickAddUiState(
                        isLoading = false,
                        hasRemoteSession = true,
                        isParent = isParent,
                    )
            }
        }
    }
