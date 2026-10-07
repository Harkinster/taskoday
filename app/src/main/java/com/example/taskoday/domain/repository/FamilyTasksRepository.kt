package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskOccurrencesRange
import com.example.taskoday.domain.model.FamilyTasksToday
import com.example.taskoday.domain.model.CompletionReward

interface FamilyTasksRepository {
    suspend fun fetchToday(): Result<FamilyTasksToday>

    suspend fun fetchOccurrences(
        startDate: String,
        endDate: String,
    ): Result<FamilyTaskOccurrencesRange>

    suspend fun fetchOverdueOccurrences(): Result<FamilyTaskOccurrencesRange>

    suspend fun fetchTasks(): Result<List<FamilyTaskDefinition>>

    suspend fun fetchTask(taskId: Long): Result<FamilyTaskDefinition>

    suspend fun fetchMembers(): Result<List<FamilyTaskMember>>

    suspend fun createTask(input: FamilyTaskCreateInput): Result<Unit>

    suspend fun updateTask(
        taskId: Long,
        input: FamilyTaskCreateInput,
    ): Result<Unit>

    suspend fun deleteTask(taskId: Long): Result<Unit>

    suspend fun completeOccurrence(occurrenceId: Long): Result<Unit>

    suspend fun completeOccurrenceWithReward(occurrenceId: Long): Result<Int> =
        completeOccurrence(occurrenceId).map { 0 }

    suspend fun completeOccurrenceWithBundle(occurrenceId: Long): Result<CompletionReward> =
        completeOccurrenceWithReward(occurrenceId).map { CompletionReward(xp = it) }

    suspend fun startOccurrence(occurrenceId: Long): Result<Unit> = Result.failure(UnsupportedOperationException())

    suspend fun joinOccurrence(occurrenceId: Long): Result<Unit> = Result.failure(UnsupportedOperationException())

    suspend fun rescheduleOccurrence(occurrenceId: Long, dueDate: String): Result<Unit> = Result.failure(UnsupportedOperationException())

    suspend fun failOccurrence(occurrenceId: Long): Result<Unit> = Result.failure(UnsupportedOperationException())

    suspend fun validateOccurrence(occurrenceId: Long): Result<Unit>

    suspend fun validateOccurrenceWithReward(occurrenceId: Long): Result<Int> =
        validateOccurrence(occurrenceId).map { 0 }

    suspend fun validateOccurrenceWithBundle(occurrenceId: Long): Result<CompletionReward> =
        validateOccurrenceWithReward(occurrenceId).map { CompletionReward(xp = it) }

    suspend fun reopenOccurrence(occurrenceId: Long): Result<Unit>
}
