package com.example.taskoday.domain.model

data class Task(
    val id: Long = 0L,
    val title: String,
    val emoji: String = "\u2B50",
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.TODO,
    val taskType: TaskType = TaskType.ONE_TIME,
    val dayPart: DayPart = DayPart.MATIN,
    val scheduledDate: Long? = null,
    // Empty set => every day. Non-empty => ISO days 1..7 (Mon..Sun).
    val routineDays: Set<Int> = emptySet(),
    val projectId: Long? = null,
    val isRoutine: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isDaily: Boolean
        get() = taskType == TaskType.DAILY || isRoutine

    val isEveryDayRoutine: Boolean
        get() = isDaily && routineDays.isEmpty()
}
