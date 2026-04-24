package com.example.taskoday.features.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RoutinesViewModel
    @Inject
    constructor(
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RoutinesUiState())
        val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                routineRepository.observeRoutines().collect { routines ->
                    _uiState.value = RoutinesUiState(routines = routines, isLoading = false)
                }
            }
        }

        fun toggleRoutine(routineId: Long, isActive: Boolean) {
            viewModelScope.launch {
                routineRepository.setRoutineActive(routineId = routineId, isActive = isActive)
            }
        }
    }
