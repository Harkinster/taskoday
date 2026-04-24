package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.RoutineDao
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Routine
import com.example.taskoday.domain.repository.RoutineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepositoryImpl
    @Inject
    constructor(
        private val routineDao: RoutineDao,
    ) : RoutineRepository {
        override fun observeRoutines(): Flow<List<Routine>> =
            routineDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override fun observeActiveRoutines(): Flow<List<Routine>> =
            routineDao.observeActive().map { entities -> entities.map { it.toDomain() } }

        override suspend fun upsertRoutine(routine: Routine): Long = routineDao.upsert(routine.toEntity())

        override suspend fun setRoutineActive(routineId: Long, isActive: Boolean) {
            routineDao.setActive(
                routineId = routineId,
                isActive = isActive,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }
