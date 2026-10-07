package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceUtilityDtoTest {
    @Test fun `family utility contracts parse persisted policy and request identity`() {
        val gson = Gson()
        val catalog = gson.fromJson(
            """{"family_id":11,"user_id":31,"crystals":8,"chests":[{"chest_type":"COMMON","title":"Coffre commun","crystal_cost":3,"drop_count":1}]}""",
            ResourceChestCatalogDto::class.java,
        )
        assertEquals(11L, catalog.familyId)
        assertEquals(8, catalog.crystals)
        assertEquals("COMMON", catalog.chests.single().chestType)
        assertEquals(3, catalog.chests.single().crystalCost)

        val request = gson.fromJson(
            """{"id":4,"offer_id":2,"user_id":31,"requester_name":"Reward Enfant","title":"Choisir le film","flame_cost":5,"status":"PENDING","requested_at":"2026-10-07T10:00:00Z"}""",
            WishRequestDto::class.java,
        )
        assertEquals("Reward Enfant", request.requesterName)
        assertEquals("PENDING", request.status)
        assertEquals(5, request.flameCost)
    }
}
