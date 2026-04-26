package com.example.taskoday.features.quests

import com.example.taskoday.domain.model.QuestForDay

data class QuestsUiState(
    val selectedDayStartMillis: Long = 0L,
    val dateLabel: String = "",
    val pointsBalance: Int = 0,
    val quests: List<QuestForDay> = emptyList(),
    val canCreateQuest: Boolean = false,
    val canManageQuests: Boolean = false,
    val isSubmittingQuest: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
