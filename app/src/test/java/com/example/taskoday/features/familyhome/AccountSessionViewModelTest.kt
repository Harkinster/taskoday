package com.example.taskoday.features.familyhome

import androidx.lifecycle.SavedStateHandle
import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import com.example.taskoday.features.exploration.ExplorationViewModel
import com.example.taskoday.features.followup.FollowUpViewModel
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionViewModelTest {
    @Test fun `suivi refresh failure clears previous family summaries`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var offline = false
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = if (offline) Result.failure(IllegalStateException("offline")) else base.fetchToday()
            }
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = emptyList<ParentChild>()
            }
            val vm = FollowUpViewModel(auth(identity(100L, "PARENT")), children, family, unused(), unused(), unused(), unused())
            assertFalse(vm.uiState.value.members.isEmpty())
            offline = true
            vm.refresh()
            assertEquals("offline", vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.members.isEmpty())
            assertNull(vm.uiState.value.house)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `suivi does not confuse child profile id with another account user id`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = listOf(ParentChild(101L, "Other child", "other@example.test"))
            }
            val vm = FollowUpViewModel(auth(identity(100L, "PARENT")), children, familyRepository(), unused(), unused(), unused(), unused())
            assertNull(vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.members.all { it.total == 0 })
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `member selection network error clears exploration without crashing`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var offline = false
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = if (offline) Result.failure(IllegalStateException("offline")) else base.fetchToday()
            }
            val vm = exploration(identity(100L, "PARENT"), true, family = family)
            offline = true
            vm.selectMember(101L)
            assertEquals("offline", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.houseTasks.isEmpty())
            assertTrue(vm.uiState.value.routines.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `suivi rejects a child before loading family administration`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = FollowUpViewModel(auth(identity(101L, "CHILD")), unused(), unused(), unused(), unused(), unused(), unused())
            assertNotNull(vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.members.isEmpty())
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `suivi excludes seeds and failed member sync cache`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for ((cachedId, synced, expected) in listOf(Triple(1L, true, 0), Triple(-111L, false, 0), Triple(-111L, true, 1))) {
                val children = object : ChildrenRepository by unused() {
                    override suspend fun fetchChildren() = listOf(ParentChild(201L, "First", "user101@example.test"))
                }
                val tasks = object : TaskRepository by unused() {
                    override fun observeTasksForDay(dayStartMillis: Long) = flowOf(listOf(TaskForDay(Task(id = cachedId, title = "Cached", createdAt = 0L, updatedAt = 0L, isRoutine = true), false)))
                }
                val routines = object : RoutinesRepository by unused() {
                    override suspend fun syncRoutinesForDay(dayStartMillis: Long) = RoutinesSyncResult(synced)
                }
                val missions = object : MissionsRepository by unused() {
                    override suspend fun syncMissions() = MissionsSyncResult(synced)
                }
                val vm = FollowUpViewModel(auth(identity(100L, "PARENT")), children, familyRepository(), tasks, routines, missions, unused())
                assertNull(vm.uiState.value.errorMessage)
                assertEquals(expected, vm.uiState.value.members.first { it.memberId == 101L }.total)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `list applies identity filtering before every user filter`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for ((user, expected) in listOf(
                identity(100L, "PARENT") to listOf("First", "Second", "House"),
                identity(101L, "CHILD") to listOf("First", "House"),
                identity(102L, "CHILD") to listOf("Second", "House"),
            )) {
                val vm = FamilyTaskListViewModel(familyRepository(), auth(user))
                assertEquals(expected.sorted(), vm.uiState.value.visibleTasks.map { it.title })
                assertEquals(user.role == "PARENT", vm.uiState.value.access.canManage)
                vm.selectFilter(FAMILY_TASK_FILTER_ALL)
                assertEquals(expected.sorted(), vm.uiState.value.visibleTasks.map { it.title })
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `child detail cannot expose sibling or initiate delete`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = FamilyTaskDetailViewModel(SavedStateHandle(mapOf("taskId" to 2L)), familyRepository(), auth(identity(101L, "CHILD")))
            assertNull(vm.uiState.value.task)
            vm.requestDelete()
            vm.deleteTask() // Any repository mutation would fail the test via the unused delegate.
            assertFalse(vm.uiState.value.showDeleteConfirmation)
            assertFalse(vm.uiState.value.deleted)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `authenticated exploration excludes seeds and restricts child member selection`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for (user in listOf(identity(100L, "PARENT"), identity(101L, "CHILD"))) {
                val vm = exploration(user, syncSucceeded = true)
                assertNull(vm.uiState.value.errorMessage)
                assertTrue(vm.uiState.value.routines.isEmpty())
                assertTrue(vm.uiState.value.missions.isEmpty())
                assertTrue(vm.uiState.value.objectives.isEmpty())
                if (user.role == "CHILD") {
                    assertEquals(listOf(user.id), vm.uiState.value.members.map { it.id })
                    vm.selectMember(100L)
                    assertEquals(user.id, vm.uiState.value.selectedMemberId)
                }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `failed sync never displays another session remote cache`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val state = exploration(identity(101L, "CHILD"), syncSucceeded = false, cachedId = -111L).uiState.value
            assertTrue(state.routines.isEmpty())
            assertTrue(state.missions.isEmpty())
            assertTrue(state.objectives.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    private fun exploration(user: AuthenticatedUser, syncSucceeded: Boolean, cachedId: Long = 1L, family: FamilyTasksRepository = familyRepository()): ExplorationViewModel {
        val tasks = object : TaskRepository by unused() {
            override fun observeTasksForDay(dayStartMillis: Long) = flowOf(listOf(
                TaskForDay(Task(id = cachedId, title = "Cached example", createdAt = 0L, updatedAt = 0L, isRoutine = true), false)
            ))
        }
        val quests = object : QuestRepository by unused() {
            override fun observeQuestsForDay(dayStartMillis: Long) = flowOf(listOf(
                QuestForDay(Quest(id = if (cachedId < 0L) -133L else 1L, title = "Cached objective", createdAt = 0L, updatedAt = 0L), false)
            ))
        }
        val children = object : ChildrenRepository by unused() {
            override suspend fun fetchChildren() = emptyList<ParentChild>()
        }
        val routines = object : RoutinesRepository by unused() {
            override suspend fun syncRoutinesForDay(dayStartMillis: Long) = RoutinesSyncResult(syncSucceeded)
        }
        val missions = object : MissionsRepository by unused() {
            override suspend fun syncMissions() = MissionsSyncResult(syncSucceeded)
        }
        val remoteQuests = object : QuestsRepository by unused() {
            override suspend fun syncQuests() = QuestsSyncResult(syncSucceeded)
        }
        return ExplorationViewModel(auth(user), children, family, tasks, routines, missions, quests, remoteQuests, unused())
    }

    private fun identity(id: Long, role: String) = AuthenticatedUser(id, "user$id@example.test", role, true, listOf(1L), "User $id")
    private fun auth(user: AuthenticatedUser) = object : AuthRepository by unused() {
        override suspend fun fetchMe() = user
        override suspend fun getActiveChildId(forceRefresh: Boolean) = if (user.role == "CHILD") 201L else null
        override fun setActiveChildId(childId: Long) = Unit
    }

    private fun familyRepository() = object : FamilyTasksRepository by unused() {
        private val tasks = listOf(definition(1L, "First", 101L), definition(2L, "Second", 102L), definition(3L, "House", null))
        override suspend fun fetchTasks() = Result.success(tasks)
        override suspend fun fetchTask(taskId: Long) = Result.success(tasks.first { it.id == taskId })
        override suspend fun fetchMembers() = Result.success(listOf(
            FamilyTaskMember(100L, "Parent", "user100@example.test", FamilyTaskMemberRole.PARENT, true),
            FamilyTaskMember(101L, "First", "user101@example.test", FamilyTaskMemberRole.CHILD, true),
            FamilyTaskMember(102L, "Second", "user102@example.test", FamilyTaskMemberRole.CHILD, true),
        ))
        override suspend fun fetchToday() = Result.success(FamilyTasksToday(1L, "2026-09-27", emptyList()))
        override suspend fun fetchOverdueOccurrences() = Result.success(FamilyTaskOccurrencesRange(1L, "2026-09-26", "2026-09-27", emptyList()))
    }

    private fun definition(id: Long, title: String, assignee: Long?) = FamilyTaskDefinition(
        id, 1L, title, null, assignee?.let { listOf(FamilyTaskAssignee(it, title)) }.orEmpty(),
        null, null, false, null, FamilyTaskRecurrence.NONE, emptyList(), false, false, FamilyTaskPriority.NORMAL, true,
    )

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected call: ${method.name}")
    } as T
}
