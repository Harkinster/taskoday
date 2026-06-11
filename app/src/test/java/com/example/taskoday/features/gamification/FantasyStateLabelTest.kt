package com.example.taskoday.features.gamification

import org.junit.Assert.assertEquals
import org.junit.Test

class FantasyStateLabelTest {
    @Test
    fun `technical egg and dragon states are translated`() {
        assertEquals("Endormi", "sleeping".toFantasyStateLabel())
        assertEquals("Éclosion", "hatching".toFantasyStateLabel())
        assertEquals("Bébé", "baby".toFantasyStateLabel())
        assertEquals("Légendaire", "legendary".toFantasyStateLabel())
    }
}
