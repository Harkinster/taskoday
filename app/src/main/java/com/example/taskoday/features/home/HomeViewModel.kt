package com.example.taskoday.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.RoutineRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                HomeUiState(
                    dateLabel = DateTimeUtils.currentDateLabel(),
                    isLoading = true,
                ),
            )
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            observeHomeData()
        }

        private fun observeHomeData() {
            val (startMillis, endMillis) = DateTimeUtils.todayBoundsMillis()
            viewModelScope.launch {
                combine(
                    taskRepository.observeTasksDueBetween(startMillis, endMillis),
                    routineRepository.observeActiveRoutines(),
                ) { tasks, routines ->
                    HomeUiState(
                        dateLabel = DateTimeUtils.currentDateLabel(),
                        todayTasks = tasks,
                        activeRoutines = routines,
                        isLoading = false,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }

        fun markTaskAsDone(taskId: Long) {
            viewModelScope.launch {
                taskRepository.updateTaskStatus(taskId = taskId, status = TaskStatus.DONE)
            }
        }

        fun startTask(taskId: Long) {
            viewModelScope.launch {
                taskRepository.updateTaskStatus(taskId = taskId, status = TaskStatus.IN_PROGRESS)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
