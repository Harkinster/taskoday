package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** LOCAL_SIMULATION_PASS: real repository mapping with a fake API; no HTTP is sent. */
class FamilyTaskRepositoryBoundaryTest {
    private var error: Throwable? = null
    private var success = true
    private var receivedFamily: Long? = null
    private var receivedPayload: FamilyTaskCreateRequestDto? = null
    private val auth = object : AuthRepository by unused() {
        override suspend fun getActiveFamilyId(forceRefresh: Boolean) = 9L
    }
    private fun response(): ApiEnvelopeDto<JsonElement> {
        error?.let { throw it }
        return ApiEnvelopeDto(success, JsonParser.parseString("{}"))
    }
    private val api = object : FamilyTasksApi by unused() {
        override suspend fun createTask(familyId: Long, payload: FamilyTaskCreateRequestDto): ApiEnvelopeDto<JsonElement> {
            receivedFamily = familyId
            receivedPayload = payload
            return response()
        }
        override suspend fun completeOccurrence(occurrenceId: Long) = response()
        override suspend fun validateOccurrence(occurrenceId: Long) = response()
        override suspend fun startOccurrence(occurrenceId: Long) = response()
    }
    private val repository = FamilyTasksRepositoryImpl(auth, api, Gson())
    private val input = FamilyTaskCreateInput("Préparer le sac", null, "2026-10-20", null, FamilyTaskRecurrence.NONE, emptyList(), listOf(101L), true, false, FamilyTaskPriority.NORMAL, category = FamilyActionType.PERSONAL_MISSION.category)

    @Test fun `create maps active family and account assignee without confusing child profile id`() = runTest {
        assertTrue(repository.createTask(input).isSuccess)
        assertEquals(9L, receivedFamily)
        val json = Gson().toJsonTree(receivedPayload).asJsonObject
        assertEquals(101L, json.getAsJsonArray("assignee_user_ids").single().asLong)
        assertEquals("PERSONAL", json.get("scope").asString)
        assertEquals("MISSION", json.get("kind").asString)
    }

    @Test fun `HTTP errors remain failures for create completion and validation`() = runTest {
        for (status in listOf(401, 403, 422, 500)) {
            error = HttpException(Response.error<Any>(status, "{}".toResponseBody()))
            assertTrue(repository.createTask(input).isFailure)
            assertTrue(repository.completeOccurrenceWithBundle(11L).isFailure)
            assertTrue(repository.validateOccurrenceWithBundle(11L).isFailure)
        }
    }

    @Test fun `network failure never becomes a successful creation`() = runTest {
        error = java.net.SocketTimeoutException()
        assertTrue(repository.createTask(input).isFailure)
    }

    @Test fun `negative application acknowledgement is not a successful creation`() = runTest {
        success = false
        assertTrue(repository.createTask(input).isFailure)
    }

    @Test fun `negative application acknowledgement is not successful completion validation or start`() = runTest {
        success = false
        assertTrue(repository.completeOccurrenceWithBundle(11L).isFailure)
        assertTrue(repository.validateOccurrenceWithBundle(11L).isFailure)
        assertTrue(repository.startOccurrence(11L).isFailure)
    }
}

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected API call: ${method.name}")
} as T
