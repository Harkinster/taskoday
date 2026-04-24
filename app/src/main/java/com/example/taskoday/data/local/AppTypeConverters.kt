package com.example.taskoday.data.local

import androidx.room.TypeConverter
import com.example.taskoday.domain.model.RoutineFrequency
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus

class AppTypeConverters {
    @TypeConverter
    fun taskPriorityFromString(value: String): TaskPriority = TaskPriority.fromStorage(value)

    @TypeConverter
    fun taskPriorityToString(value: TaskPriority): String = value.name

    @TypeConverter
    fun taskStatusFromString(value: String): TaskStatus =
        TaskStatus.entries.firstOrNull { it.name == value } ?: TaskStatus.TODO

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun routineFrequencyFromString(value: String): RoutineFrequency =
        RoutineFrequency.entries.firstOrNull { it.name == value } ?: RoutineFrequency.DAILY

    @TypeConverter
    fun routineFrequencyToString(value: RoutineFrequency): String = value.name
}
