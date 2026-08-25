package com.example.taskoday.features.notifications

import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

data class FamilyNotificationSettings(
    val dailySummaryEnabled: Boolean = false,
    val hour: Int = DEFAULT_DAILY_SUMMARY_HOUR,
    val minute: Int = DEFAULT_DAILY_SUMMARY_MINUTE,
)

data class FamilyDailySummaryNotificationContent(
    val title: String,
    val text: String,
    val todayRemainingCount: Int,
    val overdueCount: Int,
)

fun buildFamilyDailySummaryNotificationContent(
    todayTasks: List<FamilyTaskTodayItem>,
    overdueTasks: List<FamilyTaskTodayItem>,
): FamilyDailySummaryNotificationContent? {
    val todayRemainingCount = todayTasks.count { task -> task.status.countsForParentDailySummary }
    val overdueCount = overdueTasks.size
    if (todayRemainingCount == 0 && overdueCount == 0) return null

    val title =
        if (todayRemainingCount > 0) {
            "Taskoday - Aujourd'hui"
        } else {
            "Taskoday - A verifier"
        }
    val text =
        when {
            todayRemainingCount > 0 && overdueCount > 0 ->
                "${familyTaskCountLabel(todayRemainingCount)} aujourd'hui · ${overdueCount} en retard"

            overdueCount > 0 ->
                "${familyTaskCountLabel(overdueCount)} ${if (overdueCount == 1) "est" else "sont"} en retard"

            else ->
                "${familyTaskCountLabel(todayRemainingCount)} ${if (todayRemainingCount == 1) "prevue" else "prevues"} aujourd'hui"
        }

    return FamilyDailySummaryNotificationContent(
        title = title,
        text = text,
        todayRemainingCount = todayRemainingCount,
        overdueCount = overdueCount,
    )
}

fun normalizedFamilyNotificationSettings(
    enabled: Boolean,
    hour: Int,
    minute: Int,
): FamilyNotificationSettings =
    FamilyNotificationSettings(
        dailySummaryEnabled = enabled,
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
    )

fun formatFamilyNotificationTime(
    hour: Int,
    minute: Int,
): String = "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

object FamilyNotificationSchedulePolicy {
    fun nextDailySummaryRun(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
    ): ZonedDateTime {
        val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val todayTarget = now.toLocalDate().atTime(targetTime).atZone(now.zone)
        return if (todayTarget.toInstant().isAfter(now.toInstant())) {
            todayTarget
        } else {
            now.toLocalDate().plusDays(1).atTime(targetTime).atZone(now.zone)
        }
    }

    fun delayUntilNextDailySummaryMillis(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
    ): Long =
        Duration
            .between(now.toInstant(), nextDailySummaryRun(now, hour, minute).toInstant())
            .toMillis()
            .coerceAtLeast(0L)
}

private val FamilyTaskStatus.countsForParentDailySummary: Boolean
    get() = this == FamilyTaskStatus.TODO || this == FamilyTaskStatus.PENDING_VALIDATION

private fun familyTaskCountLabel(count: Int): String = "$count ${if (count == 1) "tache" else "taches"}"

const val DEFAULT_DAILY_SUMMARY_HOUR: Int = 8
const val DEFAULT_DAILY_SUMMARY_MINUTE: Int = 0
