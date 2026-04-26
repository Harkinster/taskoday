package com.example.taskoday.domain.model

data class Quest(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val emoji: String = "\u2B50",
    val pointsReward: Int = 3,
    val isActive: Boolean = true,
    val dayPart: DayPart = DayPart.APRES_MIDI,
    val createdAt: Long,
    val updatedAt: Long,
)
