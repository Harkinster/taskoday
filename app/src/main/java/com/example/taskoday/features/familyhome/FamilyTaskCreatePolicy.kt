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
import java.util.Locale

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

    val dueDate =
        buildFamilyTaskDueDate(
            dateText = form.date,
        ).getOrElse { return FamilyTaskCreateValidation(errorMessage = it.message ?: "Date invalide.") }
    val dueTime =
        buildFamilyTaskDueTime(
            timeText = form.time,
        ).getOrElse { return FamilyTaskCreateValidation(errorMessage = it.message ?: "Heure invalide.") }

    return FamilyTaskCreateValidation(
        input =
            FamilyTaskCreateInput(
                title = title,
                description = form.description.trim().takeIf { it.isNotBlank() },
                dueDate = dueDate,
                dueTime = dueTime,
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

fun buildFamilyTaskDueDate(dateText: String): Result<String> =
    runCatching {
        try {
            LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE).toString()
        } catch (error: DateTimeParseException) {
            throw IllegalArgumentException("Date invalide. Utilise AAAA-MM-JJ.")
        }
    }

fun buildFamilyTaskDueTime(timeText: String): Result<String?> =
    runCatching {
        if (timeText.isBlank()) {
            null
        } else {
            try {
                LocalTime.parse(timeText.trim(), DateTimeFormatter.ofPattern("HH:mm")).format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (error: DateTimeParseException) {
                throw IllegalArgumentException("Heure invalide. Utilise HH:mm.")
            }
        }
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

fun parseFamilyTaskDateInput(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()

fun parseFamilyTaskTimeInput(value: String): LocalTime? =
    value
        .trim()
        .takeIf { it.isNotBlank() }
        ?.let { text -> runCatching { LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() }

fun familyTaskDateFromDueAt(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return trimmed.substringBefore("T").takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
}

fun familyTaskTimeFromDueAt(value: String?): String {
    val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return ""
    val timePart =
        trimmed
            .substringAfter("T", missingDelimiterValue = trimmed)
            .substringBefore("Z")
            .substringBefore("+")
    return timePart.takeIf { it.length >= 5 }?.take(5).orEmpty()
}

fun familyTaskDateFromFields(
    dueDate: String?,
    dueAt: String?,
): String? =
    dueDate
        ?.trim()
        ?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        ?: familyTaskDateFromDueAt(dueAt)

fun familyTaskTimeFromFields(
    hasDueTime: Boolean,
    dueTime: String?,
    dueAt: String?,
): String =
    if (hasDueTime) {
        dueTime
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { time -> familyTaskTimeFromDueAt(time) }
            ?: familyTaskTimeFromDueAt(dueAt)
    } else {
        ""
    }

fun familyTaskDueLabel(
    dueDate: String?,
    dueTime: String?,
    hasDueTime: Boolean,
    dueAt: String?,
): String? {
    val dateLabel =
        familyTaskDateFromFields(dueDate = dueDate, dueAt = dueAt)
            ?.let { date -> formatFamilyTaskDateLabel(date) }
            ?: return null
    val timeLabel =
        familyTaskTimeFromFields(
            hasDueTime = hasDueTime,
            dueTime = dueTime,
            dueAt = dueAt,
        ).takeIf { it.isNotBlank() }
    return if (timeLabel == null) dateLabel else "$dateLabel à $timeLabel"
}

fun formatFamilyTaskDateLabel(value: String): String =
    parseFamilyTaskDateInput(value)?.let { date ->
        date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.FRANCE))
    } ?: "Choisir une date"

fun formatFamilyTaskTimeLabel(value: String): String =
    parseFamilyTaskTimeInput(value)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Sans heure"

fun familyTaskRecurrenceLabel(recurrence: FamilyTaskRecurrence): String =
    when (recurrence) {
        FamilyTaskRecurrence.NONE -> "Jamais"
        FamilyTaskRecurrence.DAILY -> "Tous les jours"
        FamilyTaskRecurrence.WEEKLY -> "Chaque semaine"
        FamilyTaskRecurrence.SELECTED_WEEKDAYS -> "Certains jours"
    }

fun familyTaskRecurrenceSummary(
    recurrence: FamilyTaskRecurrence,
    selectedWeekdays: List<Int>,
): String =
    when (recurrence) {
        FamilyTaskRecurrence.NONE -> "Une seule fois"
        FamilyTaskRecurrence.DAILY -> "Tous les jours"
        FamilyTaskRecurrence.WEEKLY -> "Chaque semaine"
        FamilyTaskRecurrence.SELECTED_WEEKDAYS ->
            selectedWeekdays
                .mapNotNull { day -> familyTaskShortWeekdayLabels[day] }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" • ")
                ?: "Certains jours"
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

private val familyTaskShortWeekdayLabels: Map<Int, String> =
    mapOf(
        1 to "Lun.",
        2 to "Mar.",
        3 to "Mer.",
        4 to "Jeu.",
        5 to "Ven.",
        6 to "Sam.",
        7 to "Dim.",
    )
