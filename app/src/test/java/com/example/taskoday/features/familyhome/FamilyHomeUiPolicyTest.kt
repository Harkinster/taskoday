package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskActor
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.FamilyActionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyHomeUiPolicyTest {
    @Test fun `kind labels cover all supported scope kind pairs and overdue metadata` () {
        val labels = listOf(
            FamilyActionType.PERSONAL_ROUTINE to "Routine",
            FamilyActionType.PERSONAL_MISSION to "Mission",
            FamilyActionType.HOUSE_ROUTINE to "Routine Maison",
            FamilyActionType.HOUSE_MISSION to "Mission Maison",
            FamilyActionType.HOUSE_QUEST to "Quête Maison",
        )
        labels.forEach { (type, label) -> assertEquals(label, familyTaskKindLabel(task(category = type.category))) }
        assertTrue(familyTaskOverdueMetaLabel(task(category = FamilyActionType.HOUSE_MISSION.category), LocalDate.of(2026, 8, 23)).contains("Mission Maison"))
    }

    @Test
    fun `undated missions have an open work group while dated and completed tasks keep temporal groups`() {
        val today = LocalDate.of(2026, 10, 6)
        val personalNoDue = task(title = "Personal sans date", scheduledDate = null, dueDate = null, category = FamilyActionType.PERSONAL_MISSION.category)
        val houseNoDue = task(title = "Maison sans date", scheduledDate = null, dueDate = null, category = FamilyActionType.HOUSE_MISSION.category)
        val doneNoDue = personalNoDue.copy(status = FamilyTaskStatus.COMPLETED)

        assertEquals(FamilyTaskTemporalGroup.OPEN_UNDATED, familyTaskTemporalGroup(personalNoDue, today))
        assertEquals(FamilyTaskTemporalGroup.OPEN_UNDATED, familyTaskTemporalGroup(houseNoDue, today))
        assertEquals(FamilyTaskTemporalGroup.TODAY, familyTaskTemporalGroup(task(scheduledDate = today.toString()), today))
        assertEquals(FamilyTaskTemporalGroup.OVERDUE, familyTaskTemporalGroup(task(scheduledDate = "2026-10-05"), today))
        assertEquals(FamilyTaskTemporalGroup.UPCOMING, familyTaskTemporalGroup(task(scheduledDate = "2026-10-07"), today))
        assertEquals(FamilyTaskTemporalGroup.COMPLETED, familyTaskTemporalGroup(doneNoDue, today))
    }

    @Test
    fun `undated routines and quests retain occurrence behavior and are not missions without due dates`() {
        val today = LocalDate.of(2026, 10, 6)
        val routine = task(title = "Routine", scheduledDate = null, dueDate = null, category = FamilyActionType.HOUSE_ROUTINE.category)
        val quest = task(title = "Quest", scheduledDate = null, dueDate = null, category = FamilyActionType.HOUSE_QUEST.category)

        assertEquals(FamilyTaskTemporalGroup.TODAY, familyTaskTemporalGroup(routine, today))
        assertEquals(FamilyTaskTemporalGroup.TODAY, familyTaskTemporalGroup(quest, today))
        assertEquals(emptyList<FamilyTaskTodayItem>(), openUndatedMissions(listOf(routine, quest)))
    }

    @Test
    fun `open undated mission policy separates it from scheduled today tasks`() {
        val today = LocalDate.of(2026, 10, 6)
        val noDue = task(title = "Sans date", scheduledDate = null, dueDate = null, category = FamilyActionType.HOUSE_MISSION.category)
        val scheduled = task(title = "Aujourd'hui", scheduledDate = today.toString(), category = FamilyActionType.HOUSE_MISSION.category)

        assertEquals(listOf("Aujourd'hui"), todayScheduledTasks(listOf(noDue, scheduled), today).map { it.title })
        assertEquals(listOf("Sans date"), openUndatedMissions(listOf(noDue)).map { it.title })
    }

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
    fun `maison keeps assigned quests and excludes personal actions`() {
        val member = FamilyTaskAssignee(id = 1L, displayName = "Ada")
        val tasks =
            listOf(
                task(title = "Maison", assignees = emptyList()),
                task(title = "Personnel", assignees = listOf(member), category = FamilyActionType.PERSONAL_MISSION.category),
                task(title = "Partagee", assignees = listOf(member, FamilyTaskAssignee(id = 2L, displayName = "Nino"))),
            )

        assertEquals(listOf("Maison", "Partagee"), familyHouseTasks(tasks).map { it.title })
    }

    @Test fun `maison contains all three house kinds regardless of assignment`() {
        val member = FamilyTaskAssignee(id = 1L, displayName = "Ada")
        val tasks = listOf(
            task(title = "Routine Maison", assignees = listOf(member), category = FamilyActionType.HOUSE_ROUTINE.category),
            task(title = "Mission Maison", assignees = emptyList(), category = FamilyActionType.HOUSE_MISSION.category),
            task(title = "Quête Maison", assignees = listOf(member), category = FamilyActionType.HOUSE_QUEST.category),
            task(title = "Routine personnelle", assignees = listOf(member), category = FamilyActionType.PERSONAL_ROUTINE.category),
        )
        assertEquals(listOf("Routine Maison", "Mission Maison", "Quête Maison"), familyHouseTasks(tasks).map { it.title })
    }

    @Test
    fun `parent daily view includes unassigned single and multi assigned house quests once`() {
        val ada = FamilyTaskAssignee(id = 11L, displayName = "Ada")
        val nino = FamilyTaskAssignee(id = 12L, displayName = "Nino")
        val tasks = listOf(
            task(title = "Maison", assignees = emptyList()),
            task(title = "Ada", assignees = listOf(ada)),
            task(title = "Nino", assignees = listOf(nino)),
            task(title = "Ensemble", assignees = listOf(ada, nino)),
        )

        assertEquals(listOf("Maison", "Ada", "Nino", "Ensemble"), visibleDailyTasks(tasks, FamilyTaskAccessPolicy(25L, "PARENT")).map { it.title })
    }

    @Test
    fun `child daily view includes quests assigned to another child but excludes personal actions`() {
        val ada = FamilyTaskAssignee(id = 11L, displayName = "Ada")
        val nino = FamilyTaskAssignee(id = 12L, displayName = "Nino")
        val tasks = listOf(
            task(title = "Maison", assignees = emptyList()),
            task(title = "Ada", assignees = listOf(ada)),
            task(title = "Nino", assignees = listOf(nino)),
            task(title = "Mission Nino", assignees = listOf(nino), category = FamilyActionType.PERSONAL_MISSION.category),
            task(title = "Routine Ada", assignees = listOf(ada), category = FamilyActionType.PERSONAL_ROUTINE.category),
            task(title = "Ensemble", assignees = listOf(ada, nino)),
        )

        assertEquals(listOf("Maison", "Ada", "Nino", "Ensemble"), visibleDailyTasks(tasks, FamilyTaskAccessPolicy(11L, "CHILD")).map { it.title })
    }

    @Test
    fun `today and completed partitions keep validation pending with work`() {
        val tasks = listOf(
            task(title = "A faire", status = FamilyTaskStatus.TODO),
            task(title = "A valider", status = FamilyTaskStatus.PENDING_VALIDATION),
            task(title = "Terminee", status = FamilyTaskStatus.COMPLETED),
            task(title = "Validee", status = FamilyTaskStatus.VALIDATED),
        )

        assertEquals(listOf("A faire", "A valider"), pendingDailyTasks(tasks).map { it.title })
        assertEquals(listOf("Terminee", "Validee"), completedDailyTasks(tasks).map { it.title })
        assertEquals("En attente de validation", familyTaskStatusLabel(FamilyTaskStatus.PENDING_VALIDATION))
        assertEquals("Terminée", familyTaskStatusLabel(FamilyTaskStatus.COMPLETED))
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
        assertEquals("En cours", familyTaskStatusLabel(FamilyTaskStatus.IN_PROGRESS))
        assertEquals("En attente de validation", familyTaskStatusLabel(FamilyTaskStatus.PENDING_VALIDATION))
        assertEquals("Validée", familyTaskStatusLabel(FamilyTaskStatus.VALIDATED))
        assertEquals("Ratée", familyTaskStatusLabel(FamilyTaskStatus.FAILED))
        assertEquals("Ignorée", familyTaskStatusLabel(FamilyTaskStatus.SKIPPED))
    }

    @Test
    fun `quick actions follow status`() {
        assertEquals(FamilyTaskQuickAction.START, quickActionFor(task(status = FamilyTaskStatus.TODO)))
        val inProgress = task(status = FamilyTaskStatus.IN_PROGRESS, category = FamilyActionType.HOUSE_MISSION.category)
        assertEquals(FamilyTaskQuickAction.JOIN, quickActionFor(inProgress, userId = 101L))
        assertEquals(
            FamilyTaskQuickAction.COMPLETE,
            quickActionFor(inProgress.copy(contributors = listOf(FamilyTaskActor(101L, "Ada"))), userId = 101L),
        )
        assertEquals(FamilyTaskQuickAction.VALIDATE, quickActionFor(task(status = FamilyTaskStatus.PENDING_VALIDATION)))
        assertEquals(FamilyTaskQuickAction.REOPEN, quickActionFor(task(status = FamilyTaskStatus.VALIDATED)))
        assertNull(quickActionFor(task(status = FamilyTaskStatus.FAILED)))
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
            "Hier · Quête Maison · Maison",
            familyTaskOverdueMetaLabel(
                task = task(title = "Maison", assignees = emptyList(), scheduledDate = "2026-08-22"),
                today = today,
            ),
        )
        assertEquals(
            "21 août · Quête Maison · Ada, Nino",
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
            "20 août à 00:00 · Quête Maison · Ada",
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
        assertEquals("00:00 · Quête Maison · Ada", familyTaskUpcomingMetaLabel(rows[0].task))
        assertEquals("Quête Maison · Ada", familyTaskUpcomingMetaLabel(rows[2].task))
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
        scheduledDate: String? = "2026-08-22",
        dueDate: String? = scheduledDate,
        dueTime: String? = null,
        hasDueTime: Boolean = false,
        category: String? = null,
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
            category = category,
        )
}
