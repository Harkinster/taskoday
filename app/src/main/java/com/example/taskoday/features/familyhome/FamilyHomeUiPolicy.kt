package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FamilyTaskQuickAction {
    COMPLETE,
    VALIDATE,
    REOPEN,
}

fun buildFamilyTaskSections(tasks: List<FamilyTaskTodayItem>): List<FamilyTaskMemberSection> {
    val house = FamilyTaskAssignee(id = null, displayName = HOUSE_LABEL)
    val grouped =
        tasks
            .flatMap { task ->
                val assignees = task.assignees.ifEmpty { listOf(house) }
                assignees.map { assignee -> assignee to FamilyTaskRow(task) }
            }
            .groupBy { (assignee, _) -> assignee.sectionKey() }

    return grouped
        .map { (key, rows) ->
            val name = rows.first().first.displayName
            val sectionTasks = rows.map { it.second }
            FamilyTaskMemberSection(
                key = key,
                name = name,
                completedCount = sectionTasks.count { row -> row.task.status.countsAsDone },
                totalCount = sectionTasks.size,
                tasks = sectionTasks.sortedWith(compareBy<FamilyTaskRow> { it.task.sortKey() }.thenBy { it.task.title }),
            )
        }
        .sortedWith(compareBy<FamilyTaskMemberSection> { if (it.name == HOUSE_LABEL) 0 else 1 }.thenBy { it.name.lowercase(Locale.FRANCE) })
}

fun familyTaskStatusLabel(status: FamilyTaskStatus): String =
    when (status) {
        FamilyTaskStatus.TODO -> "À faire"
        FamilyTaskStatus.COMPLETED -> "Terminée"
        FamilyTaskStatus.PENDING_VALIDATION -> "En attente de validation"
        FamilyTaskStatus.VALIDATED -> "Validée"
        FamilyTaskStatus.SKIPPED -> "Ignorée"
        FamilyTaskStatus.UNKNOWN -> "À suivre"
    }

fun familyTaskPriorityLabel(priority: FamilyTaskPriority): String? =
    when (priority) {
        FamilyTaskPriority.LOW -> "Priorité basse"
        FamilyTaskPriority.NORMAL -> null
        FamilyTaskPriority.HIGH -> "Prioritaire"
        FamilyTaskPriority.URGENT -> "Urgent"
        FamilyTaskPriority.UNKNOWN -> null
    }

fun quickActionFor(task: FamilyTaskTodayItem): FamilyTaskQuickAction? =
    when (task.status) {
        FamilyTaskStatus.TODO -> FamilyTaskQuickAction.COMPLETE
        FamilyTaskStatus.PENDING_VALIDATION -> FamilyTaskQuickAction.VALIDATE
        FamilyTaskStatus.COMPLETED,
        FamilyTaskStatus.VALIDATED,
        -> FamilyTaskQuickAction.REOPEN
        FamilyTaskStatus.SKIPPED,
        FamilyTaskStatus.UNKNOWN,
        -> null
    }

fun familyTaskActionLabel(action: FamilyTaskQuickAction): String =
    when (action) {
        FamilyTaskQuickAction.COMPLETE -> "Terminer"
        FamilyTaskQuickAction.VALIDATE -> "Valider"
        FamilyTaskQuickAction.REOPEN -> "Rouvrir"
    }

fun canRunQuickAction(task: FamilyTaskTodayItem): Boolean =
    task.occurrenceId > 0L && quickActionFor(task) != null

fun formatFamilyHomeDateLabel(
    date: String?,
    fallback: LocalDate = LocalDate.now(),
): String {
    val parsed =
        date
            ?.trim()
            ?.take(10)
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
            ?: fallback
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
    return parsed.format(formatter).replaceFirstChar { char -> char.titlecase(Locale.FRANCE) }
}

private const val HOUSE_LABEL = "Maison"

private fun FamilyTaskAssignee.sectionKey(): String =
    id?.let { "member_$it" } ?: "house_${displayName.lowercase(Locale.FRANCE)}"

private fun FamilyTaskTodayItem.sortKey(): String =
    buildString {
        append(familyTaskDateFromFields(dueDate = dueDate, dueAt = dueAt).orEmpty())
        append(" ")
        append(
            familyTaskTimeFromFields(
                hasDueTime = hasDueTime,
                dueTime = dueTime,
                dueAt = dueAt,
            ),
        )
    }
