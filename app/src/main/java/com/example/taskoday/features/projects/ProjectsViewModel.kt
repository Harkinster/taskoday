package com.example.taskoday.features.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProjectsViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProjectsUiState())
        val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                projectRepository.observeProjects().collect { projects ->
                    _uiState.value = ProjectsUiState(projects = projects, isLoading = false)
                }
            }
        }
    }
