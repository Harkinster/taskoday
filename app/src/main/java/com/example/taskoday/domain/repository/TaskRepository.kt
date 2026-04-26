package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeMissionTasks(): Flow<List<Task>>
    fun observeTask(taskId: Long): Flow<Task?>
    fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>>
    fun observeTasksForDay(dayStartMillis: Long): Flow<List<TaskForDay>>
    suspend fun upsertTask(task: Task): Long
    suspend fun deleteTask(taskId: Long)
    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus)
    suspend fun setTaskCheckedForDay(taskId: Long, dayStartMillis: Long, checked: Boolean)
    suspend fun cleanupOldTaskChecks(minDayStartMillis: Long)
    suspend fun clearRemoteRoutineCache()
    suspend fun clearRemoteMissionCache()
    suspend fun clearRemoteCache()
}
