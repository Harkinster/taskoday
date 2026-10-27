package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** LOCAL_SIMULATION_PASS: account ownership checked with in-memory API responses only. */
class NestOwnershipTest {
    private var userId = 100L
    private var familyId = 1L
    private var rewardUserId = 100L
    private var rewardFamilyId = 1L
    private var companionOwner = 100L
    private var active = true
    private val companion get() = PersonalCompanionDto(7L, companionOwner, "dragon", lineageStatus = "CHOSEN", stage = "BABY", progress = 0, active = active)
    private val auth = object : AuthRepository by unused() {
        override suspend fun fetchMe() = AuthenticatedUser(userId, "local@example.test", "PARENT", true, listOf(familyId))
        override suspend fun getActiveFamilyId(forceRefresh: Boolean) = familyId
    }
    private val api = object : NestApi by unused() {
        override suspend fun getPersonalRewards(familyId: Long) = ApiEnvelopeDto(true, PersonalRewardSummaryDto(rewardFamilyId, rewardUserId, 4, 5, 6))
        override suspend fun getMyCompanions() = ApiEnvelopeDto(true, MyCompanionsDto(listOf(companion), activeState = "ACTIVE"))
        override suspend fun getMyActiveCompanion() = ApiEnvelopeDto(true, if (active) companion else null)
    }

    @Test fun `personal rewards reject another user within the same family`() = runTest {
        rewardUserId = 101L
        assertTrue(NestRepository(api, auth).loadPersonalRewards().isFailure)
    }

    @Test fun `personal rewards reject another family`() = runTest {
        rewardFamilyId = 2L
        assertTrue(NestRepository(api, auth).loadPersonalRewards().isFailure)
    }

    @Test fun `switching accounts cannot reuse previous personal rewards`() = runTest {
        val repository = NestRepository(api, auth)
        assertEquals(100L, repository.loadPersonalRewards().getOrThrow().userId)
        userId = 101L
        assertTrue(repository.loadPersonalRewards().isFailure)
        rewardUserId = 101L
        assertEquals(101L, repository.loadPersonalRewards().getOrThrow().userId)
    }

    @Test fun `companion of another user is rejected`() = runTest {
        companionOwner = 101L
        assertTrue(NestRepository(api, auth).loadPersonalCompanions().isFailure)
    }

    @Test fun `companion ownership is checked again after a profile switch`() = runTest {
        val repository = NestRepository(api, auth)
        assertEquals(100L, repository.loadPersonalCompanions().getOrThrow().activeCompanion!!.ownerUserId)
        userId = 101L
        assertTrue(repository.loadPersonalCompanions().isFailure)
        companionOwner = 101L
        assertEquals(101L, repository.loadPersonalCompanions().getOrThrow().activeCompanion!!.ownerUserId)
    }

    @Test fun `no active companion remains a valid explicit state`() = runTest {
        active = false
        val result = NestRepository(api, auth).loadPersonalCompanions().getOrThrow()
        assertNull(result.activeCompanion)
        assertFalse(result.companions.single().active)
    }
}

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected API call: ${method.name}")
} as T
