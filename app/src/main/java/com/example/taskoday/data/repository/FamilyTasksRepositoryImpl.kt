package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyTasksTodayResponseDto
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.FamilyTasksToday
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyTasksRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val familyTasksApi: FamilyTasksApi,
        private val gson: Gson,
    ) : FamilyTasksRepository {
        override suspend fun fetchToday(): Result<FamilyTasksToday> =
            runCatching {
                val familyId = resolveFamilyId()
                val response = familyTasksApi.getTodayTasks(familyId)
                val today = response.data.toFamilyTasksTodayResponseDto(gson)
                FamilyTasksToday(
                    familyId = familyId,
                    date = today.date,
                    tasks = today.tasks.map { task -> task.toDomain() },
                )
            }

        override suspend fun completeOccurrence(occurrenceId: Long): Result<Unit> =
            runCatching {
                familyTasksApi.completeOccurrence(occurrenceId)
                Unit
            }

        override suspend fun validateOccurrence(occurrenceId: Long): Result<Unit> =
            runCatching {
                familyTasksApi.validateOccurrence(occurrenceId)
                Unit
            }

        override suspend fun reopenOccurrence(occurrenceId: Long): Result<Unit> =
            runCatching {
                familyTasksApi.reopenOccurrence(occurrenceId)
                Unit
            }

        private suspend fun resolveFamilyId(): Long =
            authRepository
                .fetchMe()
                .familyIds
                .firstOrNull()
                ?: error("Aucune famille active pour ce compte.")
    }
