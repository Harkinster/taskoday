package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.RoutineItemDto
import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutinesRepositoryImplTest {
    @Test
    fun `completed remote routine is stored and checked for selected day`() =
        runBlocking {
            val taskRepository = RecordingTaskRepository()
            val repository =
                RoutinesRepositoryImpl(
                    authRepository = FakeRoutineAuthRepository(),
                    childrenApi = FakeRoutineChildrenApi(completed = true),
                    taskRepository = taskRepository,
                )

            val result = repository.syncRoutinesForDay(dayStartMillis = 1_717_977_600_000L)

            assertTrue(result.usedRemoteData)
            assertEquals(TaskStatus.DONE, taskRepository.savedTask?.status)
            assertEquals(true, taskRepository.lastChecked)
        }
}

private class FakeRoutineAuthRepository : AuthRepository {
    override suspend fun registerParent(email: String, password: String, familyName: String, birthDate: String): AuthSession = error("Not used")
    override suspend fun registerChild(email: String, password: String, displayName: String, birthDate: String?): AuthSession = error("Not used")
    override suspend fun login(email: String, password: String): AuthSession = error("Not used")
    override suspend fun fetchMe(): AuthenticatedUser = error("Not used")
    override fun getAccessToken(): String = "token"
    override suspend fun getActiveChildId(forceRefresh: Boolean): Long = 19L
    override fun setActiveChildId(childId: Long) = Unit
    override fun clearSession() = Unit
}

private class FakeRoutineChildrenApi(
    private val completed: Boolean,
) : ChildrenApi {
    override suspend fun getChildren(): ApiEnvelopeDto<List<ChildResponseDto>> = error("Not used")
    override suspend fun getChild(childId: Long): ApiEnvelopeDto<ChildResponseDto> = error("Not used")
    override suspend fun getProfile(childId: Long): ApiEnvelopeDto<ChildProfileResponseDto> = error("Not used")

    override suspend fun getRoutines(childId: Long): ApiEnvelopeDto<List<RoutineItemDto>> =
        ApiEnvelopeDto(
            success = true,
            data =
                listOf(
                    RoutineItemDto(
                        id = 5L,
                        title = "Audit reward routine",
                        completed = completed,
                    ),
                ),
        )
}

private class RecordingTaskRepository : TaskRepository {
    var savedTask: Task? = null
    var lastChecked: Boolean? = null

    override fun observeTasks(): Flow<List<Task>> = flowOf(emptyList())
    override fun observeMissionTasks(): Flow<List<Task>> = flowOf(emptyList())
    override fun observeTask(taskId: Long): Flow<Task?> = flowOf(null)
    override fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>> = flowOf(emptyList())
    override fun observeTasksForDay(dayStartMillis: Long): Flow<List<TaskForDay>> = flowOf(emptyList())

    override suspend fun upsertTask(task: Task): Long {
        savedTask = task
        return task.id
    }

    override suspend fun deleteTask(taskId: Long) = Unit
    override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) = Unit

    override suspend fun setTaskCheckedForDay(taskId: Long, dayStartMillis: Long, checked: Boolean) {
        lastChecked = checked
    }

    override suspend fun cleanupOldTaskChecks(minDayStartMillis: Long) = Unit
    override suspend fun clearRemoteRoutineCache() = Unit
    override suspend fun clearRemoteMissionCache() = Unit
    override suspend fun clearRemoteCache() = Unit
}
