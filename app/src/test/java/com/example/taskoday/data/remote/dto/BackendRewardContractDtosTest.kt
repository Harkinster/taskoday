package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendRewardContractDtosTest {
    private val gson = Gson()

    @Test
    fun `routine reads completed field from real backend contract`() {
        val routine =
            gson.fromJson(
                """
                {
                  "id": 5,
                  "child_id": 19,
                  "title": "Audit reward routine",
                  "is_active": true,
                  "completed": true
                }
                """.trimIndent(),
                RoutineItemDto::class.java,
            )

        assertTrue(routine.completed)
    }

    @Test
    fun `stats read xp and nested counters from real backend contract`() {
        val stats =
            gson.fromJson(
                """
                {
                  "child_id": 19,
                  "xp": 5,
                  "level": 1,
                  "routines": {"total": 1, "completed": 1},
                  "missions": {"total": 2, "completed": 1},
                  "quests": {"total": 3, "completed": 2},
                  "xp_history_events": 1
                }
                """.trimIndent(),
                ChildStatsDto::class.java,
            )

        assertEquals(5, stats.totalXp)
        assertEquals(1, stats.routines?.completed)
        assertEquals(2, stats.missions?.total)
        assertEquals(2, stats.quests?.completed)
    }

    @Test
    fun `wish update request uses backend snake case contract`() {
        val json =
            gson.toJson(
                RewardUpdateRequestDto(
                    title = "Sortie cinema",
                    description = "Samedi apres-midi",
                    costScales = 12,
                    isActive = false,
                ),
            )

        assertTrue(json.contains("\"cost_scales\":12"))
        assertTrue(json.contains("\"is_active\":false"))
        assertTrue(json.contains("\"title\":\"Sortie cinema\""))
    }
}
