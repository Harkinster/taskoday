package com.example.taskoday.data.local

import androidx.room.TypeConverter
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.RoutineFrequency
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType

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
    fun taskTypeFromString(value: String): TaskType =
        TaskType.entries.firstOrNull { it.name == value } ?: TaskType.ONE_TIME

    @TypeConverter
    fun taskTypeToString(value: TaskType): String = value.name

    @TypeConverter
    fun dayPartFromString(value: String): DayPart =
        when (value) {
            "MATIN" -> DayPart.MATIN
            "MATINEE" -> DayPart.MATINEE
            "MIDI" -> DayPart.MIDI
            "APRES_MIDI" -> DayPart.APRES_MIDI
            "SOIR" -> DayPart.SOIR
            "SOIREE" -> DayPart.SOIREE
            // Backward compatibility from v2/v3 values.
            "MORNING" -> DayPart.MATIN
            "LATE_MORNING" -> DayPart.MATINEE
            "NOON" -> DayPart.MIDI
            "AFTERNOON" -> DayPart.APRES_MIDI
            "EVENING" -> DayPart.SOIR
            else -> DayPart.MATIN
        }

    @TypeConverter
    fun dayPartToString(value: DayPart): String = value.name

    @TypeConverter
    fun routineFrequencyFromString(value: String): RoutineFrequency =
        RoutineFrequency.entries.firstOrNull { it.name == value } ?: RoutineFrequency.DAILY

    @TypeConverter
    fun routineFrequencyToString(value: RoutineFrequency): String = value.name

    @TypeConverter
    fun pointsSourceTypeFromString(value: String): PointsSourceType =
        PointsSourceType.entries.firstOrNull { it.name == value } ?: PointsSourceType.ROUTINE

    @TypeConverter
    fun pointsSourceTypeToString(value: PointsSourceType): String = value.name
}
