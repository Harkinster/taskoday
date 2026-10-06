package com.example.taskoday.features.exploration

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.features.familyhome.FamilyTaskAccessPolicy
import com.example.taskoday.features.familyhome.FamilyTaskQuickAction
import com.example.taskoday.features.familyhome.familyTaskActionLabel
import com.example.taskoday.features.familyhome.familyTaskStatusLabel
import com.example.taskoday.features.familyhome.isOpenUndatedMission
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

    @Test fun `house routine and mission stay outside personal exploration`() {
        assertTrue(task(recurrence = "DAILY", category = FamilyActionType.HOUSE_ROUTINE.category).isHouseholdTask())
        assertTrue(task(recurrence = "NONE", category = FamilyActionType.HOUSE_MISSION.category).isHouseholdTask())
    }

    @Test fun `personal one shot is a personal task`() {
        val task = task(recurrence = "NONE", category = FamilyActionType.PERSONAL_MISSION.category)
        assertTrue(task.isPersonalTask())
        assertFalse(task.isRecurringTask())
        assertEquals(ExplorationCategory.PERSONAL_TASK, task.explorationCategory())
    }

    @Test fun `personal recurring is a routine`() {
        val task = task(recurrence = "DAILY", category = FamilyActionType.PERSONAL_ROUTINE.category)
        assertEquals(ExplorationCategory.PERSONAL_ROUTINE, task.explorationCategory())
        assertEquals("Tous les jours", familyTaskOccurrenceRecurrenceLabel(task.recurrenceLabel))
    }

    @Test fun `undated personal mission belongs in open work while routine remains occurrence based`() {
        val mission = task(recurrence = "NONE", category = FamilyActionType.PERSONAL_MISSION.category, scheduledDate = null, dueDate = null)
        val routine = task(recurrence = "DAILY", category = FamilyActionType.PERSONAL_ROUTINE.category, scheduledDate = null, dueDate = null)

        assertTrue(mission.isOpenUndatedMission())
        assertFalse(routine.isOpenUndatedMission())
    }

    @Test fun `a recurring mission does not turn into a routine`() {
        val task = task(recurrence = "DAILY", category = FamilyActionType.PERSONAL_MISSION.category)
        assertEquals(ExplorationCategory.PERSONAL_TASK, task.explorationCategory())
    }

    @Test fun `weekday labels stay compact`() {
        assertEquals("Lun Mer", familyTaskRecurrenceLabel(com.example.taskoday.domain.model.FamilyTaskRecurrence.SELECTED_WEEKDAYS, listOf(1, 3)))
    }

    @Test fun `assigned recurring legacy family task stays collective`() {
        assertEquals(ExplorationCategory.HOUSEHOLD, task(recurrence = "DAILY").explorationCategory())
    }

    @Test fun `pending personal action offers validation only to parent`() {
        val pending = task(recurrence = "NONE", category = FamilyActionType.PERSONAL_MISSION.category)
            .copy(status = FamilyTaskStatus.PENDING_VALIDATION)
        val parentAction = FamilyTaskAccessPolicy(1L, "PARENT").quickAction(pending)
        assertEquals(FamilyTaskQuickAction.VALIDATE, parentAction)
        assertEquals("Valider", parentAction?.let(::familyTaskActionLabel))
        assertEquals("En attente de validation", familyTaskStatusLabel(pending.status))
        assertEquals(null, FamilyTaskAccessPolicy(1L, "CHILD").quickAction(pending))
    }

    @Test fun `exploration action labels follow current status`() {
        val parent = FamilyTaskAccessPolicy(1L, "PARENT")
        val original = task(recurrence = "NONE", category = FamilyActionType.PERSONAL_MISSION.category)
        assertEquals("Terminer", parent.quickAction(original)?.let(::familyTaskActionLabel))
        assertEquals("Rouvrir", parent.quickAction(original.copy(status = FamilyTaskStatus.COMPLETED))?.let(::familyTaskActionLabel))
        assertEquals("Rouvrir", parent.quickAction(original.copy(status = FamilyTaskStatus.VALIDATED))?.let(::familyTaskActionLabel))
        assertEquals("Terminée", familyTaskStatusLabel(FamilyTaskStatus.COMPLETED))
        assertEquals("Validée", familyTaskStatusLabel(FamilyTaskStatus.VALIDATED))
    }

    private fun task(
        assignees: List<FamilyTaskAssignee> = listOf(FamilyTaskAssignee(1L, "Naomy")),
        recurrence: String,
        category: String? = null,
        scheduledDate: String? = "2026-09-19",
        dueDate: String? = scheduledDate,
    ): FamilyTaskTodayItem =
        FamilyTaskTodayItem(1L, 2L, "Test", assignees, scheduledDate, dueDate, null, false, null, FamilyTaskStatus.TODO, false, false, FamilyTaskPriority.NORMAL, recurrence, category = category)
}
