package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyHomeDisplayPolicyTest {
    @Test
    fun `today sections preserve real status groups and include undated missions once`() {
        val todo = row(task(1, "À faire", FamilyTaskStatus.TODO))
        val active = row(task(2, "En cours", FamilyTaskStatus.IN_PROGRESS))
        val pending = row(task(3, "À valider", FamilyTaskStatus.PENDING_VALIDATION))
        val undated = row(task(4, "Mission sans date", FamilyTaskStatus.TODO, FamilyActionType.HOUSE_MISSION.category, dueDate = null))
        val undatedPending = row(task(7, "Mission sans date à valider", FamilyTaskStatus.PENDING_VALIDATION, FamilyActionType.HOUSE_MISSION.category, dueDate = null))
        val overdue = row(task(5, "En retard", FamilyTaskStatus.TODO))
        val completed = row(task(6, "Terminée", FamilyTaskStatus.VALIDATED))

        val sections = FamilyHomeUiState(
            todayTasks = listOf(todo, active, pending),
            openUndatedTasks = listOf(undated, undatedPending),
            overdueTasks = listOf(overdue),
            completedTodayTasks = listOf(completed),
            totalTasks = 3,
            access = FamilyTaskAccessPolicy(userId = 1L, role = "PARENT"),
        ).todayDisplaySections()

        assertEquals(listOf(1L), sections.toDo.map { it.task.occurrenceId })
        assertEquals(listOf(4L), sections.withoutDueDate.map { it.task.occurrenceId })
        assertEquals(listOf(2L), sections.inProgress.map { it.task.occurrenceId })
        assertEquals(listOf(3L, 7L), sections.pendingValidation.map { it.task.occurrenceId })
        assertEquals(listOf(5L), sections.overdue.map { it.task.occurrenceId })
        assertEquals(listOf(6L), sections.completed.map { it.task.occurrenceId })
        assertEquals(4, sections.toDo.size + sections.inProgress.size + sections.pendingValidation.size)
        assertFalse(sections.isEmpty)
    }

    @Test
    fun `empty successful state has no sections while loading and errors are separate state`() {
        val sections = FamilyHomeUiState().todayDisplaySections()
        assertTrue(sections.isEmpty)
        assertTrue(FamilyHomeUiState(isLoading = true).isLoading)
        assertEquals("Réseau indisponible", FamilyHomeUiState(errorMessage = "Réseau indisponible").errorMessage)
    }

    @Test
    fun `empty state is shown only after access resolves and all requests succeed`() {
        val parent = FamilyTaskAccessPolicy(userId = 10L, role = "PARENT")
        val child = FamilyTaskAccessPolicy(userId = 11L, role = "CHILD")

        assertTrue(FamilyHomeUiState(isLoading = false, access = parent).shouldShowTodayEmptyState())
        assertTrue(FamilyHomeUiState(isLoading = false, access = child).shouldShowTodayEmptyState())
        assertFalse(FamilyHomeUiState(isLoading = true, access = parent).shouldShowTodayEmptyState())
        assertFalse(FamilyHomeUiState(errorMessage = "Réseau indisponible", access = parent).shouldShowTodayEmptyState())
        assertFalse(FamilyHomeUiState(secondaryErrorMessage = "Retards indisponibles", access = parent).shouldShowTodayEmptyState())
        assertFalse(FamilyHomeUiState(access = FamilyTaskAccessPolicy()).hasResolvedHouseholdAccess())
        assertFalse(FamilyHomeUiState(access = FamilyTaskAccessPolicy(1L, "UNKNOWN")).hasResolvedHouseholdAccess())
    }

    @Test
    fun `summary never describes unavailable data as an empty house`() {
        assertEquals("Informations indisponibles", familyHomeSummaryText(0, FamilyHomeMode.TODAY, dataUnavailable = true))
        assertEquals("Aucune action prévue aujourd'hui", familyHomeSummaryText(0, FamilyHomeMode.TODAY, dataUnavailable = false))
        assertEquals("4 actions prévues aujourd'hui", familyHomeSummaryText(4, FamilyHomeMode.TODAY, dataUnavailable = false))
    }

    @Test
    fun `fantasy summary count is compact and remains neutral on errors`() {
        assertEquals("Les compteurs sont indisponibles.", familyHomeSummaryCountText(0, dataUnavailable = true))
        assertEquals("Aucune action pr\u00e9vue", familyHomeSummaryCountText(0, dataUnavailable = false))
        assertEquals("1 action pr\u00e9vue", familyHomeSummaryCountText(1, dataUnavailable = false))
        assertEquals("3 actions pr\u00e9vues", familyHomeSummaryCountText(3, dataUnavailable = false))
    }

    @Test
    fun `parent can manage actions but child cannot manage or validate`() {
        val parent = FamilyTaskAccessPolicy(userId = 10L, role = "PARENT")
        val child = FamilyTaskAccessPolicy(userId = 11L, role = "CHILD")
        val awaitingValidation = task(12, "À valider", FamilyTaskStatus.PENDING_VALIDATION)

        assertTrue(parent.canManage)
        assertEquals(FamilyTaskQuickAction.VALIDATE, parent.quickAction(awaitingValidation))
        assertFalse(child.canManage)
        assertEquals(null, child.quickAction(awaitingValidation))
    }

    private fun row(task: FamilyTaskTodayItem) = FamilyTaskRow(task)

    private fun task(
        occurrenceId: Long,
        title: String,
        status: FamilyTaskStatus,
        category: String = FamilyActionType.HOUSE_ROUTINE.category,
        dueDate: String? = "2026-10-09",
    ) = FamilyTaskTodayItem(
        taskId = occurrenceId,
        occurrenceId = occurrenceId,
        title = title,
        assignees = listOf(FamilyTaskAssignee(11L, "Léa"), FamilyTaskAssignee(12L, "Mila")),
        scheduledDate = dueDate,
        dueDate = dueDate,
        dueTime = null,
        hasDueTime = false,
        dueAt = null,
        status = status,
        validationRequired = status == FamilyTaskStatus.PENDING_VALIDATION,
        gamificationEnabled = false,
        priority = FamilyTaskPriority.NORMAL,
        category = category,
    )
}
