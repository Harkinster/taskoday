package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class FamilyTaskQuickAction {
    COMPLETE,
    VALIDATE,
    REOPEN,
}

data class FamilyTaskWeekWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<LocalDate>,
)

data class FamilyTaskDateWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

fun familyTaskUpcomingWindow(today: LocalDate): FamilyTaskDateWindow =
    FamilyTaskDateWindow(
        startDate = today.plusDays(1),
        endDate = today.plusDays(7),
    )

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

fun buildFamilyTaskOverduePreview(
    tasks: List<FamilyTaskTodayItem>,
    previewLimit: Int = FAMILY_TASK_OVERDUE_PREVIEW_LIMIT,
): List<FamilyTaskRow> =
    tasks
        .sortedWith(familyTaskOccurrenceComparator())
        .take(previewLimit.coerceAtLeast(0))
        .map { task -> FamilyTaskRow(task) }

fun buildFamilyTaskUpcomingSections(
    tasks: List<FamilyTaskTodayItem>,
    today: LocalDate,
    previewLimit: Int = FAMILY_TASK_UPCOMING_PREVIEW_LIMIT,
): List<FamilyTaskUpcomingDaySection> {
    val window = familyTaskUpcomingWindow(today)
    return tasks
        .mapNotNull { task ->
            val date = task.occurrenceLocalDate() ?: return@mapNotNull null
            if (date !in window.startDate..window.endDate) return@mapNotNull null
            date to task
        }
        .sortedWith(compareBy<Pair<LocalDate, FamilyTaskTodayItem>> { it.first }.thenBy { it.second.timedSortKey() }.thenBy { it.second.title })
        .take(previewLimit.coerceAtLeast(0))
        .groupBy { it.first }
        .map { (date, datedTasks) ->
            FamilyTaskUpcomingDaySection(
                date = date.toString(),
                label = familyTaskUpcomingDayLabel(date = date, today = today),
                tasks = datedTasks.map { (_, task) -> FamilyTaskRow(task) },
            )
        }
}

fun hasMoreFamilyTaskUpcomingPreview(
    tasks: List<FamilyTaskTodayItem>,
    today: LocalDate,
    previewLimit: Int = FAMILY_TASK_UPCOMING_PREVIEW_LIMIT,
): Boolean {
    val window = familyTaskUpcomingWindow(today)
    val eligibleCount =
        tasks.count { task ->
            val date = task.occurrenceLocalDate()
            date != null && date in window.startDate..window.endDate
        }
    return eligibleCount > previewLimit
}

fun familyTaskOverdueMetaLabel(
    task: FamilyTaskTodayItem,
    today: LocalDate,
): String {
    val dateLabel =
        task.occurrenceLocalDate()
            ?.let { date ->
                if (date == today.minusDays(1)) {
                    "Hier"
                } else {
                    formatFamilyTaskDateLabel(date.toString())
                }
            }
            ?: "Date à vérifier"
    val time = task.occurrenceTimeLabel()
    val dateAndTime = if (time == null) dateLabel else "$dateLabel à $time"
    return listOf(dateAndTime, familyTaskAssignmentLabel(task)).joinToString(" · ")
}

fun familyTaskUpcomingMetaLabel(task: FamilyTaskTodayItem): String {
    val time = task.occurrenceTimeLabel()
    return listOfNotNull(time, familyTaskAssignmentLabel(task)).joinToString(" · ")
}

fun familyTaskAssignmentLabel(task: FamilyTaskTodayItem): String =
    task.assignees
        .takeIf { it.isNotEmpty() }
        ?.joinToString { assignee -> assignee.displayName }
        ?: HOUSE_LABEL

fun familyTaskPendingValidationCount(tasks: List<FamilyTaskTodayItem>): Int =
    tasks.count { task -> task.status == FamilyTaskStatus.PENDING_VALIDATION }

fun familyTaskWeekWindowContaining(date: LocalDate): FamilyTaskWeekWindow {
    val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..6L).map { offset -> start.plusDays(offset) }
    return FamilyTaskWeekWindow(
        startDate = start,
        endDate = start.plusDays(6L),
        days = days,
    )
}

fun familyTaskWeekRangeLabel(
    startDate: LocalDate,
    endDate: LocalDate,
): String {
    val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRANCE)
    val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE)
    return when {
        startDate.year != endDate.year ->
            "${startDate.format(dayMonthYearFormatter)} - ${endDate.format(dayMonthYearFormatter)}"
        startDate.month != endDate.month ->
            "${startDate.format(dayMonthFormatter)} - ${endDate.format(dayMonthFormatter)}"
        else ->
            "${startDate.dayOfMonth} - ${endDate.format(dayMonthFormatter)}"
    }
}

fun buildFamilyTaskWeekDaySummaries(
    days: List<LocalDate>,
    tasks: List<FamilyTaskTodayItem>,
    selectedDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): List<FamilyTaskWeekDaySummary> {
    val formatter = DateTimeFormatter.ofPattern("E", Locale.FRANCE)
    return days.map { date ->
        val dayTasks = familyTasksForDate(tasks = tasks, date = date)
        FamilyTaskWeekDaySummary(
            date = date.toString(),
            weekdayLabel = date.format(formatter).take(1).uppercase(Locale.FRANCE),
            dayNumberLabel = date.dayOfMonth.toString(),
            completedCount = dayTasks.count { task -> task.status.countsAsDone },
            totalCount = dayTasks.size,
            isSelected = date == selectedDate,
            isToday = date == today,
        )
    }
}

fun familyTasksForDate(
    tasks: List<FamilyTaskTodayItem>,
    date: LocalDate,
): List<FamilyTaskTodayItem> =
    tasks.filter { task -> task.occurrenceDateKey() == date.toString() }

fun familyTaskWeekIsEmpty(tasks: List<FamilyTaskTodayItem>): Boolean =
    tasks.isEmpty()

fun resolveFamilyTaskSelectedWeekDate(
    preferredDate: LocalDate?,
    weekStartDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): LocalDate {
    val week = familyTaskWeekWindowContaining(weekStartDate)
    return when {
        preferredDate != null && preferredDate in week.startDate..week.endDate -> preferredDate
        today in week.startDate..week.endDate -> today
        else -> week.startDate
    }
}

fun familyTaskCreationDateForMode(
    mode: FamilyHomeMode,
    selectedWeekDate: String?,
    todayDate: String?,
): String? =
    when (mode) {
        FamilyHomeMode.TODAY -> todayDate?.takeIf { value -> parseFamilyTaskDateInput(value) != null }
        FamilyHomeMode.WEEK -> selectedWeekDate?.takeIf { value -> parseFamilyTaskDateInput(value) != null }
    }

fun familyTaskOccurrenceRecurrenceLabel(value: String?): String? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return when (normalized.replace("-", "_").replace(" ", "_").uppercase(Locale.US)) {
        "NONE", "ONCE", "ONE_SHOT", "SINGLE" -> null
        "DAILY" -> "Tous les jours"
        "WEEKLY" -> "Chaque semaine"
        "SELECTED_WEEKDAYS" -> "Certains jours"
        else -> normalized
    }
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

fun formatFamilyHomeSelectedDayLabel(date: LocalDate): String =
    formatFamilyHomeDateLabel(date.toString(), fallback = date)

fun familyTaskUpcomingDayLabel(
    date: LocalDate,
    today: LocalDate,
): String =
    if (date == today.plusDays(1)) {
        "Demain"
    } else {
        formatFamilyHomeDateLabel(date.toString(), fallback = date)
    }

private const val HOUSE_LABEL = "Maison"
private const val FAMILY_TASK_OVERDUE_PREVIEW_LIMIT = 4
private const val FAMILY_TASK_UPCOMING_PREVIEW_LIMIT = 5

private fun FamilyTaskAssignee.sectionKey(): String =
    id?.let { "member_$it" } ?: "house_${displayName.lowercase(Locale.FRANCE)}"

private fun familyTaskOccurrenceComparator(): Comparator<FamilyTaskTodayItem> =
    compareBy<FamilyTaskTodayItem> { it.occurrenceDateKey().orEmpty() }
        .thenBy { it.timedSortKey() }
        .thenBy { it.title }

private fun FamilyTaskTodayItem.sortKey(): String =
    buildString {
        append(occurrenceDateKey().orEmpty())
        append(" ")
        append(
            familyTaskTimeFromFields(
                hasDueTime = hasDueTime,
                dueTime = dueTime,
                dueAt = dueAt,
            ),
        )
    }

private fun FamilyTaskTodayItem.occurrenceDateKey(): String? =
    scheduledDate
        ?.trim()
        ?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        ?: familyTaskDateFromFields(dueDate = dueDate, dueAt = dueAt)

private fun FamilyTaskTodayItem.occurrenceLocalDate(): LocalDate? =
    occurrenceDateKey()?.let { value -> parseFamilyTaskDateInput(value) }

private fun FamilyTaskTodayItem.occurrenceTimeLabel(): String? =
    familyTaskTimeFromFields(
        hasDueTime = hasDueTime,
        dueTime = dueTime,
        dueAt = dueAt,
    ).takeIf { it.isNotBlank() }

private fun FamilyTaskTodayItem.timedSortKey(): String =
    occurrenceTimeLabel() ?: "99:99"
