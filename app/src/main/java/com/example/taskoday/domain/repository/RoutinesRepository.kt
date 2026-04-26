package com.example.taskoday.domain.repository

data class RoutinesSyncResult(
    val usedRemoteData: Boolean,
    val errorMessage: String? = null,
)

interface RoutinesRepository {
    suspend fun syncRoutinesForDay(dayStartMillis: Long): RoutinesSyncResult
}
