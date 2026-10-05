package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Shared safe prefill for explicit duplication and contextual recent actions. */
internal fun reuseTaskPrefill(
    state: FamilyTaskCreateUiState,
    source: FamilyTaskDefinition,
    familyId: Long,
    members: List<FamilyTaskMember>,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): FamilyTaskCreateUiState {
    require(source.active && source.familyId == familyId) { "Cette action n'appartient plus à la famille active." }
    val type = FamilyActionType.fromCategory(source.category)
    require(type == state.actionType) { "Le type de cette action ne correspond pas à cet espace." }
    val sourceIds = source.assignees.mapNotNull { it.id }.toSet()
    val validIds = members.filter { it.isActive }.map { it.userId }.toSet()
    val assignees = sourceIds.intersect(validIds)
    val invalidAssignment = assignees.size != sourceIds.size || (type != FamilyActionType.HOUSE_QUEST && assignees.size != 1)
    val time = familyTaskTimeFromFields(source.hasDueTime, source.dueTime, source.dueAt)
    val earliest = if (time.isNotBlank() && parseFamilyTaskTimeInput(time)?.isBefore(now) == true) today.plusDays(1) else today
    val start = familyTaskDateFromFields(source.dueDate, source.dueAt)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val date = when (source.recurrence) {
        FamilyTaskRecurrence.NONE -> earliest
        FamilyTaskRecurrence.DAILY -> earliest
        FamilyTaskRecurrence.WEEKLY -> nextMatchingDate(earliest) { it.dayOfWeek == (start ?: today).dayOfWeek }
        FamilyTaskRecurrence.SELECTED_WEEKDAYS -> nextMatchingDate(earliest) { it.dayOfWeek.value in source.selectedWeekdays }
    }
    return state.copy(
        taskId = null,
        title = source.title,
        description = source.description.orEmpty(),
        date = date.toString(),
        time = time,
        recurrence = source.recurrence,
        recurrenceInterval = source.recurrenceInterval.coerceIn(1, 52),
        isCustomRecurrence = source.recurrenceInterval > 1,
        customRecurrenceUnit = if (source.recurrence == FamilyTaskRecurrence.DAILY) CustomRecurrenceUnit.DAYS else CustomRecurrenceUnit.WEEKS,
        selectedWeekdays = source.selectedWeekdays.toSet(),
        selectedAssigneeUserIds = assignees,
        validationRequired = source.validationRequired,
        gamificationEnabled = source.gamificationEnabled,
        priority = source.priority,
        prefillWarning = if (invalidAssignment) "Un participant n'est plus disponible. Vérifiez l'attribution avant de créer." else null,
        requiresAssigneeReview = invalidAssignment,
        reusedSourceId = source.id,
        errorMessage = null,
    )
}

private fun nextMatchingDate(today: LocalDate, matches: (LocalDate) -> Boolean): LocalDate =
    (0L..7L).map(today::plusDays).firstOrNull(matches) ?: today

internal fun recentTasksForContext(
    definitions: List<FamilyTaskDefinition>,
    familyId: Long,
    type: FamilyActionType,
    memberId: Long?,
    limit: Int = 3,
): List<FamilyTaskDefinition> {
    if (type == FamilyActionType.PERSONAL_ROUTINE) return emptyList()
    return definitions.asSequence()
        .filter { it.active && it.familyId == familyId }
        .filter { runCatching { FamilyActionType.fromCategory(it.category) }.getOrNull() == type }
        .filter { type == FamilyActionType.HOUSE_QUEST || (memberId != null && it.assignees.mapNotNull { assignee -> assignee.id }.toSet() == setOf(memberId)) }
        .mapNotNull { task -> task.createdAt?.let(::createdAtInstant)?.let { task to it } }
        .sortedByDescending { it.second }
        .take(limit.coerceIn(0, 5))
        .map { it.first }
        .toList()
}

private fun createdAtInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }.getOrNull()
