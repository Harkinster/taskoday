package com.example.taskoday.features.settings

import com.example.taskoday.domain.model.ParentChild

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val useDynamicColors: Boolean = true,
    val appVersionLabel: String = "1.0",
    val isParentUser: Boolean = false,
    val familyIds: List<Long> = emptyList(),
    val selectedFamilyId: Long? = null,
    val profileName: String = "Profil local",
    val profileSubtitle: String = "Mode hors-ligne",
    val profileEmail: String = "",
    val profileInitials: String = "TL",
    val totalXp: Int = 2450,
    val level: Int = 12,
    val levelXp: Int = 620,
    val nextLevelXp: Int = 1000,
    val missionsStat: String = "28",
    val questsStat: String = "15",
    val streakStat: String = "56 j",
    val successStat: String = "93%",
    val xpHistoryTokens: List<String> = listOf("+40", "+20", "+10", "+5"),
    val pairedChildren: List<ParentChild> = emptyList(),
    val pairingCode: String? = null,
    val pairingCodeExpiresAt: String? = null,
    val isPairingBusy: Boolean = false,
    val pairingSuccessMessage: String? = null,
    val pairingErrorMessage: String? = null,
    val profileErrorMessage: String? = null,
)
