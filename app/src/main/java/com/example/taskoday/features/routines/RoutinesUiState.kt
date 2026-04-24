package com.example.taskoday.features.routines

import com.example.taskoday.domain.model.Routine

data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
