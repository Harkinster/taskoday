package com.example.taskoday.features.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionSelectionPolicyTest {
    @Test
    fun `all eight stable backend lineage ids have local name suggestions`() {
        assertEquals(
            setOf("FULMIO", "SYLVYN", "PHENOR", "LUNARYS", "PYRON", "CHRONYX", "AMBRIO", "CRISTAO"),
            companionLineageIds.toSet(),
        )
        companionLineageIds.forEach { id ->
            val suggestions = lineageSuggestions(id)
            assertTrue("$id needs multiple suggestions", suggestions.size >= 3)
            assertTrue(suggestions.none { it.equals(id, ignoreCase = true) })
        }
    }

    @Test
    fun `name validation follows personal companion input rules`() {
        assertNull(companionNameError("Voltís"))
        assertNull(companionNameError("Lune-d'Or"))
        assertNull(companionNameError("Dragon Étoilé"))
        assertNotNull(companionNameError("   "))
        assertNotNull(companionNameError("12345"))
        assertNotNull(companionNameError("x".repeat(33)))
        assertNotNull(companionNameError("Dragon\nNom"))
    }

    @Test
    fun `suggested names are editable local values and do not invoke network`() {
        val first = lineageSuggestions("PYRON").first()
        val second = requireNotNull(alternateNameSuggestion("PYRON", first))
        assertFalse(first == second)
        assertEquals("Flamaris", first)
        assertTrue(lineageSuggestions("PYRON").contains(second))
    }
}
