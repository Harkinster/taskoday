package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyTaskDefinitionDtos
import com.example.taskoday.data.remote.dto.toFamilyTaskMemberDtos
import com.example.taskoday.data.remote.dto.toFamilyTasksTodayResponseDto
import com.example.taskoday.data.remote.dto.toRequestDto
import com.example.taskoday.data.remote.dto.toUpdateRequestDto
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
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

        override suspend fun fetchTasks(): Result<List<FamilyTaskDefinition>> =
            runCatching {
                val familyId = resolveFamilyId()
                familyTasksApi
                    .getTasks(familyId)
                    .data
                    .toFamilyTaskDefinitionDtos(gson)
                    .map { task -> task.toDomain() }
            }

        override suspend fun fetchTask(taskId: Long): Result<FamilyTaskDefinition> =
            runCatching {
                fetchTasks()
                    .getOrThrow()
                    .firstOrNull { task -> task.id == taskId }
                    ?: error("Tache familiale introuvable.")
            }

        override suspend fun fetchMembers(): Result<List<FamilyTaskMember>> =
            runCatching {
                val me = authRepository.fetchMe()
                val familyId = resolveFamilyId(me.familyIds)
                val parent =
                    FamilyTaskMember(
                        userId = me.id,
                        displayName = me.email.substringBefore("@").ifBlank { "Parent" },
                        email = me.email,
                        role = FamilyTaskMemberRole.PARENT,
                    )
                val children =
                    familyTasksApi
                        .getFamilyChildren(familyId)
                        .data
                        .toFamilyTaskMemberDtos(gson)
                        .map { child -> child.toDomain() }
                listOf(parent)
                    .plus(children)
                    .distinctBy { member -> member.userId }
            }

        override suspend fun createTask(input: FamilyTaskCreateInput): Result<Unit> =
            runCatching {
                val familyId = resolveFamilyId()
                familyTasksApi.createTask(familyId, input.toRequestDto())
                Unit
            }

        override suspend fun updateTask(
            taskId: Long,
            input: FamilyTaskCreateInput,
        ): Result<Unit> =
            runCatching {
                familyTasksApi.updateTask(taskId, input.toUpdateRequestDto())
                Unit
            }

        override suspend fun deleteTask(taskId: Long): Result<Unit> =
            runCatching {
                familyTasksApi.deleteTask(taskId)
                Unit
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
            resolveFamilyId(authRepository.fetchMe().familyIds)

        private fun resolveFamilyId(familyIds: List<Long>): Long =
            familyIds.firstOrNull() ?: error("Aucune famille active pour ce compte.")
    }
