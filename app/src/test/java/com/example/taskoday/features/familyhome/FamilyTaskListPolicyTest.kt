package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTaskListPolicyTest {
    @Test
    fun `filters include all house and real family members`() {
        val filters =
            buildFamilyTaskListFilters(
                listOf(
                    member(userId = 10L, displayName = "Parent Test", role = FamilyTaskMemberRole.PARENT),
                    member(userId = 20L, displayName = "Enfant Test", role = FamilyTaskMemberRole.CHILD),
                ),
            )

        assertEquals(listOf("all", "house", "member_20", "member_10"), filters.map { filter -> filter.key })
        assertEquals(listOf("Toutes", "Maison", "Enfant Test", "Parent Test"), filters.map { filter -> filter.label })
    }

    @Test
    fun `empty task list stays empty`() {
        assertEquals(emptyList<FamilyTaskDefinition>(), filterFamilyTaskDefinitions(emptyList(), FAMILY_TASK_FILTER_ALL))
    }

    @Test
    fun `house filter keeps only unassigned tasks`() {
        val house = task(id = 1L, title = "Ranger l'entrée", assignees = emptyList())
        val assigned = task(id = 2L, title = "Cartable", assignees = listOf(assignee(20L, "Enfant Test")))

        assertEquals(listOf(house), filterFamilyTaskDefinitions(listOf(house, assigned), FAMILY_TASK_FILTER_HOUSE))
        assertEquals("Maison", house.assignmentLabel())
    }

    @Test
    fun `member filters match parent child and multi assigned tasks`() {
        val parentTask = task(id = 1L, title = "Papiers", assignees = listOf(assignee(10L, "Parent Test")))
        val childTask = task(id = 2L, title = "Cartable", assignees = listOf(assignee(20L, "Enfant Test")))
        val sharedTask = task(id = 3L, title = "Table", assignees = listOf(assignee(10L, "Parent Test"), assignee(20L, "Enfant Test")))

        assertEquals(
            listOf(parentTask, sharedTask),
            filterFamilyTaskDefinitions(listOf(parentTask, childTask, sharedTask), familyTaskMemberFilterKey(10L)),
        )
        assertEquals(
            listOf(childTask, sharedTask),
            filterFamilyTaskDefinitions(listOf(parentTask, childTask, sharedTask), familyTaskMemberFilterKey(20L)),
        )
    }

    @Test
    fun `inactive tasks are not shown`() {
        val active = task(id = 1L, title = "Active")
        val inactive = task(id = 2L, title = "Inactive", active = false)

        assertEquals(listOf(active), filterFamilyTaskDefinitions(listOf(active, inactive), FAMILY_TASK_FILTER_ALL))
    }

    @Test
    fun `recurrence labels stay human readable`() {
        val selected =
            task(
                id = 1L,
                title = "Sport",
                recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                selectedWeekdays = listOf(1, 3, 5),
            )
        val daily = task(id = 2L, title = "Vaisselle", recurrence = FamilyTaskRecurrence.DAILY)

        assertTrue(selected.listMetadataLabels().contains("Lun. • Mer. • Ven."))
        assertTrue(daily.listMetadataLabels().contains("Tous les jours"))
    }

    @Test
    fun `schedule label distinguishes date without time from real midnight`() {
        assertEquals(
            "22 août",
            task(id = 1L, dueDate = "2026-08-22", dueTime = null, hasDueTime = false).scheduleLabel(),
        )
        assertEquals(
            "22 août à 00:00",
            task(id = 2L, dueDate = "2026-08-22", dueTime = "00:00", hasDueTime = true).scheduleLabel(),
        )
    }

    private fun member(
        userId: Long,
        displayName: String,
        role: FamilyTaskMemberRole,
    ): FamilyTaskMember =
        FamilyTaskMember(
            userId = userId,
            displayName = displayName,
            email = null,
            role = role,
            isActive = true,
        )

    private fun assignee(
        id: Long,
        displayName: String,
    ): FamilyTaskAssignee =
        FamilyTaskAssignee(id = id, displayName = displayName)

    private fun task(
        id: Long,
        title: String = "Tâche",
        assignees: List<FamilyTaskAssignee> = emptyList(),
        dueDate: String? = "2026-08-22",
        dueTime: String? = null,
        hasDueTime: Boolean = false,
        recurrence: FamilyTaskRecurrence = FamilyTaskRecurrence.NONE,
        selectedWeekdays: List<Int> = emptyList(),
        active: Boolean = true,
    ): FamilyTaskDefinition =
        FamilyTaskDefinition(
            id = id,
            familyId = 4L,
            title = title,
            description = null,
            assignees = assignees,
            dueDate = dueDate,
            dueTime = dueTime,
            hasDueTime = hasDueTime,
            dueAt = null,
            recurrence = recurrence,
            selectedWeekdays = selectedWeekdays,
            validationRequired = false,
            gamificationEnabled = false,
            priority = FamilyTaskPriority.NORMAL,
            active = active,
        )
}

