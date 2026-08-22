package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember

data class FamilyTaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<FamilyTaskDefinition> = emptyList(),
    val visibleTasks: List<FamilyTaskDefinition> = emptyList(),
    val members: List<FamilyTaskMember> = emptyList(),
    val filters: List<FamilyTaskListFilterOption> = emptyList(),
    val selectedFilterKey: String = FAMILY_TASK_FILTER_ALL,
    val errorMessage: String? = null,
)

data class FamilyTaskListFilterOption(
    val key: String,
    val label: String,
)

