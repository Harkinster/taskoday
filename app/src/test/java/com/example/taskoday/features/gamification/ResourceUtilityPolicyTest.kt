package com.example.taskoday.features.gamification

import com.example.taskoday.data.remote.dto.ChestDropDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceUtilityPolicyTest {
    @Test fun `chest affordability respects exact provisional costs`() {
        assertFalse(canOpenChest(2, 3)); assertTrue(canOpenChest(3, 3))
        assertFalse(canOpenChest(7, 8)); assertTrue(canOpenChest(8, 8))
        assertFalse(canOpenChest(14, 15)); assertTrue(canOpenChest(15, 15))
        assertFalse(canOpenChest(99, 0))
    }

    @Test fun `chest reveal uses persisted human readable drop labels`() {
        assertEquals("2 Pomme dragon, 1 Rune ancienne", chestRevealText(listOf(
            ChestDropDto("pomme_dragon", "Pomme dragon", 2),
            ChestDropDto("rune_ancienne", "Rune ancienne", 1),
        )))
    }
}
