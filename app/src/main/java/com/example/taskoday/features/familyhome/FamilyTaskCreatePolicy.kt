package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class FamilyTaskCreateForm(
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val recurrence: FamilyTaskRecurrence,
    val selectedWeekdays: Set<Int>,
    val assigneeUserIds: Set<Long>,
    val validationRequired: Boolean,
    val gamificationEnabled: Boolean,
    val priority: FamilyTaskPriority,
)

data class FamilyTaskCreateValidation(
    val input: FamilyTaskCreateInput? = null,
    val errorMessage: String? = null,
) {
    val isValid: Boolean
        get() = input != null && errorMessage == null
}

fun validateFamilyTaskCreateForm(form: FamilyTaskCreateForm): FamilyTaskCreateValidation {
    val title = form.title.trim()
    if (title.isBlank()) {
        return FamilyTaskCreateValidation(errorMessage = "Le titre est obligatoire.")
    }

    if (form.recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS && form.selectedWeekdays.isEmpty()) {
        return FamilyTaskCreateValidation(errorMessage = "Choisis au moins un jour.")
    }

    val dueAt =
        buildFamilyTaskDueAt(
            dateText = form.date,
            timeText = form.time,
        ).getOrElse { return FamilyTaskCreateValidation(errorMessage = it.message ?: "Date invalide.") }

    return FamilyTaskCreateValidation(
        input =
            FamilyTaskCreateInput(
                title = title,
                description = form.description.trim().takeIf { it.isNotBlank() },
                dueAt = dueAt,
                recurrence = form.recurrence,
                selectedWeekdays = form.selectedWeekdays.sorted(),
                assigneeUserIds = form.assigneeUserIds.sorted(),
                validationRequired = form.validationRequired,
                gamificationEnabled = form.gamificationEnabled,
                priority =
                    form.priority.takeUnless { it == FamilyTaskPriority.UNKNOWN }
                        ?: FamilyTaskPriority.NORMAL,
            ),
    )
}

fun buildFamilyTaskDueAt(
    dateText: String,
    timeText: String,
): Result<String> =
    runCatching {
        val date =
            try {
                LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (error: DateTimeParseException) {
                throw IllegalArgumentException("Date invalide. Utilise AAAA-MM-JJ.")
            }
        val time =
            if (timeText.isBlank()) {
                LocalTime.MIDNIGHT
            } else {
                try {
                    LocalTime.parse(timeText.trim(), DateTimeFormatter.ofPattern("HH:mm"))
                } catch (error: DateTimeParseException) {
                    throw IllegalArgumentException("Heure invalide. Utilise HH:mm.")
                }
            }
        OffsetDateTime.of(date, time, ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

fun familyTaskRecurrenceLabel(recurrence: FamilyTaskRecurrence): String =
    when (recurrence) {
        FamilyTaskRecurrence.NONE -> "Jamais"
        FamilyTaskRecurrence.DAILY -> "Tous les jours"
        FamilyTaskRecurrence.WEEKLY -> "Chaque semaine"
        FamilyTaskRecurrence.SELECTED_WEEKDAYS -> "Certains jours"
    }

fun familyTaskPriorityFormLabel(priority: FamilyTaskPriority): String =
    when (priority) {
        FamilyTaskPriority.LOW -> "Basse"
        FamilyTaskPriority.NORMAL,
        FamilyTaskPriority.UNKNOWN,
        -> "Normale"
        FamilyTaskPriority.HIGH -> "Haute"
        FamilyTaskPriority.URGENT -> "Urgente"
    }

val FamilyTaskPriority.allowedCreatePriorities: Boolean
    get() = this != FamilyTaskPriority.UNKNOWN

val familyTaskWeekdays: List<Pair<Int, String>> =
    listOf(
        1 to "Lundi",
        2 to "Mardi",
        3 to "Mercredi",
        4 to "Jeudi",
        5 to "Vendredi",
        6 to "Samedi",
        7 to "Dimanche",
    )
