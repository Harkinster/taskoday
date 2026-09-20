package com.example.taskoday.features.exploration

import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay

data class ExplorationMember(
    val id: Long,
    val displayName: String,
)

data class ExplorationUiState(
    val isLoading: Boolean = true,
    val members: List<ExplorationMember> = emptyList(),
    val selectedMemberId: Long? = null,
    val selectedMemberName: String = "Moi",
    val dateLabel: String = "Aujourd'hui",
    val personalTasks: List<ExplorationTask> = emptyList(),
    val routines: List<ExplorationRoutineItem> = emptyList(),
    val missions: List<TaskForDay> = emptyList(),
    val houseTasks: List<ExplorationTask> = emptyList(),
    val objectives: List<QuestForDay> = emptyList(),
    val errorMessage: String? = null,
    val actingKey: String? = null,
)

data class ExplorationTask(
    val occurrence: FamilyTaskTodayItem,
    val overdue: Boolean = false,
)

data class ExplorationRoutineItem(
    val title: String,
    val subtitle: String,
    val completed: Boolean,
    val familyTask: ExplorationTask? = null,
    val legacyTask: TaskForDay? = null,
)
