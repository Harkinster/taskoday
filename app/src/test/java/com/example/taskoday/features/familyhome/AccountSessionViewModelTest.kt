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
    @Test fun `exploration refresh uses readable network error instead of DNS exception`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val family = object : FamilyTasksRepository by familyRepository() {
                override suspend fun fetchMembers() = Result.failure<List<FamilyTaskMember>>(
                    java.net.UnknownHostException("Unable to resolve host internal.example"),
                )
            }
            val state = exploration(identity(100L, "PARENT"), true, family = family).uiState.value
            assertEquals("Serveur indisponible.", state.errorMessage)
            assertFalse(state.isLoading)
            assertTrue(state.houseTasks.isEmpty())
            assertTrue(state.routines.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `exploration member timeout is readable and clears stale data`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var timedOut = false
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = if (timedOut) {
                    Result.failure(java.net.SocketTimeoutException("technical timeout"))
                } else base.fetchToday()
            }
            val vm = exploration(identity(100L, "PARENT"), true, family = family)
            timedOut = true
            vm.refresh()
            assertEquals("Requête expirée.", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.houseTasks.isEmpty())
            assertTrue(vm.uiState.value.routines.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

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

    @Test fun `suivi separates personal actions from assigned house quests`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = Result.success(FamilyTasksToday(1L, "2026-10-04", listOf(
                    occurrence(1L, "Quest", 102L, null),
                    occurrence(2L, "Mission", 100L, FamilyActionType.PERSONAL_MISSION.category),
                    occurrence(3L, "Routine", 101L, FamilyActionType.PERSONAL_ROUTINE.category),
                )))
            }
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = emptyList<ParentChild>()
            }
            val state = FollowUpViewModel(auth(identity(100L, "PARENT")), children, family, unused(), unused(), unused(), unused()).uiState.value
            assertEquals(listOf("Quest"), state.house?.items?.map { it.title })
            assertEquals(listOf("Mission"), state.members.first { it.memberId == 100L }.items.map { it.title })
            assertEquals(listOf("Routine"), state.members.first { it.memberId == 101L }.items.map { it.title })
            assertTrue(state.members.first { it.memberId == 102L }.items.isEmpty())
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
            vm.refresh()
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
                identity(101L, "CHILD") to listOf("First", "Second", "House"),
                identity(102L, "CHILD") to listOf("First", "Second", "House"),
            )) {
                val vm = FamilyTaskListViewModel(familyRepository(), auth(user))
                assertEquals(expected.sorted(), vm.uiState.value.visibleTasks.map { it.title })
                assertEquals(user.role == "PARENT", vm.uiState.value.access.canManage)
                vm.selectFilter(FAMILY_TASK_FILTER_ALL)
                assertEquals(expected.sorted(), vm.uiState.value.visibleTasks.map { it.title })
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `child can read collective quest assigned to sibling but cannot delete`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = FamilyTaskDetailViewModel(SavedStateHandle(mapOf("taskId" to 2L)), familyRepository(), auth(identity(101L, "CHILD")))
            assertNotNull(vm.uiState.value.task)
            vm.requestDelete()
            vm.deleteTask() // Any repository mutation would fail the test via the unused delegate.
            assertFalse(vm.uiState.value.showDeleteConfirmation)
            assertFalse(vm.uiState.value.deleted)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `detail validation action follows status and existing access policy`() {
        val occurrence = occurrence(2L, "Mission", 102L, FamilyActionType.PERSONAL_MISSION.category)
        val parent = FamilyTaskAccessPolicy(100L, "PARENT")
        val child = FamilyTaskAccessPolicy(102L, "CHILD")
        assertFalse(FamilyTaskDetailUiState(access = parent, todayOccurrence = occurrence).canValidateToday)
        val pending = occurrence.copy(status = FamilyTaskStatus.PENDING_VALIDATION)
        assertTrue(FamilyTaskDetailUiState(access = parent, todayOccurrence = pending).canValidateToday)
        assertFalse(FamilyTaskDetailUiState(access = child, todayOccurrence = pending).canValidateToday)
        assertFalse(FamilyTaskDetailUiState(access = parent, todayOccurrence = pending.copy(occurrenceId = 0L)).canValidateToday)
        assertFalse(FamilyTaskDetailUiState(access = parent, todayOccurrence = pending.copy(status = FamilyTaskStatus.COMPLETED)).canValidateToday)
        assertFalse(FamilyTaskDetailUiState(access = parent, todayOccurrence = pending.copy(status = FamilyTaskStatus.VALIDATED)).canValidateToday)
        assertEquals("En attente de validation", familyTaskStatusLabel(pending.status))
        assertEquals("Terminée", familyTaskStatusLabel(FamilyTaskStatus.COMPLETED))
        assertEquals("Validée", familyTaskStatusLabel(FamilyTaskStatus.VALIDATED))
    }

    @Test fun `detail validates through existing repository and preserves suivi selection`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val pending = occurrence(2L, "Mission", 102L, FamilyActionType.PERSONAL_MISSION.category)
                .copy(status = FamilyTaskStatus.PENDING_VALIDATION)
            var current = pending
            var validationCalls = 0
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = Result.success(FamilyTasksToday(1L, "2026-10-05", listOf(current)))
                override suspend fun validateOccurrence(occurrenceId: Long): Result<Unit> {
                    assertEquals(pending.occurrenceId, occurrenceId)
                    validationCalls++
                    current = current.copy(status = FamilyTaskStatus.VALIDATED)
                    return Result.success(Unit)
                }
                override suspend fun validateOccurrenceWithReward(occurrenceId: Long): Result<Int> =
                    validateOccurrence(occurrenceId).map { 0 }
            }
            val parent = auth(identity(100L, "PARENT"))
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = emptyList<ParentChild>()
            }
            val followUp = FollowUpViewModel(parent, children, family, unused(), unused(), unused(), unused())
            followUp.selectMember(102L)
            val detail = FamilyTaskDetailViewModel(SavedStateHandle(mapOf("taskId" to 2L)), family, parent)
            assertTrue(detail.uiState.value.canValidateToday)
            detail.validateTodayOccurrence()
            assertEquals(1, validationCalls)
            assertEquals(FamilyTaskStatus.VALIDATED, detail.uiState.value.todayOccurrence?.status)
            assertEquals("Tâche validée.", detail.uiState.value.successMessage)
            assertFalse(detail.uiState.value.canValidateToday)
            followUp.refresh()
            assertEquals(102L, followUp.uiState.value.selectedMemberId)
            assertEquals(FamilyTaskStatus.VALIDATED, followUp.uiState.value.members.first { it.memberId == 102L }.items.single().familyTask?.status)
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
                assertEquals(listOf(user.id), vm.uiState.value.members.map { it.id })
                assertEquals(user.id, vm.uiState.value.selectedMemberId)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `exploration contains only own personal actions and never house quests`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val base = familyRepository()
            val family = object : FamilyTasksRepository by base {
                override suspend fun fetchToday() = Result.success(FamilyTasksToday(1L, "2026-10-04", listOf(
                    occurrence(1L, "Quest", 101L, null),
                    occurrence(2L, "Papa mission", 100L, FamilyActionType.PERSONAL_MISSION.category),
                    occurrence(3L, "Sibling mission", 102L, FamilyActionType.PERSONAL_MISSION.category),
                    occurrence(4L, "Child routine", 101L, FamilyActionType.PERSONAL_ROUTINE.category),
                )))
            }
            val parent = exploration(identity(100L, "PARENT"), true, family = family).uiState.value
            assertEquals(listOf("Papa mission"), parent.personalTasks.map { it.occurrence.title })
            assertTrue(parent.routines.isEmpty())
            assertTrue(parent.houseTasks.isEmpty())
            val child = exploration(identity(101L, "CHILD"), true, family = family).uiState.value
            assertTrue(child.personalTasks.isEmpty())
            assertEquals(listOf("Child routine"), child.routines.map { it.title })
            assertTrue(child.houseTasks.isEmpty())
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

    private fun occurrence(id: Long, title: String, assignee: Long, category: String?) = FamilyTaskTodayItem(
        id, id, title, listOf(FamilyTaskAssignee(assignee, title)), "2026-10-04", "2026-10-04", null, false, null,
        FamilyTaskStatus.TODO, false, false, FamilyTaskPriority.NORMAL, category = category,
    )

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected call: ${method.name}")
    } as T
}
