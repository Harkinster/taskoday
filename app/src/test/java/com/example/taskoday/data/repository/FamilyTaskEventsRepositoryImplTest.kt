package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyTaskEventsResponseDto
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTaskEventsRepositoryImplTest {
    private val gson = Gson()

    @Test fun `API event maps snapshot category actor and timestamp`() {
        val item = json(7, "TASKODAY_PERSONAL_MISSION", "2026-10-05T15:42:00", actor = false)
            .toFamilyTaskEventsResponseDto(gson).items.single().toDomain()
        assertEquals(FamilyActionType.PERSONAL_MISSION, item.category)
        assertEquals("Membre de la famille", item.actorName)
        assertEquals(Instant.parse("2026-10-05T15:42:00Z"), item.occurredAt)
        assertEquals(listOf(27L), item.participantUserIds)
    }

    @Test fun `repository uses active family once and excludes another family`() = runTest {
        var calls = 0
        val api = proxy<FamilyTasksApi> { name, args ->
            if (name != "getTaskEvents") error("Unexpected $name")
            calls++
            assertEquals(7L, args[0])
            assertEquals(100, args[2])
            assertEquals(0, args[3])
            ApiEnvelopeDto(true, json(7, "TASKODAY_HOUSE_QUEST", "2026-10-05T15:42:00Z"))
        }
        val auth = object : AuthRepository by proxy({ name, _ -> error("Unexpected $name") }) {
            override suspend fun getActiveFamilyId(forceRefresh: Boolean): Long = 7L
        }
        val rows = FamilyTaskEventsRepositoryImpl(auth, api, gson).fetchSince(Instant.parse("2026-09-29T00:00:00Z")).getOrThrow()
        assertEquals(1, calls)
        assertEquals(1, rows.size)
        assertEquals(7L, rows.single().familyId)
        assertEquals(FamilyActionType.HOUSE_QUEST, rows.single().category)
    }

    @Test fun `repository rejects response for another family`() = runTest {
        val api = proxy<FamilyTasksApi> { _, _ -> ApiEnvelopeDto(true, json(8, "TASKODAY_HOUSE_QUEST", "2026-10-05T15:42:00Z")) }
        val auth = object : AuthRepository by proxy({ name, _ -> error("Unexpected $name") }) {
            override suspend fun getActiveFamilyId(forceRefresh: Boolean): Long = 7L
        }
        assertTrue(FamilyTaskEventsRepositoryImpl(auth, api, gson).fetchSince(Instant.EPOCH).isFailure)
    }

    private fun json(family: Int, category: String, time: String, actor: Boolean = true) = JsonParser.parseString("""
        {"family_id":$family,"items":[{"id":1,"family_id":$family,"task_id":9,"occurrence_id":11,
        "category":"$category","title":"Faire les devoirs","event_type":"COMPLETE",
        "status_from":"TODO","status_to":"PENDING_VALIDATION","actor_user_id":27,
        "actor_user":${if (actor) "{\"user_id\":27,\"display_name\":\"Naomy\"}" else "null"},
        "completed_by_user_id":27,"participant_user_ids":[27],"occurred_at":"$time","legacy_inferred":false}]}
    """.trimIndent())

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> proxy(crossinline response: (String, Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            response(method.name, args ?: emptyArray())
        } as T
}
