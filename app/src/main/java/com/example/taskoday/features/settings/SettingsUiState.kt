package com.example.taskoday.features.settings

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val useDynamicColors: Boolean = true,
    val appVersionLabel: String = "1.0",
)
