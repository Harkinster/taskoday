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
    ): FamilyTaskTodayItem =
        FamilyTaskTodayItem(
            taskId = title.hashCode().toLong(),
            occurrenceId = title.hashCode().toLong(),
            title = title,
            assignees = assignees,
            scheduledDate = "2026-08-22",
            dueDate = "2026-08-22",
            dueTime = null,
            hasDueTime = false,
            dueAt = null,
            status = status,
            validationRequired = false,
            gamificationEnabled = false,
            priority = FamilyTaskPriority.NORMAL,
        )
}
