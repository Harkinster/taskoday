package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
