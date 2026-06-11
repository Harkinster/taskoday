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
    val totalXp: Int = 0,
    val level: Int = 1,
    val levelXp: Int = 0,
    val nextLevelXp: Int = 1000,
    val missionsStat: String = "0/0",
    val questsStat: String = "0/0",
    val streakStat: String = "0 j",
    val successStat: String = "0%",
    val xpHistoryTokens: List<String> = emptyList(),
    val pairedChildren: List<ParentChild> = emptyList(),
    val activeChildId: Long? = null,
    val pairingCode: String? = null,
    val pairingCodeExpiresAt: String? = null,
    val isPairingBusy: Boolean = false,
    val pairingSuccessMessage: String? = null,
    val pairingErrorMessage: String? = null,
    val profileErrorMessage: String? = null,
)
