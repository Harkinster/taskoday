package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.LocalDate

data class FamilyTaskCreateUiState(
    val isLoadingMembers: Boolean = true,
    val members: List<FamilyTaskMember> = emptyList(),
    val title: String = "",
    val description: String = "",
    val date: String = LocalDate.now().toString(),
    val time: String = "",
    val recurrence: FamilyTaskRecurrence = FamilyTaskRecurrence.NONE,
    val selectedWeekdays: Set<Int> = emptySet(),
    val selectedAssigneeUserIds: Set<Long> = emptySet(),
    val validationRequired: Boolean = false,
    val gamificationEnabled: Boolean = false,
    val priority: FamilyTaskPriority = FamilyTaskPriority.NORMAL,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val created: Boolean = false,
) {
    val isHouseTask: Boolean
        get() = selectedAssigneeUserIds.isEmpty()
}
