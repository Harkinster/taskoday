package com.example.taskoday.features.tasks

import com.example.taskoday.domain.model.Task

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val canManageMissions: Boolean = true,
    val manageableTaskIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
