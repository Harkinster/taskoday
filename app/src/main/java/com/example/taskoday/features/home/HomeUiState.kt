package com.example.taskoday.features.home

import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.QuestForDay

data class HomeUiState(
    val selectedDayStartMillis: Long = 0L,
    val dateLabel: String = "",
    val tasksForDay: List<TaskForDay> = emptyList(),
    val questsForDay: List<QuestForDay> = emptyList(),
    val pointsBalance: Int = 0,
    val usingRemoteData: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
