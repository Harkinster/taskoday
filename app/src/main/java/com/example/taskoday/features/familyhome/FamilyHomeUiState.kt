package com.example.taskoday.features.familyhome

data class FamilyHomeUiState(
    val isLoading: Boolean = true,
    val mode: FamilyHomeMode = FamilyHomeMode.TODAY,
    val familyId: Long? = null,
    val dateLabel: String = "",
    val sections: List<FamilyTaskMemberSection> = emptyList(),
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val weekRangeLabel: String = "",
    val selectedWeekDateLabel: String = "",
    val selectedWeekDate: String? = null,
    val weekDays: List<FamilyTaskWeekDaySummary> = emptyList(),
    val isCurrentWeek: Boolean = true,
    val isWeekEmpty: Boolean = false,
    val actingOccurrenceId: Long? = null,
    val errorMessage: String? = null,
    val userMessage: String? = null,
)

enum class FamilyHomeMode {
    TODAY,
    WEEK,
}

data class FamilyTaskWeekDaySummary(
    val date: String,
    val weekdayLabel: String,
    val dayNumberLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val isSelected: Boolean,
    val isToday: Boolean,
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
