package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId"), Index("dueDate"), Index("taskType"), Index("dayPart"), Index("scheduledDate"), Index("routineDays")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val emoji: String,
    val description: String?,
    val dueDate: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val taskType: TaskType,
    val dayPart: DayPart,
    val scheduledDate: Long?,
    // Empty/NULL = routine visible every day. CSV tokens with commas = ",1,4,".
    val routineDays: String?,
    val projectId: Long?,
    val isRoutine: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
