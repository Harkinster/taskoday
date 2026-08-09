package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NestDtosTest {
    private val gson = Gson()

    @Test
    fun lootQuantityRemainsTheChestGain() {
        val loot =
            gson.fromJson(
                """
                {
                  "key": "pomme_dragon",
                  "title": "Pomme dragon",
                  "name": "Pomme dragon",
                  "rarity": "common",
                  "category": "material",
                  "quantity": 8,
                  "quantity_total": 16,
                  "is_duplicate_compensation": false
                }
                """.trimIndent(),
                LootGainDto::class.java,
            )

        assertEquals(8, loot.quantity)
        assertEquals(16, loot.quantityTotal)
        assertFalse(loot.isDuplicateCompensation)
    }

    @Test
    fun bestiaryFamilyReadsCurrentStatesAndUnlocks() {
        val family =
            gson.fromJson(
                """
                {
                  "family_id": "braise",
                  "family_name": "Braise",
                  "element": "fire",
                  "discovered": true,
                  "egg_owned": true,
                  "dragon_owned": true,
                  "current_egg_state": "hatching",
                  "current_dragon_stage": "baby",
                  "active_companion": true,
                  "legendary_unlocked": false,
                  "progress_percent": 60,
                  "egg_asset_key": "egg_braise_hatching",
                  "dragon_asset_key": "dragon_braise_baby",
                  "egg_states": [{"state": "sleeping", "unlocked": true}],
                  "dragon_stages": [{"state": "baby", "unlocked": true}],
                  "legendary_artifact": {"item_key": "rune_ancienne", "required": 1, "owned": 0}
                }
                """.trimIndent(),
                BestiaryFamilyDto::class.java,
            )

        assertEquals("hatching", family.currentEggState)
        assertEquals("baby", family.currentDragonStage)
        assertTrue(family.activeCompanion)
        assertTrue(family.eggStates.first().unlocked)
    }

    @Test
    fun dragonReadsTypedEvolutionContract() {
        val dragon =
            gson.fromJson(
                """
                {
                  "id": 7,
                  "child_id": 19,
                  "dragon_key": "dragon_braise",
                  "title": "Dragon de Braise",
                  "stage": "baby",
                  "current_stage": "baby",
                  "progress_percent": 40,
                  "next_stage": "young",
                  "requirements": {"pomme_dragon": 5},
                  "required_resources": [
                    {
                      "item_key": "pomme_dragon",
                      "title": "Pomme dragon",
                      "owned_quantity": 29,
                      "required_quantity": 5,
                      "is_satisfied": true
                    },
                    {
                      "item_key": "petit_cristal",
                      "title": "Petit cristal",
                      "owned_quantity": 2,
                      "required_quantity": 4,
                      "is_satisfied": false
                    }
                  ],
                  "can_evolve": false,
                  "active_companion": false,
                  "asset_key": "dragon_braise_baby",
                  "next_evolution": {"legacy": true}
                }
                """.trimIndent(),
                DragonDto::class.java,
            )

        assertEquals("baby", dragon.currentStage)
        assertEquals("young", dragon.nextStage)
        assertEquals(5, dragon.requirements?.get("pomme_dragon"))
        assertFalse(dragon.canEvolve)
        assertEquals(2, dragon.requiredResources?.size)
        assertEquals("petit_cristal", dragon.requiredResources?.last()?.itemKey)
        assertFalse(dragon.requiredResources?.last()?.isSatisfied ?: true)
        assertEquals(true, dragon.nextEvolution?.get("legacy"))
    }

    @Test
    fun bestiaryFamilyReadsTypedDragonEvolutionContract() {
        val family =
            gson.fromJson(
                """
                {
                  "family_id": "braise",
                  "family_name": "Braise",
                  "element": "fire",
                  "discovered": true,
                  "egg_owned": true,
                  "dragon_owned": true,
                  "current_egg_state": "hatching",
                  "current_dragon_stage": "baby",
                  "active_companion": true,
                  "legendary_unlocked": false,
                  "progress_percent": 60,
                  "dragon": {
                    "id": 7,
                    "child_id": 19,
                    "dragon_key": "dragon_braise",
                    "title": "Dragon de Braise",
                    "stage": "baby",
                    "current_stage": "baby",
                    "progress_percent": 40,
                    "next_stage": null,
                    "required_resources": [],
                    "can_evolve": true,
                    "active_companion": true,
                    "asset_key": "dragon_braise_baby"
                  },
                  "next_dragon_stage": null,
                  "dragon_required_resources": [
                    {
                      "item_key": "rune_ancienne",
                      "title": "Rune ancienne",
                      "owned_quantity": 0,
                      "required_quantity": 2,
                      "is_satisfied": false
                    }
                  ],
                  "dragon_can_evolve": false,
                  "egg_asset_key": "egg_braise_hatching",
                  "dragon_asset_key": "dragon_braise_baby",
                  "egg_states": [{"state": "sleeping", "unlocked": true}],
                  "dragon_stages": [{"state": "baby", "unlocked": true}],
                  "legendary_artifact": {"item_key": "rune_ancienne", "required": 1, "owned": 0}
                }
                """.trimIndent(),
                BestiaryFamilyDto::class.java,
            )

        assertEquals("Dragon de Braise", family.dragon?.title)
        assertNull(family.nextDragonStage)
        assertEquals(1, family.dragonRequiredResources?.size)
        assertEquals("rune_ancienne", family.dragonRequiredResources?.single()?.itemKey)
        assertFalse(family.dragonCanEvolve ?: true)
    }
}
