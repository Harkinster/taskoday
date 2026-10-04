package com.example.taskoday.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilyActionTypeTest {
    @Test fun `documented historical categories remain collective`() {
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory(null))
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory("Maison"))
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory("  maison  "))
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory("TASKODAY_HOUSE_QUEST"))
    }

    @Test fun `unexpected category cannot silently become a house quest`() {
        assertThrows(IllegalArgumentException::class.java) { FamilyActionType.fromCategory("MISSION_LEGACY_UNKNOWN") }
    }

    @Test fun `explicit personal category does not depend on recurrence or assignees`() {
        assertEquals(FamilyActionType.PERSONAL_ROUTINE, FamilyActionType.fromCategory("TASKODAY_PERSONAL_ROUTINE"))
        assertEquals(FamilyActionType.PERSONAL_MISSION, FamilyActionType.fromCategory("TASKODAY_PERSONAL_MISSION"))
    }
}
