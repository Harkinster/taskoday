package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskRecurrence
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

    @Test
    fun `family children payload maps from envelope data array`() {
        val payload =
            JsonParser.parseString(
                """
                [
                  {"id": 11, "email": "ada@example.test", "display_name": "Ada"}
                ]
                """.trimIndent(),
            )

        val member = payload.toFamilyTaskMemberDtos(gson).single().toDomain()

        assertEquals(11L, member.userId)
        assertEquals("Ada", member.displayName)
    }

    @Test
    fun `create request maps to backend snake case contract`() {
        val request =
            FamilyTaskCreateInput(
                title = "Sortir les poubelles",
                description = "Bac jaune",
                dueAt = "2026-08-22T18:30:00Z",
                recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                selectedWeekdays = listOf(1, 3),
                assigneeUserIds = listOf(10L, 20L),
                validationRequired = true,
                gamificationEnabled = true,
                priority = FamilyTaskPriority.HIGH,
            ).toRequestDto()
        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("Sortir les poubelles", json["title"].asString)
        assertEquals("Bac jaune", json["description"].asString)
        assertEquals("HIGH", json["priority"].asString)
        assertEquals("2026-08-22T18:30:00Z", json["due_at"].asString)
        assertEquals("SELECTED_WEEKDAYS", json["recurrence"].asString)
        assertEquals(1, json["selected_weekdays"].asJsonArray[0].asInt)
        assertEquals(3, json["selected_weekdays"].asJsonArray[1].asInt)
        assertEquals(10L, json["assignee_user_ids"].asJsonArray[0].asLong)
        assertEquals(20L, json["assignee_user_ids"].asJsonArray[1].asLong)
        assertTrue(json["validation_required"].asBoolean)
        assertTrue(json["gamification_enabled"].asBoolean)
    }

    @Test
    fun `create request keeps unassigned house task empty`() {
        val request =
            FamilyTaskCreateInput(
                title = "Ranger l'entrée",
                description = null,
                dueAt = "2026-08-22T00:00:00Z",
                recurrence = FamilyTaskRecurrence.NONE,
                selectedWeekdays = emptyList(),
                assigneeUserIds = emptyList(),
                validationRequired = false,
                gamificationEnabled = false,
                priority = FamilyTaskPriority.NORMAL,
            ).toRequestDto()
        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("NORMAL", json["priority"].asString)
        assertEquals("NONE", json["recurrence"].asString)
        assertEquals(0, json["assignee_user_ids"].asJsonArray.size())
        assertFalse(json.has("selected_weekdays") && !json["selected_weekdays"].isJsonNull)
        assertFalse(json["validation_required"].asBoolean)
        assertFalse(json["gamification_enabled"].asBoolean)
    }
}
