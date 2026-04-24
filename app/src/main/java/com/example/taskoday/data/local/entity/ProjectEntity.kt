package com.example.taskoday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val color: String,
    val icon: String,
)
