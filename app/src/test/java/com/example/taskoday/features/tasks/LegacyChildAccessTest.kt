package com.example.taskoday.features.tasks

import androidx.lifecycle.SavedStateHandle
import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import com.example.taskoday.features.tasks.detail.TaskDetailViewModel
import com.example.taskoday.features.quests.QuestsViewModel
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyChildAccessTest {
    private val task = Task(id = -112L, title = "Remote mission", createdAt = 0L, updatedAt = 0L)

    @Test fun `child detail cannot delete or reopen through direct callbacks`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = TaskDetailViewModel(SavedStateHandle(mapOf("taskId" to task.id)), tasks(), unused(), unused(), auth("CHILD"))
            assertFalse(vm.uiState.value.canManageTask)
            vm.deleteTask()
            vm.updateStatus(TaskStatus.TODO)
            assertFalse(vm.uiState.value.isDeleted)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `child missions cannot delete through direct callback`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val missions = object : MissionsRepository by unused() {
                override suspend fun syncMissions() = MissionsSyncResult(true)
            }
            val vm = TasksViewModel(tasks(), missions, auth("CHILD"))
            vm.deleteTask(task.id)
            assertTrue(vm.uiState.value.manageableTaskIds.isEmpty())
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `parent detail retains delete permission`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var deleted = false
            val tasks = object : TaskRepository by tasks() {
                override suspend fun deleteTask(taskId: Long) { deleted = true }
            }
            val missions = object : MissionsRepository by unused() {
                override suspend fun deleteMission(localTaskId: Long) = Result.success(Unit)
            }
            val vm = TaskDetailViewModel(SavedStateHandle(mapOf("taskId" to task.id)), tasks, missions, unused(), auth("PARENT"))
            vm.deleteTask()
            assertTrue(deleted)
            assertTrue(vm.uiState.value.isDeleted)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `child cannot undo objective through direct callback`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val quests = object : QuestRepository by unused() {
                override fun observeQuestsForDay(dayStartMillis: Long) = flowOf(emptyList<QuestForDay>())
            }
            val remote = object : QuestsRepository by unused() {
                override suspend fun syncQuests() = QuestsSyncResult(true)
            }
            val points = object : PointsRepository by unused() {
                override fun observeBalance() = flowOf(0)
            }
            val vm = QuestsViewModel(auth("CHILD"), quests, remote, points)
            vm.setQuestCompleted(QuestForDay(Quest(id = -133L, title = "Remote objective", createdAt = 0L, updatedAt = 0L), true), false)
            assertFalse(vm.uiState.value.canManageQuests)
        } finally { Dispatchers.resetMain() }
    }
    private fun tasks() = object : TaskRepository by unused() {
        override fun observeTask(taskId: Long) = flowOf(task)
        override fun observeMissionTasks() = flowOf(listOf(task))
    }
    private fun auth(role: String) = object : AuthRepository by unused() {
        override fun getAccessToken() = "stored-access"
        override suspend fun fetchMe() = AuthenticatedUser(101L, "user@example.test", role, true, listOf(1L), "User")
    }
    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Forbidden callback: ${method.name}")
    } as T
}
