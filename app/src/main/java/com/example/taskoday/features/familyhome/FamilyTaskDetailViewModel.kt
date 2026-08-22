package com.example.taskoday.features.familyhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyTaskDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val familyTasksRepository: FamilyTasksRepository,
    ) : ViewModel() {
        private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L
        private val _uiState = MutableStateFlow(FamilyTaskDetailUiState())
        val uiState: StateFlow<FamilyTaskDetailUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            if (taskId <= 0L) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        task = null,
                        todayOccurrence = null,
                        errorMessage = "Tache familiale introuvable.",
                    )
                }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                familyTasksRepository
                    .fetchTask(taskId)
                    .onSuccess { task ->
                        val todayOccurrence =
                            familyTasksRepository
                                .fetchToday()
                                .getOrNull()
                                ?.tasks
                                ?.firstOrNull { occurrence -> occurrence.taskId == taskId }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                task = task,
                                todayOccurrence = todayOccurrence,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                task = null,
                                todayOccurrence = null,
                                errorMessage = throwable.toRemoteUserMessage("Impossible de charger la tache."),
                            )
                        }
                    }
            }
        }

        fun requestDelete() {
            _uiState.update { it.copy(showDeleteConfirmation = true, errorMessage = null) }
        }

        fun dismissDelete() {
            _uiState.update { it.copy(showDeleteConfirmation = false) }
        }

        fun deleteTask() {
            if (_uiState.value.isDeleting || taskId <= 0L) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isDeleting = true,
                        showDeleteConfirmation = false,
                        errorMessage = null,
                    )
                }
                familyTasksRepository
                    .deleteTask(taskId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                deleted = true,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                errorMessage = throwable.toRemoteUserMessage("Impossible de retirer la tache."),
                            )
                        }
                    }
            }
        }
    }
