package com.example.taskoday.features.gamification

import com.example.taskoday.core.ui.format.toTaskodayDisplayLabel
import com.example.taskoday.data.remote.dto.EggDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.InventoryItemDto
import com.example.taskoday.data.remote.dto.StateUnlockDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `no owned backend egg leaves nest without active egg`() {
        assertNull(selectActiveNestEgg(emptyList()))
        assertNull(
            selectActiveNestEgg(
                listOf(
                    eggUiItem(key = "egg_pyron", id = null, progressPercent = 80, nextStateLabel = "Lumineux"),
                    eggUiItem(key = "oeuf_braise", id = 1, progressPercent = 30, nextStateLabel = "Tiède", locked = true),
                ),
            ),
        )
    }

    @Test
    fun `single backend egg is selected for the nest`() {
        val selected =
            selectActiveNestEgg(
                listOf(
                    eggUiItem(key = "oeuf_braise", id = 1, progressPercent = 50, nextStateLabel = "Lumineux"),
                ),
            )

        assertEquals("oeuf_braise", selected?.key)
    }

    @Test
    fun `old sample key without backend id does not influence nest selection`() {
        val selected =
            selectActiveNestEgg(
                listOf(
                    eggUiItem(key = "egg_pyron", id = null, progressPercent = 90, nextStateLabel = "Éclosion"),
                    eggUiItem(key = "oeuf_braise", id = 12, progressPercent = 25, nextStateLabel = "Tiède"),
                ),
            )

        assertEquals("oeuf_braise", selected?.key)
    }

    @Test
    fun `multiple eggs prefer an egg currently in progress`() {
        val selected =
            selectActiveNestEgg(
                listOf(
                    eggUiItem(key = "oeuf_lunaire", id = 2, progressPercent = 0, nextStateLabel = "Tiède"),
                    eggUiItem(key = "oeuf_braise", id = 1, progressPercent = 40, nextStateLabel = "Lumineux"),
                ),
            )

        assertEquals("oeuf_braise", selected?.key)
    }

    @Test
    fun `multiple eggs fall back deterministically to the first owned egg`() {
        val selected =
            selectActiveNestEgg(
                listOf(
                    eggUiItem(key = "oeuf_lunaire", id = 2, progressPercent = 0, nextStateLabel = null),
                    eggUiItem(key = "oeuf_braise", id = 1, progressPercent = 0, nextStateLabel = null),
                ),
            )

        assertEquals("oeuf_lunaire", selected?.key)
    }

    @Test
    fun `sufficient resources expose readable requirement rows`() {
        val rows =
            eggResourceRows(
                egg = braiseEgg(),
                inventory = inventory(pommeDragon = 3, petitCristal = 2, pierreChaude = 1),
            )

        assertEquals(3, rows.size)
        assertTrue(rows.all { row -> row.missingQuantity == 0 })
        assertEquals("Pomme dragon", rows.first { row -> row.key == "pomme_dragon" }.title)
    }

    @Test
    fun `insufficient resources expose missing quantity`() {
        val rows =
            eggResourceRows(
                egg = braiseEgg(),
                inventory = inventory(pommeDragon = 1, petitCristal = 2, pierreChaude = 0),
            )

        assertEquals(2, rows.first { row -> row.key == "pomme_dragon" }.missingQuantity)
        assertEquals(1, rows.first { row -> row.key == "pierre_chaude" }.missingQuantity)
    }

    @Test
    fun `next state label is displayed when available`() {
        assertEquals("Tiède", eggNextStateLabel(braiseEgg(nextState = "warm")))
    }

    @Test
    fun `missing next state stays absent`() {
        assertNull(eggNextStateLabel(braiseEgg(nextState = null)))
    }

    @Test
    fun `dragon next stage follows the first locked stage after current stage`() {
        assertEquals(
            "Jeune",
            nextDragonStageLabel(
                currentStage = "baby",
                stages =
                    listOf(
                        StateUnlockDto("baby", unlocked = true),
                        StateUnlockDto("young", unlocked = false),
                        StateUnlockDto("adult", unlocked = false),
                    ),
            ),
        )
    }

    @Test
    fun `dragon next stage is absent at maximum stage`() {
        assertNull(
            nextDragonStageLabel(
                currentStage = "adult",
                stages =
                    listOf(
                        StateUnlockDto("baby", unlocked = true),
                        StateUnlockDto("young", unlocked = true),
                        StateUnlockDto("adult", unlocked = true),
                    ),
            ),
        )
    }

    @Test
    fun `dragon next stage falls back to first locked stage when current stage is unknown`() {
        assertEquals(
            "Adulte",
            nextDragonStageLabel(
                currentStage = "ancient",
                stages =
                    listOf(
                        StateUnlockDto("baby", unlocked = true),
                        StateUnlockDto("young", unlocked = true),
                        StateUnlockDto("medium", unlocked = false),
                    ),
            ),
        )
    }

    private fun eggUiItem(
        key: String,
        id: Long?,
        progressPercent: Int,
        nextStateLabel: String?,
        locked: Boolean = false,
        hatched: Boolean = false,
    ) = EggUiItem(
        key = key,
        title = key,
        status = "Endormi",
        requirements = "",
        progress = progressPercent / 100f,
        assetResId = 0,
        locked = locked,
        id = id,
        progressPercent = progressPercent,
        nextStateLabel = nextStateLabel,
        hatched = hatched,
    )

    private fun braiseEgg(nextState: String? = "warm") =
        EggDto(
            id = 1,
            childId = 19,
            eggKey = "oeuf_braise",
            title = "Oeuf braise",
            status = "available",
            state = "sleeping",
            progressPercent = 0,
            nextState = nextState,
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
