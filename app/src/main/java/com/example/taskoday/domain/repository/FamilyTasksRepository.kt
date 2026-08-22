package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskOccurrencesRange
import com.example.taskoday.domain.model.FamilyTasksToday

interface FamilyTasksRepository {
    suspend fun fetchToday(): Result<FamilyTasksToday>

    suspend fun fetchOccurrences(
        startDate: String,
        endDate: String,
    ): Result<FamilyTaskOccurrencesRange>

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

    suspend fun validateOccurrence(occurrenceId: Long): Result<Unit>

    suspend fun reopenOccurrence(occurrenceId: Long): Result<Unit>
}
