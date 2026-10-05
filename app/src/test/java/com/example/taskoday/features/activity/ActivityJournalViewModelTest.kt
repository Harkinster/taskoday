package com.example.taskoday.features.activity

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.model.FamilyTaskEventType
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTaskEventsRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityJournalViewModelTest {
    @Test fun `parent loads family events filters and refreshes`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var rows = listOf(event(1, 7, 27), event(2, 7, 28), event(3, 8, 27))
            var loads = 0
            val source = object : FamilyTaskEventsRepository {
                override suspend fun fetchSince(startAt: Instant): Result<List<FamilyTaskEvent>> {
                    loads++
                    return Result.success(rows)
                }
            }
            val family = object : FamilyTasksRepository by unused() {
                override suspend fun fetchMembers() = Result.success(listOf(
                    FamilyTaskMember(27, "Naomy", null, FamilyTaskMemberRole.CHILD, true),
                    FamilyTaskMember(28, "Autre enfant", null, FamilyTaskMemberRole.CHILD, true),
                ))
            }
            val vm = ActivityJournalViewModel(auth(25, "PARENT"), source, family)
            assertEquals(2, vm.uiState.value.events.size)
            vm.selectMember(27)
            assertEquals(listOf(1L), vm.uiState.value.visibleEvents.map { it.id })
            vm.selectType(FamilyActionType.HOUSE_QUEST)
            assertTrue(vm.uiState.value.visibleEvents.isEmpty())
            rows = rows + event(4, 7, 27)
            vm.refresh()
            assertEquals(2, loads)
            assertEquals(27L, vm.uiState.value.selectedMemberId)
            assertEquals(3, vm.uiState.value.events.size)
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `child receives only server supplied events and has no member filter`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val source = object : FamilyTaskEventsRepository {
                override suspend fun fetchSince(startAt: Instant) = Result.success(listOf(event(1, 7, 27)))
            }
            val vm = ActivityJournalViewModel(auth(27, "CHILD"), source, unused())
            assertEquals(listOf(1L), vm.uiState.value.events.map { it.id })
            assertTrue(vm.uiState.value.members.isEmpty())
            vm.selectMember(28)
            assertNull(vm.uiState.value.selectedMemberId)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `empty and network error are distinct states`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var fail = false
            val source = object : FamilyTaskEventsRepository {
                override suspend fun fetchSince(startAt: Instant): Result<List<FamilyTaskEvent>> =
                    if (fail) Result.failure(java.net.UnknownHostException("internal.example")) else Result.success(emptyList())
            }
            val vm = ActivityJournalViewModel(auth(27, "CHILD"), source, unused())
            assertTrue(vm.uiState.value.events.isEmpty())
            assertNull(vm.uiState.value.errorMessage)
            fail = true
            vm.refresh()
            assertTrue(vm.uiState.value.events.isEmpty())
            assertEquals("Serveur indisponible.", vm.uiState.value.errorMessage)
        } finally { Dispatchers.resetMain() }
    }

    private fun auth(id: Long, role: String) = object : AuthRepository by unused() {
        override suspend fun fetchMe() = AuthenticatedUser(id, "qa@example.test", role, true, listOf(7), "QA")
        override suspend fun getActiveFamilyId(forceRefresh: Boolean): Long = 7
    }

    private fun event(id: Long, family: Long, actor: Long) = FamilyTaskEvent(
        id, family, id, id, FamilyActionType.PERSONAL_MISSION, "QA", FamilyTaskEventType.COMPLETE,
        FamilyTaskStatus.TODO, FamilyTaskStatus.COMPLETED, actor, "QA", actor, listOf(actor), Instant.parse("2026-10-05T12:00:00Z"), false,
    )

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected call: ${method.name}")
    } as T
}
