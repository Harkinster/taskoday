package com.example.taskoday.features.activity

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.model.FamilyTaskEventType
import com.example.taskoday.domain.model.FamilyTaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityJournalPolicyTest {
    private val today = LocalDate.of(2026, 10, 5)

    @Test fun `event and category labels are human readable`() {
        assertEquals("Routine", FamilyActionType.PERSONAL_ROUTINE.journalLabel())
        assertEquals("Mission", FamilyActionType.PERSONAL_MISSION.journalLabel())
        assertEquals("Quête Maison", FamilyActionType.HOUSE_QUEST.journalLabel())
        assertEquals("Routine Maison", FamilyActionType.HOUSE_ROUTINE.journalLabel())
        assertEquals("Mission Maison", FamilyActionType.HOUSE_MISSION.journalLabel())
        assertEquals("Terminée", event(1, FamilyTaskEventType.COMPLETE).journalEventLabel())
        assertEquals("Terminée · en attente de validation", event(2, FamilyTaskEventType.COMPLETE, FamilyTaskStatus.PENDING_VALIDATION).journalEventLabel())
        assertEquals("Validée", event(3, FamilyTaskEventType.VALIDATE).journalEventLabel())
        assertEquals("Rouverte", event(4, FamilyTaskEventType.REOPEN).journalEventLabel())
        assertEquals("Faite par Naomy", event(1, FamilyTaskEventType.COMPLETE).journalActorLabel())
        assertEquals("Validée par Naomy", event(1, FamilyTaskEventType.VALIDATE).journalActorLabel())
    }

    @Test fun `seven day sections retain every transition of repeated cycles`() {
        val events = listOf(
            event(1, FamilyTaskEventType.COMPLETE, at = "2026-10-05T16:00:00Z"),
            event(2, FamilyTaskEventType.VALIDATE, at = "2026-10-05T16:05:00Z"),
            event(3, FamilyTaskEventType.REOPEN, at = "2026-10-05T16:10:00Z"),
            event(4, FamilyTaskEventType.COMPLETE, at = "2026-10-05T16:20:00Z"),
            event(5, FamilyTaskEventType.VALIDATE, at = "2026-10-05T16:25:00Z"),
            event(6, FamilyTaskEventType.COMPLETE, at = "2026-10-04T12:00:00Z"),
            event(7, FamilyTaskEventType.COMPLETE, at = "2026-10-01T12:00:00Z"),
            event(8, FamilyTaskEventType.COMPLETE, at = "2026-09-28T12:00:00Z"),
        )
        val sections = journalSections(events, today, ZoneOffset.UTC)
        assertEquals(listOf(JournalPeriod.TODAY, JournalPeriod.YESTERDAY, JournalPeriod.LAST_WEEK), sections.map { it.period })
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), sections[0].events.map { it.id })
        assertEquals(listOf(6L), sections[1].events.map { it.id })
        assertEquals(listOf(7L), sections[2].events.map { it.id })
    }

    @Test fun `parent filters actor and participant while child has no member selector`() {
        val childAction = event(1, FamilyTaskEventType.VALIDATE).copy(actorUserId = 25, participantUserIds = listOf(27))
        val parent = ActivityJournalUiState(isParent = true, events = listOf(childAction), selectedMemberId = 27)
        assertEquals(listOf(childAction), parent.visibleEvents)
        assertEquals(emptyList<FamilyTaskEvent>(), parent.copy(selectedType = FamilyActionType.HOUSE_QUEST).visibleEvents)
        val child = ActivityJournalUiState(isParent = false, events = listOf(childAction), selectedMemberId = 28)
        assertEquals(listOf(childAction), child.visibleEvents)
    }

    private fun event(id: Long, type: FamilyTaskEventType, status: FamilyTaskStatus = FamilyTaskStatus.COMPLETED, at: String = "2026-10-05T12:00:00Z") = FamilyTaskEvent(
        id, 7, 9, 11, FamilyActionType.PERSONAL_MISSION, "Faire les devoirs", type,
        FamilyTaskStatus.TODO, status, 27, "Naomy", 27, listOf(27), Instant.parse(at), false,
    )
}
