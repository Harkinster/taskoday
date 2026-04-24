package com.example.taskoday.features.projects

import com.example.taskoday.domain.model.Project

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
