package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
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
}
