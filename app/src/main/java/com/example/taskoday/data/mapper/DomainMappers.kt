package com.example.taskoday.data.mapper

import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.PointsTransactionEntity
import com.example.taskoday.data.local.entity.QuestEntity
import com.example.taskoday.data.local.entity.RewardEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Project
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.Routine
import com.example.taskoday.domain.model.Tag
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskType

fun TaskEntity.toDomain(): Task =
    Task(
        id = id,
        title = title,
        emoji = emoji,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        taskType = taskType,
        dayPart = dayPart,
        scheduledDate = scheduledDate,
        routineDays = routineDaysCsvToSet(routineDays),
        projectId = projectId,
        isRoutine = isRoutine,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Task.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        emoji = emoji,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        taskType = taskType,
        dayPart = dayPart,
        scheduledDate = scheduledDate,
        routineDays = routineDaysSetToCsv(if (taskType == TaskType.DAILY || isRoutine) routineDays else emptySet()),
        projectId = projectId,
        isRoutine = isRoutine || taskType == TaskType.DAILY,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ProjectEntity.toDomain(): Project = Project(id = id, name = name, color = color, icon = icon)

fun Project.toEntity(): ProjectEntity = ProjectEntity(id = id, name = name, color = color, icon = icon)

fun RoutineEntity.toDomain(): Routine =
    Routine(
        id = id,
        title = title,
        frequency = frequency,
        customDays = customDays,
        reminderTime = reminderTime,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Routine.toEntity(): RoutineEntity =
    RoutineEntity(
        id = id,
        title = title,
        frequency = frequency,
        customDays = customDays,
        reminderTime = reminderTime,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, color = color)

fun QuestEntity.toDomain(): Quest =
    Quest(
        id = id,
        title = title,
        description = description,
        emoji = emoji,
        pointsReward = pointsReward,
        isActive = isActive,
        dayPart = dayPart,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Quest.toEntity(): QuestEntity =
    QuestEntity(
        id = id,
        title = title,
        description = description,
        emoji = emoji,
        pointsReward = pointsReward,
        isActive = isActive,
        dayPart = dayPart,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RewardEntity.toDomain(): Reward =
    Reward(
        id = id,
        title = title,
        description = description,
        cost = cost,
        emoji = emoji,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Reward.toEntity(): RewardEntity =
    RewardEntity(
        id = id,
        title = title,
        description = description,
        cost = cost,
        emoji = emoji,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PointsTransactionEntity.toDomain(): PointsTransaction =
    PointsTransaction(
        id = id,
        amount = amount,
        reason = reason,
        sourceType = sourceType,
        sourceId = sourceId,
        dayStartMillis = dayStartMillis,
        createdAt = createdAt,
    )

private fun routineDaysCsvToSet(value: String?): Set<Int> {
    if (value.isNullOrBlank()) return emptySet()
    return value
        .trim(',')
        .split(',')
        .mapNotNull { token -> token.toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()
}

private fun routineDaysSetToCsv(value: Set<Int>): String? {
    if (value.isEmpty()) return null
    val normalized = value.filter { it in 1..7 }.sorted()
    if (normalized.isEmpty()) return null
    return "," + normalized.joinToString(",") + ","
}
