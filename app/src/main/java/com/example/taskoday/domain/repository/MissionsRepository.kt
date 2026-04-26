package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Task

data class MissionsSyncResult(
    val usedRemoteData: Boolean,
    val errorMessage: String? = null,
)

interface MissionsRepository {
    suspend fun syncMissions(): MissionsSyncResult

    suspend fun createMissionFromTask(task: Task): Result<Task>

    suspend fun updateMissionFromTask(localTaskId: Long, task: Task): Result<Task>

    suspend fun completeMission(localTaskId: Long): Result<Unit>

    suspend fun deleteMission(localTaskId: Long): Result<Unit>
}
