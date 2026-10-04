package com.example.taskoday.data.repository

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilyActionCategoryJoinTest {
    @Test fun `occurrences keep their definition type regardless of assignees recurrence and status`() {
        val categories = listOf(
            1L to FamilyActionType.HOUSE_QUEST.category,
            2L to FamilyActionType.PERSONAL_ROUTINE.category,
            3L to FamilyActionType.PERSONAL_MISSION.category,
        )
        val definitions = categories.map { (id, category) -> definition(id, category) }
        val occurrences = listOf(
            occurrence(3L, FamilyTaskStatus.VALIDATED),
            occurrence(1L, FamilyTaskStatus.COMPLETED),
            occurrence(2L, FamilyTaskStatus.PENDING_VALIDATION),
        )

        val enriched = attachFamilyActionCategories(occurrences, definitions, 7L)

        assertEquals(listOf(FamilyActionType.PERSONAL_MISSION, FamilyActionType.HOUSE_QUEST, FamilyActionType.PERSONAL_ROUTINE),
            enriched.map { FamilyActionType.fromCategory(it.category) })
        assertEquals(occurrences.map { it.status }, enriched.map { it.status })
    }

    @Test fun `missing or foreign definition fails instead of inventing a house quest`() {
        assertThrows(IllegalStateException::class.java) { attachFamilyActionCategories(listOf(occurrence(8L)), listOf(definition(7L, "Maison")), 7L) }
        assertThrows(IllegalStateException::class.java) { attachFamilyActionCategories(listOf(occurrence(7L)), listOf(definition(7L, "Maison", familyId = 8L)), 7L) }
        assertThrows(IllegalArgumentException::class.java) { attachFamilyActionCategories(listOf(occurrence(7L)), listOf(definition(7L, "UNRECOGNISED")), 7L) }
    }

    @Test fun `editing cannot change the action category`() {
        requireUnchangedFamilyActionCategory("Maison", "Maison")
        assertThrows(IllegalStateException::class.java) {
            requireUnchangedFamilyActionCategory(FamilyActionType.PERSONAL_ROUTINE.category, FamilyActionType.PERSONAL_MISSION.category)
        }
    }

    private fun definition(id: Long, category: String?, familyId: Long = 7L) = FamilyTaskDefinition(
        id = id, familyId = familyId, title = "Action $id", description = null,
        assignees = listOf(FamilyTaskAssignee(25L, "Papa")), dueDate = "2026-10-04", dueTime = null,
        hasDueTime = false, dueAt = null, recurrence = FamilyTaskRecurrence.DAILY,
        selectedWeekdays = emptyList(), validationRequired = false, gamificationEnabled = false,
        priority = FamilyTaskPriority.NORMAL, active = true, category = category,
    )

    private fun occurrence(taskId: Long, status: FamilyTaskStatus = FamilyTaskStatus.TODO) = FamilyTaskTodayItem(
        taskId = taskId, occurrenceId = taskId + 100L, title = "Action $taskId",
        assignees = listOf(FamilyTaskAssignee(25L, "Papa")), scheduledDate = "2026-10-04",
        dueDate = "2026-10-04", dueTime = null, hasDueTime = false, dueAt = null,
        status = status, validationRequired = false, gamificationEnabled = false,
        priority = FamilyTaskPriority.NORMAL, recurrence = FamilyTaskRecurrence.DAILY,
    )
}
