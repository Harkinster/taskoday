package com.example.taskoday.features.tasks.edit

import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.Project
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType

data class TaskEditUiState(
    val taskId: Long? = null,
    val title: String = "",
    val emoji: String = "\u2B50",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.TODO,
    val taskType: TaskType = TaskType.ONE_TIME,
    val dayPart: DayPart = DayPart.MATIN,
    val scheduledDate: Long = 0L,
    // Empty = every day.
    val routineDays: Set<Int> = emptySet(),
    val projectId: Long? = null,
    val isRoutine: Boolean = false,
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val savedTaskId: Long? = null,
    val errorMessage: String? = null,
)
