package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskoday.domain.model.PointsSourceType

@Entity(
    tableName = "points_transactions",
    indices = [Index("sourceType"), Index("sourceId"), Index("dayStartMillis"), Index("createdAt")],
)
data class PointsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Int,
    val reason: String,
    val sourceType: PointsSourceType,
    val sourceId: Long?,
    val dayStartMillis: Long?,
    val createdAt: Long,
)
