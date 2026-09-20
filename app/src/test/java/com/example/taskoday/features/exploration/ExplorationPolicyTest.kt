package com.example.taskoday.features.exploration

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorationPolicyTest {
    @Test fun `household recurring remains household`() {
        val task = task(assignees = emptyList(), recurrence = "WEEKLY")
        assertTrue(task.isHouseholdTask())
        assertEquals(ExplorationCategory.HOUSEHOLD, task.explorationCategory())
    }

    @Test fun `personal one shot is a personal task`() {
        val task = task(recurrence = "NONE")
        assertTrue(task.isPersonalTask())
        assertFalse(task.isRecurringTask())
        assertEquals(ExplorationCategory.PERSONAL_TASK, task.explorationCategory())
    }

    @Test fun `personal recurring is a routine`() {
        val task = task(recurrence = "DAILY")
        assertEquals(ExplorationCategory.PERSONAL_ROUTINE, task.explorationCategory())
        assertEquals("Tous les jours", familyTaskOccurrenceRecurrenceLabel(task.recurrenceLabel))
    }

    @Test fun `weekday labels stay compact`() {
        assertEquals("Lun Mer", familyTaskRecurrenceLabel(com.example.taskoday.domain.model.FamilyTaskRecurrence.SELECTED_WEEKDAYS, listOf(1, 3)))
    }

    private fun task(assignees: List<FamilyTaskAssignee> = listOf(FamilyTaskAssignee(1L, "Naomy")), recurrence: String): FamilyTaskTodayItem =
        FamilyTaskTodayItem(1L, 2L, "Test", assignees, "2026-09-19", "2026-09-19", null, false, null, FamilyTaskStatus.TODO, false, false, FamilyTaskPriority.NORMAL, recurrence)
}
