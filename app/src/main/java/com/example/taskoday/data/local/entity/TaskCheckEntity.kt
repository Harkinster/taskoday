package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_checks",
    primaryKeys = ["taskId", "dayStartMillis"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("dayStartMillis")],
)
data class TaskCheckEntity(
    val taskId: Long,
    val dayStartMillis: Long,
    val isChecked: Boolean,
    val updatedAt: Long,
)
