package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.FamilyActionKind
import com.example.taskoday.domain.model.FamilyTaskStatus
import java.time.LocalDate

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
    val canCompleteToday: Boolean
        get() = todayOccurrence?.let { occurrence ->
            occurrence.occurrenceId > 0L && access.quickAction(occurrence) == FamilyTaskQuickAction.COMPLETE
        } == true

    val canValidateToday: Boolean
        get() = todayOccurrence?.let { occurrence ->
            occurrence.occurrenceId > 0L && access.quickAction(occurrence) == FamilyTaskQuickAction.VALIDATE
        } == true

    val canManageOverdueMission: Boolean
        get() = access.canManage && task?.kind == FamilyActionKind.MISSION && todayOccurrence?.let { occurrence ->
            occurrence.status == FamilyTaskStatus.TODO &&
                occurrence.dueDate?.let { due -> runCatching { LocalDate.parse(due) < LocalDate.now() }.getOrDefault(false) } == true
        } == true
}
