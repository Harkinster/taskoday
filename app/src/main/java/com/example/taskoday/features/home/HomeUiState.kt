package com.example.taskoday.features.home

import com.example.taskoday.domain.model.CompletionReward
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay

data class CompletionFeedback(
    val actionTitle: String,
    val reward: CompletionReward?,
)

data class HomeUiState(
    val selectedDayStartMillis: Long = 0L,
    val dateLabel: String = "",
    val tasksForDay: List<TaskForDay> = emptyList(),
    val questsForDay: List<QuestForDay> = emptyList(),
    val pointsBalance: Int = 0,
    val remoteXp: Int? = null,
    val remoteFlammeches: Int? = null,
    val remoteCrystals: Int? = null,
    val usingRemoteData: Boolean = false,
    val pendingCompletionKeys: Set<String> = emptySet(),
    val completionFeedback: CompletionFeedback? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
