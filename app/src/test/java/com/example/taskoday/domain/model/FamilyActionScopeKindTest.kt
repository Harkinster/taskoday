package com.example.taskoday.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilyActionScopeKindTest {
    @Test fun `five supported combinations map to explicit scope and kind`() {
        assertEquals(5, FamilyActionType.entries.size)
        FamilyActionType.entries.forEach { type ->
            assertEquals(type, FamilyActionType.fromWire(type.scope.name, type.kind.name, type.category))
        }
    }

    @Test fun `personal quest and mismatched category are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { FamilyActionType.fromWire("PERSONAL", "QUEST", null) }
        assertThrows(IllegalStateException::class.java) { FamilyActionType.fromWire("HOUSE", "ROUTINE", FamilyActionType.HOUSE_QUEST.category) }
    }

    @Test fun `legacy categories retain deterministic mapping`() {
        assertEquals(FamilyActionType.HOUSE_QUEST, FamilyActionType.fromWire(null, null, null))
        assertEquals(FamilyActionType.PERSONAL_MISSION, FamilyActionType.fromWire(null, null, "TASKODAY_PERSONAL_MISSION"))
        assertEquals(FamilyActionType.HOUSE_ROUTINE, FamilyActionType.fromWire("HOUSE", "ROUTINE", "TASKODAY_HOUSE_ROUTINE"))
    }
}
