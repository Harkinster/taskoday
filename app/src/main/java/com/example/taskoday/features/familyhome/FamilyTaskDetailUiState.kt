package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskTodayItem

data class FamilyTaskDetailUiState(
    val access: FamilyTaskAccessPolicy = FamilyTaskAccessPolicy(),
    val isLoading: Boolean = true,
    val task: FamilyTaskDefinition? = null,
    val todayOccurrence: FamilyTaskTodayItem? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isValidating: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleted: Boolean = false,
) {
    val canValidateToday: Boolean
        get() = todayOccurrence?.let { occurrence ->
            occurrence.occurrenceId > 0L && access.quickAction(occurrence) == FamilyTaskQuickAction.VALIDATE
        } == true
}
