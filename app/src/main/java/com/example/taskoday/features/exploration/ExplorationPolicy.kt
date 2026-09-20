package com.example.taskoday.features.exploration

import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyTaskTodayItem

enum class ExplorationCategory {
    PERSONAL_TASK,
    PERSONAL_ROUTINE,
    HOUSEHOLD,
}

fun FamilyTaskTodayItem.isHouseholdTask(): Boolean = assignees.isEmpty()

fun FamilyTaskTodayItem.isPersonalTask(): Boolean = assignees.isNotEmpty()

fun FamilyTaskTodayItem.isRecurringTask(): Boolean = recurrenceLabel
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?.let { !it.equals("NONE", ignoreCase = true) && !it.equals("ONCE", ignoreCase = true) }
    ?: false

fun FamilyTaskTodayItem.explorationCategory(): ExplorationCategory = when {
    isHouseholdTask() -> ExplorationCategory.HOUSEHOLD
    isRecurringTask() -> ExplorationCategory.PERSONAL_ROUTINE
    else -> ExplorationCategory.PERSONAL_TASK
}

fun familyTaskRecurrenceLabel(recurrence: FamilyTaskRecurrence, selectedWeekdays: List<Int> = emptyList()): String? =
    when (recurrence) {
        FamilyTaskRecurrence.NONE -> null
        FamilyTaskRecurrence.DAILY -> "Tous les jours"
        FamilyTaskRecurrence.WEEKLY -> "Chaque semaine"
        FamilyTaskRecurrence.SELECTED_WEEKDAYS -> selectedWeekdays.mapNotNull {
            mapOf(1 to "Lun", 2 to "Mar", 3 to "Mer", 4 to "Jeu", 5 to "Ven", 6 to "Sam", 7 to "Dim")[it]
        }.joinToString(" ").ifBlank { "Certains jours" }
    }

fun familyTaskOccurrenceRecurrenceLabel(value: String?): String? = when (value?.trim()?.uppercase()) {
    null, "", "NONE", "ONCE", "ONE_SHOT", "SINGLE" -> null
    "DAILY" -> "Tous les jours"
    "WEEKLY" -> "Chaque semaine"
    "SELECTED_WEEKDAYS", "WEEKDAYS", "CUSTOM_WEEKDAYS" -> "Certains jours"
    else -> value
}
