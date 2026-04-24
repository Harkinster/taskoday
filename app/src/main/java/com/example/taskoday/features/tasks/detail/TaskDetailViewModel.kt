package com.example.taskoday.features.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.TaskRepository
import com.example.taskoday.navigation.TaskodayDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val taskRepository: TaskRepository,
    ) : ViewModel() {
        private val taskId: Long = savedStateHandle[TaskodayDestination.TaskDetail.ARG_TASK_ID] ?: -1L

        private val _uiState = MutableStateFlow(TaskDetailUiState())
        val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

        init {
            if (taskId <= 0L) {
                _uiState.value = TaskDetailUiState(isLoading = false, errorMessage = "Identifiant de tâche invalide.")
            } else {
                observeTask(taskId)
            }
        }

        private fun observeTask(id: Long) {
            viewModelScope.launch {
                taskRepository.observeTask(id).collect { task ->
                    _uiState.value =
                        TaskDetailUiState(
                            task = task,
                            isLoading = false,
                            errorMessage = if (task == null) "Tâche introuvable." else null,
                        )
                }
            }
        }

        fun updateStatus(status: TaskStatus) {
            val id = uiState.value.task?.id ?: return
            viewModelScope.launch {
                taskRepository.updateTaskStatus(taskId = id, status = status)
            }
        }

        fun markTaskAsDone() {
            updateStatus(TaskStatus.DONE)
        }

        fun deleteTask() {
            val id = uiState.value.task?.id ?: return
            viewModelScope.launch {
                taskRepository.deleteTask(id)
                _uiState.update { it.copy(isDeleted = true) }
            }
        }
    }
