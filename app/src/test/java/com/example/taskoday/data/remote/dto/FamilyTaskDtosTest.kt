package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
                  "due_date": "2026-08-22",
                  "due_time": "18:30",
                  "has_due_time": true,
                  "due_at": "2026-08-22T18:30:00Z",
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
        assertEquals("2026-08-22", dto.dueDate)
        assertEquals("18:30", dto.dueTime)
        assertTrue(dto.hasDueTime == true)
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
                dueDate = "2026-08-22",
                dueTime = "00:00",
                hasDueTime = true,
            ).toDomain()

        assertEquals(42L, item.taskId)
        assertEquals(700L, item.occurrenceId)
        assertEquals("Sortir les poubelles", item.title)
        assertEquals("Ada", item.assignees.single().displayName)
        assertEquals(FamilyTaskStatus.VALIDATED, item.status)
        assertEquals(FamilyTaskPriority.URGENT, item.priority)
        assertEquals("2026-08-22", item.dueDate)
        assertEquals("00:00", item.dueTime)
        assertTrue(item.hasDueTime)
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
    fun `family members payload maps parents children and active flag`() {
        val payload =
            JsonParser.parseString(
                """
                [
                  {"user_id": 10, "email": "parent@example.test", "display_name": "Parent", "role": "PARENT", "is_active": true},
                  {"user_id": 11, "email": "second@example.test", "display_name": "Second Parent", "role": "PARENT", "is_active": true},
                  {"user_id": 12, "email": "ada@example.test", "display_name": "Ada", "role": "CHILD", "is_active": false}
                ]
                """.trimIndent(),
            )

        val members = payload.toFamilyTaskMemberDtos(gson).map { member -> member.toDomain() }

        assertEquals(listOf(10L, 11L, 12L), members.map { member -> member.userId })
        assertEquals("Parent", members[0].displayName)
        assertEquals("Second Parent", members[1].displayName)
        assertEquals("Ada", members[2].displayName)
        assertEquals(FamilyTaskMemberRole.PARENT, members[0].role)
        assertEquals(FamilyTaskMemberRole.PARENT, members[1].role)
        assertEquals(FamilyTaskMemberRole.CHILD, members[2].role)
        assertTrue(members[0].isActive)
        assertTrue(members[1].isActive)
        assertFalse(members[2].isActive)
    }

    @Test
    fun `create request maps to backend snake case contract`() {
        val request =
            FamilyTaskCreateInput(
                title = "Sortir les poubelles",
                description = "Bac jaune",
                dueDate = "2026-08-22",
                dueTime = "18:30",
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
        assertEquals("2026-08-22", json["due_date"].asString)
        assertEquals("18:30", json["due_time"].asString)
        assertFalse(json.has("due_at"))
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
                dueDate = "2026-08-22",
                dueTime = null,
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
        assertEquals("2026-08-22", json["due_date"].asString)
        assertTrue(json["due_time"].isJsonNull)
        assertFalse(json.has("due_at"))
        assertEquals(0, json["assignee_user_ids"].asJsonArray.size())
        assertFalse(json.has("selected_weekdays") && !json["selected_weekdays"].isJsonNull)
        assertFalse(json["validation_required"].asBoolean)
        assertFalse(json["gamification_enabled"].asBoolean)
    }

    @Test
    fun `task definition payload maps to domain contract`() {
        val payload =
            JsonParser.parseString(
                """
                [
                  {
                    "id": 91,
                    "family_id": 4,
                    "title": "Smoke Maison Codex",
                    "description": "Controle de smoke",
                    "due_date": "2026-08-22",
                    "due_time": "18:30",
                    "has_due_time": true,
                    "due_at": "2026-08-22T18:30:00Z",
                    "recurrence": "SELECTED_WEEKDAYS",
                    "selected_weekdays": [5, 1, 3],
                    "assignees": [
                      {"user_id": 10, "display_name": "Parent Test"},
                      {"user_id": 20, "display_name": "Enfant Test"}
                    ],
                    "validation_required": true,
                    "gamification_enabled": false,
                    "priority": "urgent",
                    "active": true
                  }
                ]
                """.trimIndent(),
            )

        val task = payload.toFamilyTaskDefinitionDtos(gson).single().toDomain()

        assertEquals(91L, task.id)
        assertEquals(4L, task.familyId)
        assertEquals("Smoke Maison Codex", task.title)
        assertEquals("Controle de smoke", task.description)
        assertEquals("2026-08-22", task.dueDate)
        assertEquals("18:30", task.dueTime)
        assertTrue(task.hasDueTime)
        assertEquals("2026-08-22T18:30:00Z", task.dueAt)
        assertEquals(FamilyTaskRecurrence.SELECTED_WEEKDAYS, task.recurrence)
        assertEquals(listOf(1, 3, 5), task.selectedWeekdays)
        assertEquals(listOf(10L, 20L), task.assignees.map { assignee -> assignee.id })
        assertTrue(task.validationRequired)
        assertFalse(task.gamificationEnabled)
        assertEquals(FamilyTaskPriority.URGENT, task.priority)
        assertTrue(task.active)
    }

    @Test
    fun `update request maps editable fields to backend snake case`() {
        val request =
            FamilyTaskCreateInput(
                title = "Smoke Maison modifie",
                description = "Nouvelle consigne",
                dueDate = "2026-08-23",
                dueTime = "09:15",
                recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                selectedWeekdays = listOf(1, 3, 5),
                assigneeUserIds = listOf(10L, 20L),
                validationRequired = true,
                gamificationEnabled = true,
                priority = FamilyTaskPriority.HIGH,
            ).toUpdateRequestDto()
        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("Smoke Maison modifie", json["title"].asString)
        assertEquals("Nouvelle consigne", json["description"].asString)
        assertEquals("HIGH", json["priority"].asString)
        assertEquals("2026-08-23", json["due_date"].asString)
        assertEquals("09:15", json["due_time"].asString)
        assertFalse(json.has("due_at"))
        assertEquals("SELECTED_WEEKDAYS", json["recurrence"].asString)
        assertEquals(1, json["selected_weekdays"].asJsonArray[0].asInt)
        assertEquals(3, json["selected_weekdays"].asJsonArray[1].asInt)
        assertEquals(5, json["selected_weekdays"].asJsonArray[2].asInt)
        assertEquals(10L, json["assignee_user_ids"].asJsonArray[0].asLong)
        assertEquals(20L, json["assignee_user_ids"].asJsonArray[1].asLong)
        assertTrue(json["validation_required"].asBoolean)
        assertTrue(json["gamification_enabled"].asBoolean)
    }

    @Test
    fun `update request can clear optional description and selected weekdays`() {
        val request =
            FamilyTaskCreateInput(
                title = "Routine simple",
                description = null,
                dueDate = "2026-08-23",
                dueTime = null,
                recurrence = FamilyTaskRecurrence.DAILY,
                selectedWeekdays = emptyList(),
                assigneeUserIds = emptyList(),
                validationRequired = false,
                gamificationEnabled = false,
                priority = FamilyTaskPriority.NORMAL,
            ).toUpdateRequestDto()
        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("", json["description"].asString)
        assertEquals("2026-08-23", json["due_date"].asString)
        assertTrue(json["due_time"].isJsonNull)
        assertFalse(json.has("due_at"))
        assertEquals("DAILY", json["recurrence"].asString)
        assertEquals(0, json["selected_weekdays"].asJsonArray.size())
        assertEquals(0, json["assignee_user_ids"].asJsonArray.size())
    }
}
