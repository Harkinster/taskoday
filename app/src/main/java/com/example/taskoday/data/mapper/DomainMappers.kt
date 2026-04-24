package com.example.taskoday.data.mapper

import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.domain.model.Project
import com.example.taskoday.domain.model.Routine
import com.example.taskoday.domain.model.Tag
import com.example.taskoday.domain.model.Task

fun TaskEntity.toDomain(): Task =
    Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        projectId = projectId,
        isRoutine = isRoutine,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Task.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        projectId = projectId,
        isRoutine = isRoutine,
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
