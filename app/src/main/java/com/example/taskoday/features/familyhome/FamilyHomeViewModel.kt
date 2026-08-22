package com.example.taskoday.features.familyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyHomeViewModel
    @Inject
    constructor(
        private val familyTasksRepository: FamilyTasksRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FamilyHomeUiState())
        val uiState: StateFlow<FamilyHomeUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                loadToday(showLoading = true)
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(errorMessage = null, userMessage = null) }
        }

        fun runQuickAction(task: FamilyTaskTodayItem) {
            val action = quickActionFor(task) ?: return
            if (!canRunQuickAction(task)) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        actingOccurrenceId = task.occurrenceId,
                        errorMessage = null,
                        userMessage = null,
                    )
                }
                val result =
                    when (action) {
                        FamilyTaskQuickAction.COMPLETE -> familyTasksRepository.completeOccurrence(task.occurrenceId)
                        FamilyTaskQuickAction.VALIDATE -> familyTasksRepository.validateOccurrence(task.occurrenceId)
                        FamilyTaskQuickAction.REOPEN -> familyTasksRepository.reopenOccurrence(task.occurrenceId)
                    }
                result
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                actingOccurrenceId = null,
                                userMessage = action.successMessage(),
                            )
                        }
                        loadToday(showLoading = false)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                actingOccurrenceId = null,
                                errorMessage = throwable.toRemoteUserMessage("Action impossible pour le moment."),
                            )
                        }
                    }
            }
        }

        private suspend fun loadToday(showLoading: Boolean) {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            familyTasksRepository
                .fetchToday()
                .onSuccess { today ->
                    val sections = buildFamilyTaskSections(today.tasks)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            familyId = today.familyId,
                            dateLabel = formatFamilyHomeDateLabel(today.date),
                            sections = sections,
                            totalTasks = today.tasks.size,
                            completedTasks = today.tasks.count { task -> task.status.countsAsDone },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dateLabel = formatFamilyHomeDateLabel(null),
                            sections = emptyList(),
                            totalTasks = 0,
                            completedTasks = 0,
                            errorMessage = throwable.toRemoteUserMessage("Impossible de charger Ma maison."),
                        )
                    }
                }
        }
    }

private fun FamilyTaskQuickAction.successMessage(): String =
    when (this) {
        FamilyTaskQuickAction.COMPLETE -> "Tâche terminée."
        FamilyTaskQuickAction.VALIDATE -> "Tâche validée."
        FamilyTaskQuickAction.REOPEN -> "Tâche rouverte."
    }
