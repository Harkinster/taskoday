package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeActiveRoutines(): Flow<List<Routine>>
    suspend fun upsertRoutine(routine: Routine): Long
    suspend fun setRoutineActive(routineId: Long, isActive: Boolean)
}
