package com.example.taskoday.features.activity

import com.example.taskoday.domain.model.*
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalJournalPresentationTest {
    private fun event(id: Long, owner: Long, actor: Long = owner) = FamilyTaskEvent(
        id = id, familyId = 1, taskId = id, occurrenceId = id,
        category = FamilyActionType.PERSONAL_MISSION, title = "Mission $id",
        eventType = FamilyTaskEventType.VALIDATE, statusFrom = FamilyTaskStatus.PENDING_VALIDATION,
        statusTo = FamilyTaskStatus.VALIDATED, actorUserId = actor, actorName = "Parent",
        completedByUserId = owner, participantUserIds = listOf(owner),
        occurredAt = Instant.parse("2026-10-25T10:00:00Z"), legacyInferred = false,
    )

    @Test fun `parent personal journal excludes family work and validations for another member`() {
        val own = event(1, 10)
        val child = event(2, 20, actor = 10)
        val house = event(3, 10).copy(scope = FamilyActionScope.HOUSE, category = FamilyActionType.HOUSE_MISSION)
        assertEquals(listOf(own), personalJournalEvents(listOf(own, child, house), 10))
    }

    @Test fun `child journal retains own mission validated by parent`() {
        val validated = event(1, 20, actor = 10)
        assertEquals(listOf(validated), personalJournalEvents(listOf(validated, event(2, 21)), 20))
    }

    @Test fun `unresolved identity does not display the family journal`() {
        assertEquals(emptyList<FamilyTaskEvent>(), personalJournalEvents(listOf(event(1, 20)), null))
    }
}
