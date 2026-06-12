package com.example.taskoday.features.gamification

import com.example.taskoday.core.ui.format.toTaskodayDisplayLabel
import com.example.taskoday.data.remote.dto.EggDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.InventoryItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NestUiPolicyTest {
    @Test
    fun `egg ownership makes a bestiary family discovered`() {
        assertTrue(isBestiaryFamilyDiscovered(discovered = false, eggOwned = true, dragonOwned = false))
    }

    @Test
    fun `technical labels are translated for display`() {
        assertEquals("Pomme dragon", "pomme_dragon".toTaskodayDisplayLabel())
        assertEquals("Fragment d'œuf", "fragment_oeuf".toTaskodayDisplayLabel())
        assertEquals("Commun", "common".toTaskodayDisplayLabel())
        assertEquals("Épique", "epic".toTaskodayDisplayLabel())
        assertEquals("Matériau", "material".toTaskodayDisplayLabel())
        assertEquals("Artefact", "artifact".toTaskodayDisplayLabel())
    }

    @Test
    fun `missing resources disable egg evolution`() {
        val state =
            eggEvolutionActionState(
                egg = braiseEgg(),
                inventory = inventory(pommeDragon = 2, petitCristal = 2, pierreChaude = 1),
            )

        assertFalse(state.enabled)
        assertEquals("Ressources insuffisantes", state.label)
    }

    @Test
    fun `backend requirements allow evolution even when progress is zero`() {
        val state =
            eggEvolutionActionState(
                egg = braiseEgg(),
                inventory = inventory(pommeDragon = 10, petitCristal = 7, pierreChaude = 1),
            )

        assertTrue(state.enabled)
        assertEquals("Évoluer", state.label)
        assertTrue(state.requirementsLabel.startsWith("Prêt à évoluer"))
    }

    @Test
    fun `submitting disables a second evolution request`() {
        val state =
            eggEvolutionActionState(
                egg = braiseEgg(),
                inventory = inventory(pommeDragon = 10, petitCristal = 7, pierreChaude = 1),
                isSubmitting = true,
            )

        assertFalse(state.enabled)
        assertEquals("Évolution...", state.label)
    }

    private fun braiseEgg() =
        EggDto(
            id = 1,
            childId = 19,
            eggKey = "oeuf_braise",
            title = "Oeuf braise",
            status = "available",
            state = "sleeping",
            progressPercent = 0,
            nextState = "warm",
            assetKey = "oeuf_braise_sleeping",
            requirements = mapOf("pomme_dragon" to 3, "petit_cristal" to 2, "pierre_chaude" to 1),
        )

    private fun inventory(
        pommeDragon: Int,
        petitCristal: Int,
        pierreChaude: Int,
    ) = InventoryDto(
        childId = 19,
        currencies = emptyMap(),
        items =
            listOf(
                InventoryItemDto("pomme_dragon", "Pomme dragon", "common", "consumable", pommeDragon),
                InventoryItemDto("petit_cristal", "Petit cristal", "common", "material", petitCristal),
                InventoryItemDto("pierre_chaude", "Pierre chaude", "common", "material", pierreChaude),
            ),
        chests = emptyList(),
    )
}
