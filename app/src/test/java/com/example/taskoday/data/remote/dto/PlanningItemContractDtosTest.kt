package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlanningItemContractDtosTest {
    private val gson = Gson()

    @Test
    fun missionWithoutIsActiveIsNotTreatedAsInactive() {
        val mission =
            gson.fromJson(
                """{"id":10,"title":"Mission test","status":"open"}""",
                MissionItemDto::class.java,
            )

        assertNull(mission.isActive)
        assertFalse(mission.isActive == false)
    }

    @Test
    fun questWithoutIsActiveIsNotTreatedAsInactive() {
        val quest =
            gson.fromJson(
                """{"id":11,"title":"Quete test","status":"open"}""",
                QuestItemDto::class.java,
            )

        assertNull(quest.isActive)
        assertFalse(quest.isActive == false)
    }

    @Test
    fun completionReadsAwardFromBackendContract() {
        val completion =
            gson.fromJson(
                """
                {
                  "routine_id": 42,
                  "completed": true,
                  "award": {
                    "guardian_xp_awarded": 5,
                    "flammeches_awarded": 2,
                    "crystals_awarded": 1
                  }
                }
                """.trimIndent(),
                RemoteCompletionResponseDto::class.java,
            )

        assertEquals(5, completion.award?.guardianXpAwarded)
        assertEquals(2, completion.award?.flammechesAwarded)
        assertEquals(1, completion.award?.crystalsAwarded)
    }
}
