package com.example.taskoday.domain.model

data class ChildProfileDashboard(
    val profile: ChildProfile,
    val stats: ChildStats = ChildStats(),
    val xpHistory: List<XpHistoryEntry> = emptyList(),
)
