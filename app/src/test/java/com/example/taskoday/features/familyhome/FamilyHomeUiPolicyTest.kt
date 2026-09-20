package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyHomeUiPolicyTest {
    @Test
    fun `empty today list has no sections`() {
        assertEquals(emptyList<FamilyTaskMemberSection>(), buildFamilyTaskSections(emptyList()))
    }

    @Test
    fun `unassigned task goes to maison`() {
        val sections =
            buildFamilyTaskSections(
                listOf(task(title = "Ranger l'entrée", assignees = emptyList())),
            )

        assertEquals("Maison", sections.single().name)
        assertEquals(0, sections.single().completedCount)
        assertEquals(1, sections.single().totalCount)
    }

    @Test
    fun `maison filter excludes personal and multi assigned tasks`() {
        val member = FamilyTaskAssignee(id = 1L, displayName = "Ada")
        val tasks =
            listOf(
                task(title = "Maison", assignees = emptyList()),
                task(title = "Personnel", assignees = listOf(member)),
                task(title = "Partagee", assignees = listOf(member, FamilyTaskAssignee(id = 2L, displayName = "Nino"))),
            )

        assertEquals(listOf("Maison"), familyHouseTasks(tasks).map { it.title })
    }

    @Test
    fun `tasks are grouped by assignee with done count`() {
        val ada = FamilyTaskAssignee(id = 1L, displayName = "Ada")
        val nino = FamilyTaskAssignee(id = 2L, displayName = "Nino")

        val sections =
            buildFamilyTaskSections(
                listOf(
                    task(title = "Table", assignees = listOf(ada), status = FamilyTaskStatus.VALIDATED),
                    task(title = "Cartable", assignees = listOf(ada), status = FamilyTaskStatus.TODO),
                    task(title = "Arroser", assignees = listOf(nino), status = FamilyTaskStatus.COMPLETED),
                ),
            )

        val adaSection = sections.first { it.name == "Ada" }
        val ninoSection = sections.first { it.name == "Nino" }

        assertEquals(1, adaSection.completedCount)
        assertEquals(2, adaSection.totalCount)
        assertEquals(1, ninoSection.completedCount)
        assertEquals(1, ninoSection.totalCount)
    }

    @Test
    fun `multi assignee task appears in each member section`() {
        val ada = FamilyTaskAssignee(id = 1L, displayName = "Ada")
        val nino = FamilyTaskAssignee(id = 2L, displayName = "Nino")

        val sections =
            buildFamilyTaskSections(
                listOf(task(title = "Courses", assignees = listOf(ada, nino))),
            )

        assertEquals(listOf("Ada", "Nino"), sections.map { section -> section.name })
        assertEquals(1, sections.first { section -> section.name == "Ada" }.totalCount)
        assertEquals(1, sections.first { section -> section.name == "Nino" }.totalCount)
    }

    @Test
    fun `week window uses french monday to sunday`() {
        val window = familyTaskWeekWindowContaining(LocalDate.of(2026, 8, 22))

        assertEquals(LocalDate.of(2026, 8, 17), window.startDate)
        assertEquals(LocalDate.of(2026, 8, 23), window.endDate)
        assertEquals(7, window.days.size)
    }

    @Test
    fun `week range label handles month and year boundaries`() {
        assertEquals(
            "27 juillet - 2 août",
            familyTaskWeekRangeLabel(LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2)),
        )
        assertEquals(
            "28 décembre 2026 - 3 janvier 2027",
            familyTaskWeekRangeLabel(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3)),
        )
    }

    @Test
    fun `week summaries count completed tasks by scheduled date`() {
        val days = familyTaskWeekWindowContaining(LocalDate.of(2026, 8, 22)).days

        val summaries =
            buildFamilyTaskWeekDaySummaries(
                days = days,
                tasks =
                    listOf(
                        task(title = "Table", scheduledDate = "2026-08-22", status = FamilyTaskStatus.VALIDATED),
                        task(title = "Cartable", scheduledDate = "2026-08-22", status = FamilyTaskStatus.TODO),
                        task(title = "Courses", scheduledDate = "2026-08-23", status = FamilyTaskStatus.COMPLETED),
                    ),
                selectedDate = LocalDate.of(2026, 8, 22),
                today = LocalDate.of(2026, 8, 22),
            )

        val saturday = summaries.first { day -> day.date == "2026-08-22" }
        val sunday = summaries.first { day -> day.date == "2026-08-23" }

        assertEquals(1, saturday.completedCount)
        assertEquals(2, saturday.totalCount)
        assertEquals(1, sunday.completedCount)
        assertEquals(1, sunday.totalCount)
        assertEquals(true, saturday.isSelected)
        assertEquals(true, saturday.isToday)
    }

    @Test
    fun `date filtering supports empty day and empty week`() {
        val tasks =
            listOf(
                task(title = "Table", scheduledDate = "2026-08-22"),
            )

        assertEquals(1, familyTasksForDate(tasks, LocalDate.of(2026, 8, 22)).size)
        assertEquals(emptyList<FamilyTaskTodayItem>(), familyTasksForDate(tasks, LocalDate.of(2026, 8, 21)))
        assertEquals(false, familyTaskWeekIsEmpty(tasks))
        assertEquals(true, familyTaskWeekIsEmpty(emptyList()))
    }

    @Test
    fun `due labels distinguish no time from real midnight`() {
        assertEquals(
            "22 août",
            familyTaskDueLabel(
                dueDate = "2026-08-22",
                dueTime = null,
                hasDueTime = false,
                dueAt = null,
            ),
        )
        assertEquals(
            "22 août à 00:00",
            familyTaskDueLabel(
                dueDate = "2026-08-22",
                dueTime = "00:00",
                hasDueTime = true,
                dueAt = null,
            ),
        )
    }

    @Test
    fun `selected day is preserved after refresh when still in week`() {
        assertEquals(
            LocalDate.of(2026, 8, 20),
            resolveFamilyTaskSelectedWeekDate(
                preferredDate = LocalDate.of(2026, 8, 20),
                weekStartDate = LocalDate.of(2026, 8, 17),
                today = LocalDate.of(2026, 8, 22),
            ),
        )
        assertEquals(
            LocalDate.of(2026, 8, 17),
            resolveFamilyTaskSelectedWeekDate(
                preferredDate = LocalDate.of(2026, 8, 30),
                weekStartDate = LocalDate.of(2026, 8, 17),
                today = LocalDate.of(2026, 9, 1),
            ),
        )
    }

    @Test
    fun `creation date is contextual only in week mode`() {
        assertEquals(
            "2026-08-23",
            familyTaskCreationDateForMode(
                mode = FamilyHomeMode.TODAY,
                selectedWeekDate = "2026-08-26",
                todayDate = "2026-08-23",
            ),
        )
        assertEquals(
            "2026-08-26",
            familyTaskCreationDateForMode(
                mode = FamilyHomeMode.WEEK,
                selectedWeekDate = "2026-08-26",
                todayDate = "2026-08-23",
            ),
        )
        assertNull(
            familyTaskCreationDateForMode(
                mode = FamilyHomeMode.WEEK,
                selectedWeekDate = "bad-date",
                todayDate = "2026-08-23",
            ),
        )
    }

    @Test
    fun `occurrence recurrence label hides one off technical values`() {
        assertNull(familyTaskOccurrenceRecurrenceLabel(null))
        assertNull(familyTaskOccurrenceRecurrenceLabel(""))
        assertNull(familyTaskOccurrenceRecurrenceLabel("NONE"))
        assertNull(familyTaskOccurrenceRecurrenceLabel("one shot"))
    }

    @Test
    fun `occurrence recurrence label maps recurring technical values`() {
        assertEquals("Tous les jours", familyTaskOccurrenceRecurrenceLabel("DAILY"))
        assertEquals("Chaque semaine", familyTaskOccurrenceRecurrenceLabel("weekly"))
        assertEquals("Certains jours", familyTaskOccurrenceRecurrenceLabel("SELECTED_WEEKDAYS"))
        assertEquals("Toutes les deux semaines", familyTaskOccurrenceRecurrenceLabel("Toutes les deux semaines"))
    }

    @Test
    fun `status labels cover expected family task states`() {
        assertEquals("À faire", familyTaskStatusLabel(FamilyTaskStatus.TODO))
        assertEquals("En attente de validation", familyTaskStatusLabel(FamilyTaskStatus.PENDING_VALIDATION))
        assertEquals("Validée", familyTaskStatusLabel(FamilyTaskStatus.VALIDATED))
        assertEquals("Ignorée", familyTaskStatusLabel(FamilyTaskStatus.SKIPPED))
    }

    @Test
    fun `quick actions follow status`() {
        assertEquals(FamilyTaskQuickAction.COMPLETE, quickActionFor(task(status = FamilyTaskStatus.TODO)))
        assertEquals(FamilyTaskQuickAction.VALIDATE, quickActionFor(task(status = FamilyTaskStatus.PENDING_VALIDATION)))
        assertEquals(FamilyTaskQuickAction.REOPEN, quickActionFor(task(status = FamilyTaskStatus.VALIDATED)))
        assertNull(quickActionFor(task(status = FamilyTaskStatus.SKIPPED)))
    }

    @Test
    fun `overdue preview is silent when empty`() {
        assertEquals(emptyList<FamilyTaskRow>(), buildFamilyTaskOverduePreview(emptyList()))
    }

    @Test
    fun `overdue preview sorts oldest tasks first and keeps total outside preview`() {
        val tasks =
            listOf(
                task(title = "Hier", scheduledDate = "2026-08-22"),
                task(title = "Ancienne", scheduledDate = "2026-08-19"),
                task(title = "Milieu", scheduledDate = "2026-08-21"),
            )

        val preview = buildFamilyTaskOverduePreview(tasks, previewLimit = 2)

        assertEquals(listOf("Ancienne", "Milieu"), preview.map { row -> row.task.title })
        assertEquals(3, tasks.size)
    }

    @Test
    fun `overdue labels use yesterday readable date house member and real midnight`() {
        val today = LocalDate.of(2026, 8, 23)

        assertEquals(
            "Hier · Maison",
            familyTaskOverdueMetaLabel(
                task = task(title = "Maison", assignees = emptyList(), scheduledDate = "2026-08-22"),
                today = today,
            ),
        )
        assertEquals(
            "21 août · Ada, Nino",
            familyTaskOverdueMetaLabel(
                task =
                    task(
                        title = "Multi",
                        assignees =
                            listOf(
                                FamilyTaskAssignee(id = 1L, displayName = "Ada"),
                                FamilyTaskAssignee(id = 2L, displayName = "Nino"),
                            ),
                        scheduledDate = "2026-08-21",
                    ),
                today = today,
            ),
        )
        assertEquals(
            "20 août à 00:00 · Ada",
            familyTaskOverdueMetaLabel(
                task = task(title = "Minuit", scheduledDate = "2026-08-20", dueTime = "00:00", hasDueTime = true),
                today = today,
            ),
        )
    }

    @Test
    fun `upcoming window starts tomorrow and ends at j plus seven`() {
        val window = familyTaskUpcomingWindow(LocalDate.of(2026, 8, 23))

        assertEquals(LocalDate.of(2026, 8, 24), window.startDate)
        assertEquals(LocalDate.of(2026, 8, 30), window.endDate)
    }

    @Test
    fun `upcoming sections group by day exclude today and keep j plus seven`() {
        val sections =
            buildFamilyTaskUpcomingSections(
                tasks =
                    listOf(
                        task(title = "Today", scheduledDate = "2026-08-23"),
                        task(title = "Tomorrow", scheduledDate = "2026-08-24"),
                        task(title = "J7", scheduledDate = "2026-08-30"),
                        task(title = "J8", scheduledDate = "2026-08-31"),
                    ),
                today = LocalDate.of(2026, 8, 23),
            )

        assertEquals(listOf("2026-08-24", "2026-08-30"), sections.map { section -> section.date })
        assertEquals("Demain", sections.first().label)
        assertEquals(listOf("Tomorrow", "J7"), sections.flatMap { section -> section.tasks }.map { row -> row.task.title })
    }

    @Test
    fun `upcoming sections sort timed tasks before untimed and preserve real midnight`() {
        val sections =
            buildFamilyTaskUpcomingSections(
                tasks =
                    listOf(
                        task(title = "Sans heure", scheduledDate = "2026-08-24"),
                        task(title = "Soir", scheduledDate = "2026-08-24", dueTime = "18:30", hasDueTime = true),
                        task(title = "Minuit", scheduledDate = "2026-08-24", dueTime = "00:00", hasDueTime = true),
                    ),
                today = LocalDate.of(2026, 8, 23),
            )

        val rows = sections.single().tasks
        assertEquals(listOf("Minuit", "Soir", "Sans heure"), rows.map { row -> row.task.title })
        assertEquals("00:00 · Ada", familyTaskUpcomingMetaLabel(rows[0].task))
        assertEquals("Ada", familyTaskUpcomingMetaLabel(rows[2].task))
    }

    @Test
    fun `upcoming preview handles month and year boundaries and exposes more flag`() {
        val today = LocalDate.of(2026, 12, 30)
        val tasks =
            (1..6).map { offset ->
                task(
                    title = "Tache $offset",
                    scheduledDate = today.plusDays(offset.toLong()).toString(),
                )
            }

        val sections = buildFamilyTaskUpcomingSections(tasks = tasks, today = today, previewLimit = 5)

        assertEquals(5, sections.flatMap { section -> section.tasks }.size)
        assertEquals(true, hasMoreFamilyTaskUpcomingPreview(tasks = tasks, today = today, previewLimit = 5))
        assertEquals("Demain", sections.first().label)
        assertEquals("Mercredi 6 janvier", familyTaskUpcomingDayLabel(LocalDate.of(2027, 1, 6), today))
    }

    @Test
    fun `pending validation is counted without being considered done`() {
        val tasks =
            listOf(
                task(title = "A valider", status = FamilyTaskStatus.PENDING_VALIDATION),
                task(title = "Validee", status = FamilyTaskStatus.VALIDATED),
            )

        assertEquals(1, familyTaskPendingValidationCount(tasks))
        assertEquals(1, tasks.count { item -> item.status.countsAsDone })
    }

    @Test
    fun `priority only highlights useful values`() {
        assertNull(familyTaskPriorityLabel(FamilyTaskPriority.NORMAL))
        assertEquals("Prioritaire", familyTaskPriorityLabel(FamilyTaskPriority.HIGH))
        assertEquals("Urgent", familyTaskPriorityLabel(FamilyTaskPriority.URGENT))
    }

    @Test
    fun `date label uses backend date when present`() {
        assertEquals(
            "Samedi 22 août",
            formatFamilyHomeDateLabel("2026-08-22", fallback = LocalDate.of(2026, 1, 1)),
        )
    }

    private fun task(
        title: String = "Tâche",
        assignees: List<FamilyTaskAssignee> = listOf(FamilyTaskAssignee(id = 1L, displayName = "Ada")),
        status: FamilyTaskStatus = FamilyTaskStatus.TODO,
        scheduledDate: String = "2026-08-22",
        dueDate: String? = scheduledDate,
        dueTime: String? = null,
        hasDueTime: Boolean = false,
    ): FamilyTaskTodayItem =
        FamilyTaskTodayItem(
            taskId = title.hashCode().toLong(),
            occurrenceId = title.hashCode().toLong(),
            title = title,
            assignees = assignees,
            scheduledDate = scheduledDate,
            dueDate = dueDate,
            dueTime = dueTime,
            hasDueTime = hasDueTime,
            dueAt = null,
            status = status,
            validationRequired = false,
            gamificationEnabled = false,
            priority = FamilyTaskPriority.NORMAL,
        )
}
