package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskoday.domain.model.DayPart

@Entity(
    tableName = "quests",
    indices = [Index("isActive"), Index("dayPart")],
)
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String?,
    val emoji: String,
    val pointsReward: Int,
    val isActive: Boolean,
    val dayPart: DayPart,
    val createdAt: Long,
    val updatedAt: Long,
)
