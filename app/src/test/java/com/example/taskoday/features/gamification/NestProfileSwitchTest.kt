package com.example.taskoday.features.gamification

import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

/** LOCAL_SIMULATION_PASS; delayed responses come from local API doubles only. */
@OptIn(ExperimentalCoroutinesApi::class)
class NestProfileSwitchTest {
    private var userId = 100L
    private var loggedIn = true
    private var starterGate: CompletableDeferred<Unit>? = null
    private val auth = object : AuthRepository by unused() {
        override fun getAccessToken() = if (loggedIn) "local-token-not-a-credential" else null
        override suspend fun fetchMe() = AuthenticatedUser(userId, "local@example.test", if (userId == 100L) "PARENT" else "CHILD", true, listOf(1L))
        override suspend fun getActiveFamilyId(forceRefresh: Boolean) = 1L
        override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null
    }
    private fun companion(ownerId: Long) = PersonalCompanionDto(ownerId, ownerId, "dragon", lineageStatus = "CHOSEN", stage = "BABY", progress = 0, active = true)
    private val api = object : NestApi by unused() {
        override suspend fun getPersonalRewards(familyId: Long) = ApiEnvelopeDto(true, PersonalRewardSummaryDto(1L, userId, 3, 4, 5))
        override suspend fun getMyCompanions() = ApiEnvelopeDto(true, MyCompanionsDto(listOf(companion(userId)), activeState = "ACTIVE"))
        override suspend fun getMyActiveCompanion() = ApiEnvelopeDto<PersonalCompanionDto?>(true, companion(userId))
        override suspend fun createStarter(request: StarterCompanionRequestDto): ApiEnvelopeDto<StarterCompanionDto> {
            val owner = userId
            starterGate?.await()
            return ApiEnvelopeDto(true, StarterCompanionDto(companion(owner), true, false))
        }
    }

    @Test fun `parent child switch replaces companions and personal resources`() = scenario {
        val vm = NestViewModel(NestRepository(api, auth))
        runCurrent()
        assertEquals(100L, vm.uiState.value.personalCompanions!!.activeCompanion!!.ownerUserId)
        userId = 101L
        vm.refresh()
        assertNull(vm.uiState.value.personalCompanions)
        assertNull(vm.uiState.value.personalRewards)
        runCurrent()
        assertEquals(101L, vm.uiState.value.accountUserId)
        assertEquals(101L, vm.uiState.value.personalCompanions!!.activeCompanion!!.ownerUserId)
        assertEquals(101L, vm.uiState.value.personalRewards!!.userId)
        loggedIn = false
        vm.refresh()
        assertFalse(vm.uiState.value.hasRemoteSession)
        assertNull(vm.uiState.value.personalCompanions)
        assertNull(vm.uiState.value.personalRewards)
    }

    @Test fun `late parent starter response does not announce success in child profile`() = scenario {
        val vm = NestViewModel(NestRepository(api, auth))
        runCurrent()
        starterGate = CompletableDeferred()
        vm.createStarter("lineage", "Parent dragon")
        runCurrent()
        userId = 101L
        vm.refresh()
        runCurrent()
        starterGate!!.complete(Unit)
        runCurrent()
        assertEquals(101L, vm.uiState.value.accountUserId)
        assertFalse(vm.uiState.value.starterCreated)
        assertEquals(101L, vm.uiState.value.personalCompanions!!.activeCompanion!!.ownerUserId)
    }

    @Test fun `late response after logout cannot restore companion success state`() = scenario {
        val vm = NestViewModel(NestRepository(api, auth))
        runCurrent()
        starterGate = CompletableDeferred()
        vm.createStarter("lineage", "Parent dragon")
        runCurrent()
        loggedIn = false
        vm.refresh()
        starterGate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.hasRemoteSession)
        assertNull(vm.uiState.value.accountUserId)
        assertFalse(vm.uiState.value.starterCreated)
        assertNull(vm.uiState.value.personalCompanions)
    }

    private fun scenario(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
}

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected API call: ${method.name}")
} as T
