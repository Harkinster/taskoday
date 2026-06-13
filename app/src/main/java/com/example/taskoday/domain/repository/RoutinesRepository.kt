package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Task

data class RoutinesSyncResult(
    val usedRemoteData: Boolean,
    val errorMessage: String? = null,
)

interface RoutinesRepository {
    suspend fun syncRoutinesForDay(dayStartMillis: Long): RoutinesSyncResult

    suspend fun updateRoutineFromTask(localTaskId: Long, task: Task): Result<Task>

    suspend fun deleteRoutine(localTaskId: Long): Result<Unit>
}
