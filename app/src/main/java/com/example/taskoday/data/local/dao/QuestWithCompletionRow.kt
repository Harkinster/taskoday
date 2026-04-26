package com.example.taskoday.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.taskoday.data.local.entity.QuestEntity

data class QuestWithCompletionRow(
    @Embedded val quest: QuestEntity,
    @ColumnInfo(name = "isCompletedForDay") val isCompletedForDay: Boolean,
)
