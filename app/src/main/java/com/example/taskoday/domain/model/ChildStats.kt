package com.example.taskoday.domain.model

data class ChildStats(
    val totalXp: Int = 0,
    val missionsCompleted: Int = 0,
    val missionsTotal: Int = 0,
    val questsCompleted: Int = 0,
    val questsTotal: Int = 0,
    val streakDays: Int = 0,
    val successRatePercent: Int = 0,
)
