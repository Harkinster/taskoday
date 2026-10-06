package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.*
import com.example.taskoday.navigation.TaskodayDestination
import com.example.taskoday.navigation.visibleAccountDestinations
import org.junit.Assert.*
import org.junit.Test

class AccountRolePolicyTest {
    private val first = listOf(FamilyTaskAssignee(101L, "First child"))
    private val second = listOf(FamilyTaskAssignee(102L, "Second child"))

    @Test fun `parent retains management and all family assignments`() {
        val access = FamilyTaskAccessPolicy(100L, "PARENT")
        assertTrue(access.canManage)
        assertTrue(access.canView(first))
        assertTrue(access.canView(second))
        assertTrue(access.canView(emptyList()))
    }

    @Test fun `first child sees own and household but not second child tasks`() {
        val access = FamilyTaskAccessPolicy(101L, "CHILD")
        assertFalse(access.canManage)
        assertTrue(access.canView(first))
        assertFalse(access.canView(second))
        assertTrue(access.canView(emptyList()))
    }

    @Test fun `second child sees own and household but not first child tasks`() {
        val access = FamilyTaskAccessPolicy(102L, "CHILD")
        assertFalse(access.canManage)
        assertFalse(access.canView(first))
        assertTrue(access.canView(second))
        assertTrue(access.canView(emptyList()))
    }

    @Test fun `shared assignment includes child and unresolved identity denies access`() {
        assertTrue(FamilyTaskAccessPolicy(101L, "CHILD").canView(first + second))
        assertFalse(FamilyTaskAccessPolicy().canManage)
        assertFalse(FamilyTaskAccessPolicy().canView(emptyList()))
        assertFalse(FamilyTaskAccessPolicy(101L, "UNKNOWN").canView(first))
    }

    @Test fun `child can complete accessible tasks but cannot validate or reopen`() {
        val access = FamilyTaskAccessPolicy(101L, "CHILD")
        val personalTodo = occurrence(FamilyTaskStatus.TODO).copy(
            category = FamilyActionType.PERSONAL_MISSION.category,
            scope = FamilyActionScope.PERSONAL,
            kind = FamilyActionKind.MISSION,
        )
        val personalPending = personalTodo.copy(status = FamilyTaskStatus.PENDING_VALIDATION)
        val personalCompleted = personalTodo.copy(status = FamilyTaskStatus.COMPLETED)
        assertEquals(FamilyTaskQuickAction.COMPLETE, access.quickAction(personalTodo))
        assertNull(access.quickAction(personalPending))
        assertNull(access.quickAction(personalCompleted))
        assertNull(access.quickAction(personalTodo.copy(assignees = second)))
        assertEquals(FamilyTaskQuickAction.START, access.quickAction(occurrence(FamilyTaskStatus.TODO)))
        assertEquals(FamilyTaskQuickAction.VALIDATE, FamilyTaskAccessPolicy(100L, "PARENT").quickAction(personalPending))
    }

    @Test fun `child navigation retains home exploration and nest but excludes follow up`() {
        val destinations = visibleAccountDestinations(isChild = true)
        assertFalse(destinations.contains(TaskodayDestination.FollowUp))
        assertTrue(destinations.contains(TaskodayDestination.FamilyHome))
        assertTrue(destinations.contains(TaskodayDestination.Exploration))
        assertTrue(destinations.contains(TaskodayDestination.Nest))
        assertEquals(visibleAccountDestinations(false).size - 1, destinations.size)
    }

    private fun occurrence(status: FamilyTaskStatus) = FamilyTaskTodayItem(
        1L, 2L, "Task", first, null, null, null, false, null, status, false, false, FamilyTaskPriority.NORMAL,
    )
}
