package com.example.taskoday.features.familyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyTaskCreateViewModel
    @Inject
    constructor(
        private val familyTasksRepository: FamilyTasksRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FamilyTaskCreateUiState())
        val uiState: StateFlow<FamilyTaskCreateUiState> = _uiState.asStateFlow()

        init {
            loadMembers()
        }

        fun loadMembers() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMembers = true, errorMessage = null) }
                familyTasksRepository
                    .fetchMembers()
                    .onSuccess { members ->
                        _uiState.update {
                            it.copy(
                                isLoadingMembers = false,
                                members = members,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingMembers = false,
                                members = emptyList(),
                                errorMessage = throwable.toRemoteUserMessage("Impossible de charger les membres."),
                            )
                        }
                    }
            }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value, errorMessage = null) }
        }

        fun onDescriptionChanged(value: String) {
            _uiState.update { it.copy(description = value, errorMessage = null) }
        }

        fun onDateChanged(value: String) {
            _uiState.update { it.copy(date = value, errorMessage = null) }
        }

        fun onTimeChanged(value: String) {
            _uiState.update { it.copy(time = value, errorMessage = null) }
        }

        fun onRecurrenceChanged(value: FamilyTaskRecurrence) {
            _uiState.update {
                it.copy(
                    recurrence = value,
                    selectedWeekdays =
                        if (value == FamilyTaskRecurrence.SELECTED_WEEKDAYS) {
                            it.selectedWeekdays
                        } else {
                            emptySet()
                        },
                    errorMessage = null,
                )
            }
        }

        fun toggleWeekday(day: Int) {
            _uiState.update {
                val next =
                    if (day in it.selectedWeekdays) {
                        it.selectedWeekdays - day
                    } else {
                        it.selectedWeekdays + day
                    }
                it.copy(selectedWeekdays = next, errorMessage = null)
            }
        }

        fun selectHouseTask() {
            _uiState.update { it.copy(selectedAssigneeUserIds = emptySet(), errorMessage = null) }
        }

        fun toggleAssignee(userId: Long) {
            _uiState.update {
                val next =
                    if (userId in it.selectedAssigneeUserIds) {
                        it.selectedAssigneeUserIds - userId
                    } else {
                        it.selectedAssigneeUserIds + userId
                    }
                it.copy(selectedAssigneeUserIds = next, errorMessage = null)
            }
        }

        fun onValidationRequiredChanged(value: Boolean) {
            _uiState.update { it.copy(validationRequired = value, errorMessage = null) }
        }

        fun onGamificationEnabledChanged(value: Boolean) {
            _uiState.update { it.copy(gamificationEnabled = value, errorMessage = null) }
        }

        fun onPriorityChanged(value: FamilyTaskPriority) {
            _uiState.update { it.copy(priority = value, errorMessage = null) }
        }

        fun submit() {
            val current = _uiState.value
            if (current.isSubmitting) return

            val validation =
                validateFamilyTaskCreateForm(
                    FamilyTaskCreateForm(
                        title = current.title,
                        description = current.description,
                        date = current.date,
                        time = current.time,
                        recurrence = current.recurrence,
                        selectedWeekdays = current.selectedWeekdays,
                        assigneeUserIds = current.selectedAssigneeUserIds,
                        validationRequired = current.validationRequired,
                        gamificationEnabled = current.gamificationEnabled,
                        priority = current.priority,
                    ),
                )
            val input = validation.input
            if (!validation.isValid || input == null) {
                _uiState.update { it.copy(errorMessage = validation.errorMessage) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                familyTasksRepository
                    .createTask(input)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, created = true) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.toRemoteUserMessage("Impossible de créer la tâche."),
                            )
                        }
                    }
            }
        }
    }
