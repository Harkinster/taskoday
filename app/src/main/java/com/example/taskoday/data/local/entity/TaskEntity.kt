package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus

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
    indices = [Index("projectId"), Index("dueDate")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val projectId: Long?,
    val isRoutine: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
