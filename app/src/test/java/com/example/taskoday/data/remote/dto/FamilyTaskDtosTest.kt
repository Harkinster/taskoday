package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTaskDtosTest {
    private val gson = Gson()

    @Test
    fun `snake case today task fields map to dto`() {
        val dto =
            gson.fromJson(
                """
                {
                  "task_id": 42,
                  "occurrence_id": 700,
                  "title": "Sortir les poubelles",
                  "assignees": [
                    {"child_profile_id": 12, "display_name": "Ada"}
                  ],
                  "scheduled_date": "2026-08-22",
                  "due_at": "18:30:00",
                  "status": "pending_validation",
                  "validation_required": true,
                  "gamification_enabled": false,
                  "priority": "high"
                }
                """.trimIndent(),
                FamilyTaskOccurrenceDto::class.java,
            )

        assertEquals(42L, dto.taskId)
        assertEquals(700L, dto.occurrenceId)
        assertEquals("Ada", dto.assignees.single().displayName)
        assertEquals("2026-08-22", dto.scheduledDate)
        assertEquals("18:30:00", dto.dueAt)
        assertTrue(dto.validationRequired == true)
        assertFalse(dto.gamificationEnabled == true)
    }

    @Test
    fun `dto maps to domain status priority and assignees`() {
        val item =
            FamilyTaskOccurrenceDto(
                taskId = 42L,
                occurrenceId = 700L,
                title = "  Sortir les poubelles  ",
                assignees = listOf(FamilyTaskAssigneeDto(id = 12L, displayName = "Ada")),
                status = "VALIDATED",
                validationRequired = true,
                gamificationEnabled = false,
                priority = "urgent",
            ).toDomain()

        assertEquals(42L, item.taskId)
        assertEquals(700L, item.occurrenceId)
        assertEquals("Sortir les poubelles", item.title)
        assertEquals("Ada", item.assignees.single().displayName)
        assertEquals(FamilyTaskStatus.VALIDATED, item.status)
        assertEquals(FamilyTaskPriority.URGENT, item.priority)
        assertTrue(item.validationRequired)
        assertFalse(item.gamificationEnabled)
    }

    @Test
    fun `today payload supports object or list data`() {
        val objectPayload =
            JsonParser.parseString(
                """
                {
                  "date": "2026-08-22",
                  "tasks": [
                    {"task_id": 1, "occurrence_id": 2, "title": "Table", "status": "todo"}
                  ]
                }
                """.trimIndent(),
            )
        val listPayload =
            JsonParser.parseString(
                """
                [
                  {"task_id": 3, "occurrence_id": 4, "title": "Vaisselle", "status": "validated"}
                ]
                """.trimIndent(),
            )

        assertEquals("2026-08-22", objectPayload.toFamilyTasksTodayResponseDto(gson).date)
        assertEquals(1, objectPayload.toFamilyTasksTodayResponseDto(gson).tasks.size)
        assertEquals(3L, listPayload.toFamilyTasksTodayResponseDto(gson).tasks.single().taskId)
    }
}
