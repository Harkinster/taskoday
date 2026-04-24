package com.example.taskoday.features.tasks.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.ProjectRepository
import com.example.taskoday.domain.repository.TaskRepository
import com.example.taskoday.navigation.TaskodayDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val taskRepository: TaskRepository,
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val editingTaskId: Long? =
            (savedStateHandle[TaskodayDestination.TaskEdit.ARG_TASK_ID] ?: -1L).takeIf { it > 0L }

        private var loadedTask: Task? = null

        private val _uiState = MutableStateFlow(TaskEditUiState())
        val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

        init {
            observeProjects()
            loadTaskIfNeeded()
        }

        private fun observeProjects() {
            viewModelScope.launch {
                projectRepository.observeProjects().collect { projects ->
                    _uiState.update { current ->
                        current.copy(
                            projects = projects,
                            projectId = current.projectId ?: projects.firstOrNull()?.id,
                        )
                    }
                }
            }
        }

        private fun loadTaskIfNeeded() {
            viewModelScope.launch {
                if (editingTaskId == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val task = taskRepository.observeTask(editingTaskId).first()
                loadedTask = task
                if (task == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Tâche introuvable.",
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description.orEmpty(),
                        dueDate = task.dueDate,
                        priority = task.priority,
                        status = task.status,
                        projectId = task.projectId,
                        isRoutine = task.isRoutine,
                        isLoading = false,
                    )
                }
            }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value, errorMessage = null) }
        }

        fun onDescriptionChanged(value: String) {
            _uiState.update { it.copy(description = value) }
        }

        fun onPriorityChanged(priority: TaskPriority) {
            _uiState.update { it.copy(priority = priority) }
        }

        fun onStatusChanged(status: TaskStatus) {
            _uiState.update { it.copy(status = status) }
        }

        fun onProjectChanged(projectId: Long?) {
            _uiState.update { it.copy(projectId = projectId) }
        }

        fun onRoutineChanged(isRoutine: Boolean) {
            _uiState.update { it.copy(isRoutine = isRoutine) }
        }

        fun onDueDateChanged(epochMillis: Long?) {
            _uiState.update { it.copy(dueDate = epochMillis) }
        }

        fun saveTask() {
            val state = uiState.value
            val trimmedTitle = state.title.trim()
            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre est obligatoire.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }

                val now = System.currentTimeMillis()
                val existing = loadedTask
                val taskToSave =
                    Task(
                        id = existing?.id ?: 0L,
                        title = trimmedTitle,
                        description = state.description.trim().ifBlank { null },
                        dueDate = state.dueDate,
                        priority = state.priority,
                        status = state.status,
                        projectId = state.projectId,
                        isRoutine = state.isRoutine,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                val savedId = taskRepository.upsertTask(taskToSave)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveCompleted = true,
                        savedTaskId = if (existing == null) savedId else existing.id,
                    )
                }
            }
        }

        fun consumeSaveCompletion() {
            _uiState.update { it.copy(saveCompleted = false) }
        }
    }
