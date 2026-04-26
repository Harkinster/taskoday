package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "quest_completions",
    primaryKeys = ["questId", "dayStartMillis"],
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questId"), Index("dayStartMillis")],
)
data class QuestCompletionEntity(
    val questId: Long,
    val dayStartMillis: Long,
    val completedAt: Long,
)
