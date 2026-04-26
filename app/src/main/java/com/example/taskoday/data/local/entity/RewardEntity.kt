package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rewards",
    indices = [Index("isActive"), Index("cost")],
)
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String?,
    val cost: Int,
    val emoji: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
