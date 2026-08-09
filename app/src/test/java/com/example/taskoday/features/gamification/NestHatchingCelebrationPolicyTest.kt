package com.example.taskoday.features.gamification

import com.example.taskoday.data.remote.dto.DragonDto
import com.example.taskoday.data.remote.dto.DragonsDto
import com.example.taskoday.data.remote.dto.EggDto
import com.example.taskoday.data.remote.dto.EggEvolutionDto
import com.example.taskoday.data.remote.dto.InventoryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NestHatchingCelebrationPolicyTest {
    @Test
    fun `successful non final evolution does not create dragon celebration`() {
        val celebration =
            hatchingCelebrationFromEvolution(
                eventId = 1L,
                result = eggEvolution(hatched = false, dragon = dragon(id = 7L)),
                refreshedDragons = dragons(dragon(id = 7L)),
            )

        assertNull(celebration)
    }

    @Test
    fun `successful hatching creates dragon celebration`() {
        val celebration =
            hatchingCelebrationFromEvolution(
                eventId = 2L,
                result = eggEvolution(hatched = true, dragon = dragon(id = 7L, title = "Braise")),
                refreshedDragons = null,
            )

        assertNotNull(celebration)
        assertEquals(7L, celebration?.dragonId)
        assertEquals("Braise", celebration?.dragonTitle)
    }

    @Test
    fun `server failure creates no hatching celebration`() {
        val celebration =
            hatchingCelebrationFromEvolution(
                eventId = 3L,
                result = null,
                refreshedDragons = null,
            )

        assertNull(celebration)
    }

    @Test
    fun `dragon found after refresh uses refreshed data`() {
        val celebration =
            hatchingCelebrationFromEvolution(
                eventId = 4L,
                result = eggEvolution(hatched = true, dragon = dragon(id = 7L, title = "Ancien nom")),
                refreshedDragons = dragons(dragon(id = 7L, title = "Braise du Nid", stage = "young")),
            )

        assertEquals("Braise du Nid", celebration?.dragonTitle)
        assertEquals("young", celebration?.dragonStage)
        assertTrue(celebration?.hasDragonDetails == true)
    }

    @Test
    fun `dragon not found after refresh falls back to generic celebration`() {
        val celebration =
            hatchingCelebrationFromEvolution(
                eventId = 5L,
                result = eggEvolution(hatched = true, dragon = dragon(id = 7L, title = "Braise")),
                refreshedDragons = dragons(dragon(id = 99L, title = "Autre dragon", key = "dragon_lunaire")),
            )

        assertNotNull(celebration)
        assertFalse(celebration?.hasDragonDetails == true)
        assertNull(celebration?.dragonTitle)
        assertNull(celebration?.dragonId)
    }

    @Test
    fun `closing hatching celebration consumes state`() {
        val state =
            NestUiState(
                hatchingCelebration =
                    NestHatchingCelebration(
                        eventId = 6L,
                        dragonId = 7L,
                        dragonKey = "dragon_braise",
                        dragonTitle = "Braise",
                        dragonStage = "baby",
                    ),
            )

        assertNull(consumeHatchingCelebrationState(state).hatchingCelebration)
    }

    private fun eggEvolution(
        hatched: Boolean,
        dragon: DragonDto?,
    ) = EggEvolutionDto(
        egg = egg(),
        hatched = hatched,
        dragon = dragon,
        inventory = inventory(),
    )

    private fun dragons(vararg dragons: DragonDto) =
        DragonsDto(
            childId = 19L,
            dragons = dragons.toList(),
            activeCompanion = dragons.firstOrNull { it.activeCompanion },
        )

    private fun dragon(
        id: Long,
        title: String = "Braise",
        stage: String = "baby",
        key: String = "dragon_braise",
    ) = DragonDto(
        id = id,
        childId = 19L,
        dragonKey = key,
        title = title,
        stage = stage,
        progressPercent = 0,
        activeCompanion = false,
        assetKey = "${key}_$stage",
    )

    private fun egg() =
        EggDto(
            id = 1L,
            childId = 19L,
            eggKey = "oeuf_braise",
            title = "Oeuf Braise",
            status = "hatched",
            state = "hatching",
            progressPercent = 100,
            nextState = null,
            assetKey = "oeuf_braise_hatching",
            requirements = emptyMap(),
        )

    private fun inventory() =
        InventoryDto(
            childId = 19L,
            currencies = emptyMap(),
            items = emptyList(),
            chests = emptyList(),
        )
}
