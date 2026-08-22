package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskTodayItem

data class FamilyTaskDetailUiState(
    val isLoading: Boolean = true,
    val task: FamilyTaskDefinition? = null,
    val todayOccurrence: FamilyTaskTodayItem? = null,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleted: Boolean = false,
)
