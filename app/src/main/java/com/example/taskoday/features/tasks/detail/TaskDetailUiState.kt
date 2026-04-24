package com.example.taskoday.features.tasks.detail

import com.example.taskoday.domain.model.Task

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
)
