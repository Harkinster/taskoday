package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyTaskDefinitionDtos
import com.example.taskoday.data.remote.dto.toFamilyTaskMemberDtos
import com.example.taskoday.data.remote.dto.toFamilyTaskOccurrencesRangeResponseDto
import com.example.taskoday.data.remote.dto.toFamilyTasksTodayResponseDto
import com.example.taskoday.data.remote.dto.toRequestDto
import com.example.taskoday.data.remote.dto.toUpdateRequestDto
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskOccurrencesRange
import com.example.taskoday.domain.model.FamilyTaskTodayItem
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
                val definitions = loadDefinitions(familyId)
                FamilyTasksToday(
                    familyId = familyId,
                    date = today.date,
                    tasks = attachFamilyActionCategories(today.tasks.map { it.toDomain() }, definitions, familyId),
                )
            }

        override suspend fun fetchOccurrences(
            startDate: String,
            endDate: String,
        ): Result<FamilyTaskOccurrencesRange> =
            runCatching {
                val familyId = resolveFamilyId()
                val range = familyTasksApi
                    .getTaskOccurrences(
                        familyId = familyId,
                        startDate = startDate,
                        endDate = endDate,
                    ).data
                    .toFamilyTaskOccurrencesRangeResponseDto(gson)
                    .toDomain(
                        fallbackFamilyId = familyId,
                        fallbackStartDate = startDate,
                        fallbackEndDate = endDate,
                    )
                val definitions = loadDefinitions(familyId)
                range.copy(occurrences = attachFamilyActionCategories(range.occurrences, definitions, familyId))
            }

        override suspend fun fetchOverdueOccurrences(): Result<FamilyTaskOccurrencesRange> =
            runCatching {
                val familyId = resolveFamilyId()
                val range = familyTasksApi
                    .getOverdueOccurrences(familyId)
                    .data
                    .toFamilyTaskOccurrencesRangeResponseDto(gson)
                    .toDomain(
                        fallbackFamilyId = familyId,
                        fallbackStartDate = "",
                        fallbackEndDate = "",
                    )
                val definitions = loadDefinitions(familyId)
                range.copy(occurrences = attachFamilyActionCategories(range.occurrences, definitions, familyId))
            }

        override suspend fun fetchTasks(): Result<List<FamilyTaskDefinition>> =
            runCatching {
                val familyId = resolveFamilyId()
                loadDefinitions(familyId)
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
                val familyId = resolveFamilyId()
                familyTasksApi
                    .getFamilyMembers(familyId)
                    .data
                    .toFamilyTaskMemberDtos(gson)
                    .map { member -> member.toDomain() }
                    .filter { member -> member.isActive }
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
                val current = loadDefinitions(resolveFamilyId()).firstOrNull { it.id == taskId }
                    ?: error("Action introuvable dans la famille active.")
                requireUnchangedFamilyActionCategory(current.category, input.category)
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
            authRepository.getActiveFamilyId() ?: error("Aucune famille active pour ce compte.")

        private suspend fun loadDefinitions(familyId: Long): List<FamilyTaskDefinition> =
            familyTasksApi.getTasks(familyId).data.toFamilyTaskDefinitionDtos(gson)
                .map { it.toDomain() }
                .also { definitions ->
                    check(definitions.all { it.id > 0L && it.familyId == familyId }) {
                        "Définition d'action incohérente avec la famille active."
                    }
                    check(definitions.map { it.id }.distinct().size == definitions.size) {
                        "Définitions d'action dupliquées."
                    }
                    definitions.forEach { FamilyActionType.fromCategory(it.category) }
                }

    }

internal fun requireUnchangedFamilyActionCategory(existing: String?, requested: String?) {
    check(existing == requested) { "Le type d'une action existante ne peut pas être modifié." }
}

/** A missing definition is an incomplete response, never evidence of a house quest. */
internal fun attachFamilyActionCategories(
    occurrences: List<FamilyTaskTodayItem>,
    definitions: List<FamilyTaskDefinition>,
    familyId: Long,
): List<FamilyTaskTodayItem> {
    val byId = definitions.associateBy { it.id }
    return occurrences.map { occurrence ->
        val definition = byId[occurrence.taskId]
            ?: error("Définition de l'action ${occurrence.taskId} indisponible.")
        check(definition.familyId == familyId) { "Action liée à une autre famille." }
        FamilyActionType.fromCategory(definition.category)
        occurrence.copy(category = definition.category)
    }
}
