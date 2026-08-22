package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.FamilyTasksToday

interface FamilyTasksRepository {
    suspend fun fetchToday(): Result<FamilyTasksToday>

    suspend fun completeOccurrence(occurrenceId: Long): Result<Unit>

    suspend fun validateOccurrence(occurrenceId: Long): Result<Unit>

    suspend fun reopenOccurrence(occurrenceId: Long): Result<Unit>
}
