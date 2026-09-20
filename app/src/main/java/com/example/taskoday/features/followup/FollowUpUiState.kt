package com.example.taskoday.features.followup

import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.TaskForDay

data class FollowUpItem(
    val key: String,
    val title: String,
    val completed: Boolean,
    val overdue: Boolean = false,
    val familyTask: FamilyTaskTodayItem? = null,
    val legacyTask: TaskForDay? = null,
)

data class FollowUpMemberSummary(
    val memberId: Long,
    val displayName: String,
    val total: Int,
    val completed: Int,
    val remaining: Int,
    val overdue: Int,
    val items: List<FollowUpItem>,
)

data class FollowUpUiState(
    val isLoading: Boolean = true,
    val members: List<FollowUpMemberSummary> = emptyList(),
    val selectedMemberId: Long? = null,
    val house: FollowUpMemberSummary? = null,
    val errorMessage: String? = null,
)

fun familyTaskCompletionActorLabels(task: FamilyTaskTodayItem): List<String> {
    if (task.assignees.isNotEmpty() || !task.status.countsAsDone) return emptyList()
    val completed = task.completedByUser ?: return emptyList()
    val validated = task.validatedByUser
    return if (task.validationRequired && validated != null) {
        listOf("Faite par ${completed.displayName}", "Validée par ${validated.displayName}")
    } else {
        listOf("Terminée par ${completed.displayName}")
    }
}
