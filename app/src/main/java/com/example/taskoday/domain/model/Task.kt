package com.example.taskoday.domain.model

data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.TODO,
    val projectId: Long? = null,
    val isRoutine: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
