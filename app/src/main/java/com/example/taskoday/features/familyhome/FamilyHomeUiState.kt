package com.example.taskoday.features.familyhome

data class FamilyHomeUiState(
    val isLoading: Boolean = true,
    val familyId: Long? = null,
    val dateLabel: String = "",
    val sections: List<FamilyTaskMemberSection> = emptyList(),
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val actingOccurrenceId: Long? = null,
    val errorMessage: String? = null,
    val userMessage: String? = null,
)

data class FamilyTaskMemberSection(
    val key: String,
    val name: String,
    val completedCount: Int,
    val totalCount: Int,
    val tasks: List<FamilyTaskRow>,
)

data class FamilyTaskRow(
    val task: com.example.taskoday.domain.model.FamilyTaskTodayItem,
)
