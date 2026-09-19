package com.example.taskoday.features.settings

import com.example.taskoday.BuildConfig
import com.example.taskoday.domain.model.ParentChild

data class SettingsUiState(
    val useDynamicColors: Boolean = true,
    val appVersionLabel: String = "Taskoday Beta · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    val isParentUser: Boolean = false,
    val familyIds: List<Long> = emptyList(),
    val selectedFamilyId: Long? = null,
    val profileName: String = "Profil",
    val profileSubtitle: String = "Chargement du profil...",
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
    val isChildManagementBusy: Boolean = false,
    val childManagementSuccessMessage: String? = null,
    val childManagementErrorMessage: String? = null,
    val hasParentPin: Boolean = false,
    val parentPinSuccessMessage: String? = null,
    val parentPinErrorMessage: String? = null,
    val profileErrorMessage: String? = null,
)
