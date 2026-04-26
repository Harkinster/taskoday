package com.example.taskoday.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.taskoday.data.local.entity.TaskEntity

data class TaskWithCheckRow(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "isChecked") val isChecked: Boolean,
)
