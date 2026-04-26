package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.RemotePlanningRef

data class PlanningSyncResult(
    val usedRemoteData: Boolean,
    val errorMessage: String? = null,
)

interface PlanningSyncRepository {
    suspend fun syncDay(dayStartMillis: Long): PlanningSyncResult

    suspend fun setCompletion(dayStartMillis: Long, remoteRef: RemotePlanningRef, completed: Boolean): Result<Unit>
}
