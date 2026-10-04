package com.example.taskoday.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyActionTypeTest {
    @Test fun `historical and unknown categories remain collective`() {
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory(null))
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromCategory("Maison"))
    }

    @Test fun `explicit personal category does not depend on recurrence or assignees`() {
        assertEquals(FamilyActionType.PERSONAL_ROUTINE, FamilyActionType.fromCategory("TASKODAY_PERSONAL_ROUTINE"))
        assertEquals(FamilyActionType.PERSONAL_MISSION, FamilyActionType.fromCategory("TASKODAY_PERSONAL_MISSION"))
    }
}
