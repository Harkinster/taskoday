package com.example.taskoday.domain.model

data class PointsTransaction(
    val id: Long = 0L,
    val amount: Int,
    val reason: String,
    val sourceType: PointsSourceType,
    val sourceId: Long?,
    val dayStartMillis: Long?,
    val createdAt: Long,
)
