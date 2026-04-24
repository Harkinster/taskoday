package com.example.taskoday.features.home

import com.example.taskoday.domain.model.Routine
import com.example.taskoday.domain.model.Task

data class HomeUiState(
    val dateLabel: String = "",
    val todayTasks: List<Task> = emptyList(),
    val activeRoutines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
