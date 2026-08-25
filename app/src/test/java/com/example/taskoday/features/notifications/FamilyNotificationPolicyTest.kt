package com.example.taskoday.features.notifications

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyNotificationPolicyTest {
    @Test
    fun `zero today and zero overdue does not create notification`() {
        assertNull(buildFamilyDailySummaryNotificationContent(todayTasks = emptyList(), overdueTasks = emptyList()))
    }

    @Test
    fun `one today task uses singular`() {
        val content =
            buildFamilyDailySummaryNotificationContent(
                todayTasks = listOf(task(status = FamilyTaskStatus.TODO)),
                overdueTasks = emptyList(),
            )

        assertEquals("Taskoday - Aujourd'hui", content?.title)
        assertEquals("1 tache prevue aujourd'hui", content?.text)
    }

    @Test
    fun `multiple today tasks use plural`() {
        val content =
            buildFamilyDailySummaryNotificationContent(
                todayTasks =
                    listOf(
                        task(status = FamilyTaskStatus.TODO),
                        task(status = FamilyTaskStatus.PENDING_VALIDATION),
                    ),
                overdueTasks = emptyList(),
            )

        assertEquals(2, content?.todayRemainingCount)
        assertEquals("2 taches prevues aujourd'hui", content?.text)
    }

    @Test
    fun `one overdue without today uses singular verb`() {
        val content =
            buildFamilyDailySummaryNotificationContent(
                todayTasks = emptyList(),
                overdueTasks = listOf(task(status = FamilyTaskStatus.TODO)),
            )

        assertEquals("Taskoday - A verifier", content?.title)
        assertEquals("1 tache est en retard", content?.text)
    }

    @Test
    fun `today and overdue are combined`() {
        val content =
            buildFamilyDailySummaryNotificationContent(
                todayTasks =
                    listOf(
                        task(status = FamilyTaskStatus.TODO),
                        task(status = FamilyTaskStatus.PENDING_VALIDATION),
                    ),
                overdueTasks =
                    listOf(
                        task(status = FamilyTaskStatus.TODO),
                        task(status = FamilyTaskStatus.TODO),
                    ),
            )

        assertEquals("2 taches aujourd'hui · 2 en retard", content?.text)
    }

    @Test
    fun `completed validated and skipped today tasks are excluded from remaining count`() {
        val content =
            buildFamilyDailySummaryNotificationContent(
                todayTasks =
                    listOf(
                        task(status = FamilyTaskStatus.COMPLETED),
                        task(status = FamilyTaskStatus.VALIDATED),
                        task(status = FamilyTaskStatus.SKIPPED),
                        task(status = FamilyTaskStatus.TODO),
                    ),
                overdueTasks = emptyList(),
            )

        assertEquals(1, content?.todayRemainingCount)
        assertEquals("1 tache prevue aujourd'hui", content?.text)
    }

    @Test
    fun `next run today when current time is before configured time`() {
        val now = ZonedDateTime.of(2026, 8, 26, 7, 30, 0, 0, ZoneId.of("Europe/Paris"))

        val next = FamilyNotificationSchedulePolicy.nextDailySummaryRun(now = now, hour = 8, minute = 0)

        assertEquals(ZonedDateTime.of(2026, 8, 26, 8, 0, 0, 0, ZoneId.of("Europe/Paris")), next)
    }

    @Test
    fun `next run tomorrow when current time is after configured time`() {
        val now = ZonedDateTime.of(2026, 8, 26, 8, 1, 0, 0, ZoneId.of("Europe/Paris"))

        val next = FamilyNotificationSchedulePolicy.nextDailySummaryRun(now = now, hour = 8, minute = 0)

        assertEquals(ZonedDateTime.of(2026, 8, 27, 8, 0, 0, 0, ZoneId.of("Europe/Paris")), next)
    }

    @Test
    fun `next run handles month and year boundaries`() {
        val now = ZonedDateTime.of(2026, 12, 31, 23, 59, 0, 0, ZoneId.of("UTC"))

        val next = FamilyNotificationSchedulePolicy.nextDailySummaryRun(now = now, hour = 8, minute = 0)

        assertEquals(ZonedDateTime.of(2027, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC")), next)
    }

    @Test
    fun `next run survives daylight saving gap using device zone rules`() {
        val zone = ZoneId.of("Europe/Paris")
        val now = ZonedDateTime.of(2026, 3, 29, 1, 30, 0, 0, zone)

        val next = FamilyNotificationSchedulePolicy.nextDailySummaryRun(now = now, hour = 2, minute = 30)

        assertTrue(next.toInstant().isAfter(now.toInstant()))
        assertEquals(zone, next.zone)
    }

    private fun task(status: FamilyTaskStatus): FamilyTaskTodayItem =
        FamilyTaskTodayItem(
            taskId = 1L,
            occurrenceId = 10L,
            title = "Tache",
            assignees = emptyList(),
            scheduledDate = "2026-08-26",
            dueDate = "2026-08-26",
            dueTime = null,
            hasDueTime = false,
            dueAt = null,
            status = status,
            validationRequired = false,
            gamificationEnabled = false,
            priority = FamilyTaskPriority.NORMAL,
        )
}
