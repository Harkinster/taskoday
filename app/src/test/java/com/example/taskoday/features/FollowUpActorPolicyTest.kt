package com.example.taskoday.features.followup

import com.example.taskoday.data.remote.dto.FamilyTaskOccurrenceDto
import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.domain.model.FamilyTaskActor
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpActorPolicyTest {
    @Test
    fun `house task completed by parent shows actor`() {
        assertEquals(listOf("Terminée par Matthieu"), labels(task(completed = FamilyTaskActor(1L, "Matthieu"))))
    }

    @Test
    fun `house task completed by child shows child`() {
        assertEquals(listOf("Terminée par Naomy"), labels(task(completed = FamilyTaskActor(2L, "Naomy"))))
    }

    @Test
    fun `validation shows distinct completion and validation actors`() {
        assertEquals(
            listOf("Faite par Naomy", "Validée par Matthieu"),
            labels(task(validationRequired = true, completed = FamilyTaskActor(2L, "Naomy"), validated = FamilyTaskActor(1L, "Matthieu"))),
        )
    }

    @Test
    fun `missing actors and reopen stay silent`() {
        assertTrue(labels(task()).isEmpty())
        assertTrue(labels(task(status = FamilyTaskStatus.TODO)).isEmpty())
    }

    @Test
    fun `legacy payload without actor objects remains compatible`() {
        val domain = Gson().fromJson("{\"task_id\": 1, \"occurrence_id\": 2, \"title\": \"Maison\", \"status\": \"COMPLETED\"}", FamilyTaskOccurrenceDto::class.java).toDomain()
        assertTrue(labels(domain).isEmpty())
    }

    @Test
    fun `invalid backend actor profile is ignored`() {
        val domain = Gson().fromJson("{\"task_id\": 1, \"occurrence_id\": 2, \"title\": \"Maison\", \"status\": \"COMPLETED\", \"completed_by_user\": {\"user_id\": 3}}", FamilyTaskOccurrenceDto::class.java).toDomain()
        assertTrue(labels(domain).isEmpty())
    }

    @Test
    fun `personal task does not get unnecessary actor label`() {
        val personal = task(assignees = listOf(FamilyTaskAssignee(9L, "Alex")), completed = FamilyTaskActor(1L, "Matthieu"))
        assertTrue(labels(personal).isEmpty())
    }

    @Test
    fun `dto maps both actor objects from backend`() {
        val domain = Gson().fromJson("{\"task_id\": 1, \"occurrence_id\": 2, \"title\": \"Maison\", \"status\": \"VALIDATED\", \"completed_by_user\": {\"user_id\": 2, \"display_name\": \"Naomy\"}, \"validated_by_user\": {\"user_id\": 1, \"display_name\": \"Matthieu\"}}", FamilyTaskOccurrenceDto::class.java).toDomain()
        assertEquals("Naomy", domain.completedByUser?.displayName)
        assertEquals("Matthieu", domain.validatedByUser?.displayName)
    }

    private fun labels(task: FamilyTaskTodayItem): List<String> = familyTaskCompletionActorLabels(task)

    private fun task(
        status: FamilyTaskStatus = FamilyTaskStatus.COMPLETED,
        assignees: List<FamilyTaskAssignee> = emptyList(),
        validationRequired: Boolean = false,
        completed: FamilyTaskActor? = null,
        validated: FamilyTaskActor? = null,
    ) = FamilyTaskTodayItem(
        taskId = 1L,
        occurrenceId = 2L,
        title = "Maison",
        assignees = assignees,
        scheduledDate = "2026-09-20",
        dueDate = "2026-09-20",
        dueTime = null,
        hasDueTime = false,
        dueAt = null,
        status = status,
        validationRequired = validationRequired,
        gamificationEnabled = false,
        priority = FamilyTaskPriority.NORMAL,
        completedByUser = completed,
        validatedByUser = validated,
    )
}
