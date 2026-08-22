package com.example.taskoday.domain.model

import java.util.Locale

data class FamilyTasksToday(
    val familyId: Long,
    val date: String?,
    val tasks: List<FamilyTaskTodayItem>,
)

data class FamilyTaskTodayItem(
    val taskId: Long,
    val occurrenceId: Long,
    val title: String,
    val assignees: List<FamilyTaskAssignee>,
    val scheduledDate: String?,
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

private fun String?.normalizedBackendKey(): String =
    this
        ?.trim()
        ?.replace("-", "_")
        ?.replace(" ", "_")
        ?.uppercase(Locale.US)
        .orEmpty()
