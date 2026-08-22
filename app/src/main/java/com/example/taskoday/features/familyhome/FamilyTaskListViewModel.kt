package com.example.taskoday.features.familyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyTaskListViewModel
    @Inject
    constructor(
        private val familyTasksRepository: FamilyTasksRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FamilyTaskListUiState())
        val uiState: StateFlow<FamilyTaskListUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val tasksResult = familyTasksRepository.fetchTasks()
                val membersResult = familyTasksRepository.fetchMembers()

                val tasks = tasksResult.getOrNull()
                val members = membersResult.getOrNull()
                if (tasks != null && members != null) {
                    publishTasks(tasks = tasks, members = members, selectedFilterKey = _uiState.value.selectedFilterKey)
                } else {
                    val throwable = tasksResult.exceptionOrNull() ?: membersResult.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = emptyList(),
                            visibleTasks = emptyList(),
                            members = emptyList(),
                            filters = buildFamilyTaskListFilters(emptyList()),
                            errorMessage =
                                throwable?.toRemoteUserMessage("Impossible de charger les tâches familiales.")
                                    ?: "Impossible de charger les tâches familiales.",
                        )
                    }
                }
            }
        }

        fun selectFilter(filterKey: String) {
            val current = _uiState.value
            publishTasks(
                tasks = current.tasks,
                members = current.members,
                selectedFilterKey = filterKey,
                isLoading = current.isLoading,
            )
        }

        private fun publishTasks(
            tasks: List<FamilyTaskDefinition>,
            members: List<FamilyTaskMember>,
            selectedFilterKey: String,
            isLoading: Boolean = false,
        ) {
            val filters = buildFamilyTaskListFilters(members)
            val safeFilterKey = filters.firstOrNull { filter -> filter.key == selectedFilterKey }?.key ?: FAMILY_TASK_FILTER_ALL
            _uiState.update {
                it.copy(
                    isLoading = isLoading,
                    tasks = tasks,
                    visibleTasks = filterFamilyTaskDefinitions(tasks = tasks, filterKey = safeFilterKey),
                    members = members,
                    filters = filters,
                    selectedFilterKey = safeFilterKey,
                    errorMessage = null,
                )
            }
        }
    }
