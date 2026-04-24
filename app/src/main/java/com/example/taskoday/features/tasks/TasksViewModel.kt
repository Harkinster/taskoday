package com.example.taskoday.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TasksUiState())
        val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

        init {
            observeTasks()
        }

        private fun observeTasks() {
            viewModelScope.launch {
                taskRepository.observeTasks().collect { tasks ->
                    _uiState.value = TasksUiState(tasks = tasks, isLoading = false)
                }
            }
        }

        fun markTaskAsDone(taskId: Long) {
            viewModelScope.launch {
                taskRepository.updateTaskStatus(taskId = taskId, status = TaskStatus.DONE)
            }
        }

        fun deleteTask(taskId: Long) {
            viewModelScope.launch {
                taskRepository.deleteTask(taskId)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
