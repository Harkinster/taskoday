package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.taskoday.domain.model.RoutineFrequency

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val frequency: RoutineFrequency,
    val customDays: String?,
    val reminderTime: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
