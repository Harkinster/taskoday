package com.example.taskoday.domain.model

data class XpHistoryEntry(
    val dateLabel: String,
    val amount: Int,
    val reason: String? = null,
)
