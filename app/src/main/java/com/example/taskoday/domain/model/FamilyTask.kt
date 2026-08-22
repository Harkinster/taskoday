package com.example.taskoday.domain.model

import java.util.Locale

data class FamilyTasksToday(
    val familyId: Long,
    val date: String?,
    val tasks: List<FamilyTaskTodayItem>,
)

data class FamilyTaskOccurrencesRange(
    val familyId: Long,
    val startDate: String,
    val endDate: String,
    val occurrences: List<FamilyTaskTodayItem>,
)

data class FamilyTaskMember(
    val userId: Long,
    val displayName: String,
    val email: String?,
    val role: FamilyTaskMemberRole,
    val isActive: Boolean,
)

enum class FamilyTaskMemberRole {
    PARENT,
    CHILD,
}

data class FamilyTaskCreateInput(
    val title: String,
    val description: String?,
    val dueDate: String,
    val dueTime: String?,
    val recurrence: FamilyTaskRecurrence,
    val selectedWeekdays: List<Int>,
    val assigneeUserIds: List<Long>,
    val validationRequired: Boolean,
    val gamificationEnabled: Boolean,
    val priority: FamilyTaskPriority,
)

data class FamilyTaskDefinition(
    val id: Long,
    val familyId: Long,
    val title: String,
    val description: String?,
    val assignees: List<FamilyTaskAssignee>,
    val dueDate: String?,
    val dueTime: String?,
    val hasDueTime: Boolean,
    val dueAt: String?,
    val recurrence: FamilyTaskRecurrence,
    val selectedWeekdays: List<Int>,
    val validationRequired: Boolean,
    val gamificationEnabled: Boolean,
    val priority: FamilyTaskPriority,
    val active: Boolean,
)

data class FamilyTaskTodayItem(
    val taskId: Long,
    val occurrenceId: Long,
    val title: String,
    val assignees: List<FamilyTaskAssignee>,
    val scheduledDate: String?,
    val dueDate: String?,
    val dueTime: String?,
    val hasDueTime: Boolean,
    val dueAt: String?,
    val status: FamilyTaskStatus,
    val validationRequired: Boolean,
    val gamificationEnabled: Boolean,
    val priority: FamilyTaskPriority,
    val recurrenceLabel: String? = null,
)

data class FamilyTaskAssignee(
    val id: Long?,
    val displayName: String,
)

enum class FamilyTaskStatus {
    TODO,
    COMPLETED,
    PENDING_VALIDATION,
    VALIDATED,
    SKIPPED,
    UNKNOWN,
    ;

    val countsAsDone: Boolean
        get() = this == COMPLETED || this == VALIDATED

    companion object {
        fun fromBackend(value: String?): FamilyTaskStatus {
            val normalized = value.normalizedBackendKey()
            return when (normalized) {
                "TODO", "TO_DO", "OPEN", "PLANNED", "PENDING" -> TODO
                "COMPLETED", "COMPLETE", "DONE", "FINISHED" -> COMPLETED
                "PENDING_VALIDATION", "AWAITING_VALIDATION", "WAITING_VALIDATION" -> PENDING_VALIDATION
                "VALIDATED", "APPROVED" -> VALIDATED
                "SKIPPED", "IGNORED", "CANCELLED", "CANCELED" -> SKIPPED
                else -> UNKNOWN
            }
        }
    }
}

enum class FamilyTaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
    UNKNOWN,
    ;

    companion object {
        fun fromBackend(value: String?): FamilyTaskPriority {
            val normalized = value.normalizedBackendKey()
            return when (normalized) {
                "LOW" -> LOW
                "NORMAL", "MEDIUM", "DEFAULT" -> NORMAL
                "HIGH" -> HIGH
                "URGENT", "CRITICAL" -> URGENT
                else -> UNKNOWN
            }
        }
    }
}

enum class FamilyTaskRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    SELECTED_WEEKDAYS,
    ;

    companion object {
        fun fromBackend(value: String?): FamilyTaskRecurrence {
            val normalized = value.normalizedBackendKey()
            return when (normalized) {
                "DAILY" -> DAILY
                "WEEKLY" -> WEEKLY
                "SELECTED_WEEKDAYS", "WEEKDAYS", "CUSTOM_WEEKDAYS" -> SELECTED_WEEKDAYS
                else -> NONE
            }
        }
    }
}

private fun String?.normalizedBackendKey(): String =
    this
        ?.trim()
        ?.replace("-", "_")
        ?.replace(" ", "_")
        ?.uppercase(Locale.US)
        .orEmpty()
