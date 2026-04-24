package com.example.taskoday.features.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        fun setNotificationsEnabled(enabled: Boolean) {
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }

        fun setDynamicColors(enabled: Boolean) {
            _uiState.update { it.copy(useDynamicColors = enabled) }
        }
    }
