package com.example.taskoday.domain.model

data class Routine(
    val id: Long = 0L,
    val title: String,
    val frequency: RoutineFrequency,
    val customDays: String? = null,
    val reminderTime: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
