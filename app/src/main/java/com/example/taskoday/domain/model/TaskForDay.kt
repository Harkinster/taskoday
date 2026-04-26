package com.example.taskoday.domain.model

data class TaskForDay(
    val task: Task,
    val isChecked: Boolean,
) {
    val isCompleted: Boolean
        get() = isChecked
}
