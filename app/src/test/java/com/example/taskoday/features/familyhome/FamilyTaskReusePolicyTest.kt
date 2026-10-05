package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTaskReusePolicyTest {
    private val today = LocalDate.parse("2026-10-15")
    private val members = listOf(member(25), member(27), member(28))

    @Test fun `quest copy preserves category title and valid multiple assignees but resets date and identity`() {
        val source = task(type = FamilyActionType.HOUSE_QUEST, assignees = listOf(27, 28))
        val copy = reuseTaskPrefill(state(FamilyActionType.HOUSE_QUEST), source, 7, members, today, LocalTime.NOON)
        assertEquals("Mettre la table", copy.title)
        assertEquals(FamilyActionType.HOUSE_QUEST.category, copy.category)
        assertEquals(setOf(27L, 28L), copy.selectedAssigneeUserIds)
        assertEquals("2026-10-15", copy.date)
        assertEquals(null, copy.taskId)
        assertEquals(source.id, copy.reusedSourceId)
        assertFalse(copy.created)
    }

    @Test fun `old date with passed hour moves to tomorrow`() {
        val source = task(type = FamilyActionType.PERSONAL_MISSION, assignees = listOf(25), dueTime = "08:00", hasDueTime = true)
        val copy = reuseTaskPrefill(state(FamilyActionType.PERSONAL_MISSION), source, 7, members, today, LocalTime.NOON)
        assertEquals("2026-10-16", copy.date)
        assertEquals("08:00", copy.time)
        assertEquals(setOf(25L), copy.selectedAssigneeUserIds)
    }

    @Test fun `removed participant requires explicit review`() {
        val source = task(type = FamilyActionType.HOUSE_QUEST, assignees = listOf(27, 99))
        val copy = reuseTaskPrefill(state(FamilyActionType.HOUSE_QUEST), source, 7, members, today, LocalTime.NOON)
        assertEquals(setOf(27L), copy.selectedAssigneeUserIds)
        assertTrue(copy.requiresAssigneeReview)
    }

    @Test fun `routine retains recurrence and moves weekly start to original weekday`() {
        val source = task(type = FamilyActionType.PERSONAL_ROUTINE, assignees = listOf(25), recurrence = FamilyTaskRecurrence.WEEKLY, dueDate = "2026-10-06", interval = 2)
        val copy = reuseTaskPrefill(state(FamilyActionType.PERSONAL_ROUTINE), source, 7, members, today, LocalTime.NOON)
        assertEquals(FamilyTaskRecurrence.WEEKLY, copy.recurrence)
        assertEquals(2, copy.recurrenceInterval)
        assertEquals("2026-10-20", copy.date)
    }

    @Test fun `cross family copy is rejected`() {
        val source = task(type = FamilyActionType.HOUSE_QUEST, assignees = emptyList())
        assertTrue(runCatching { reuseTaskPrefill(state(FamilyActionType.HOUSE_QUEST), source, 8, members, today, LocalTime.NOON) }.isFailure)
    }

    @Test fun `recents use created at and family type member filters`() {
        val questOld = task(id = 1, type = FamilyActionType.HOUSE_QUEST, assignees = emptyList(), createdAt = "2026-10-01T10:00:00")
        val questNew = task(id = 2, type = FamilyActionType.HOUSE_QUEST, assignees = listOf(27), createdAt = "2026-10-03T10:00:00")
        val otherFamily = task(id = 3, type = FamilyActionType.HOUSE_QUEST, assignees = emptyList(), familyId = 8, createdAt = "2026-10-04T10:00:00")
        val inactive = task(id = 4, type = FamilyActionType.HOUSE_QUEST, assignees = emptyList(), active = false, createdAt = "2026-10-05T10:00:00")
        val noTimestamp = task(id = 5, type = FamilyActionType.HOUSE_QUEST, assignees = emptyList())
        val mission = task(id = 6, type = FamilyActionType.PERSONAL_MISSION, assignees = listOf(25), createdAt = "2026-10-02T10:00:00")
        val tasks = listOf(questOld, questNew, otherFamily, inactive, noTimestamp, mission)
        assertEquals(listOf(2L, 1L), recentTasksForContext(tasks, 7, FamilyActionType.HOUSE_QUEST, null).map { it.id })
        assertEquals(listOf(6L), recentTasksForContext(tasks, 7, FamilyActionType.PERSONAL_MISSION, 25).map { it.id })
        assertTrue(recentTasksForContext(tasks, 7, FamilyActionType.PERSONAL_ROUTINE, 25).isEmpty())
    }

    @Test fun `recents show at most three reliable newest definitions`() {
        val tasks = (1L..6L).map { id ->
            task(id = id, type = FamilyActionType.HOUSE_QUEST, assignees = emptyList(), createdAt = "2026-10-0$id" + "T10:00:00")
        }
        assertEquals(listOf(6L, 5L, 4L), recentTasksForContext(tasks, 7, FamilyActionType.HOUSE_QUEST, null).map { it.id })
    }

    private fun state(type: FamilyActionType) = FamilyTaskCreateUiState(actionType = type, category = type.category)
    private fun member(id: Long) = FamilyTaskMember(id, "Member $id", null, FamilyTaskMemberRole.CHILD, true)
    private fun task(
        id: Long = 42,
        type: FamilyActionType,
        assignees: List<Int>,
        familyId: Long = 7,
        dueDate: String = "2026-10-06",
        dueTime: String? = null,
        hasDueTime: Boolean = false,
        recurrence: FamilyTaskRecurrence = FamilyTaskRecurrence.NONE,
        interval: Int = 1,
        active: Boolean = true,
        createdAt: String? = null,
    ) = FamilyTaskDefinition(
        id, familyId, "Mettre la table", null,
        assignees.map { FamilyTaskAssignee(it.toLong(), "Member $it") },
        dueDate, dueTime, hasDueTime, null, recurrence, emptyList(), false, false,
        FamilyTaskPriority.NORMAL, active, interval, type.category, createdAt,
    )
}
