package com.example.taskoday.data.local

import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.domain.model.RoutineFrequency
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId

object SeedData {
    fun projects(): List<ProjectEntity> =
        listOf(
            ProjectEntity(name = "Produit", color = "#2E7D32", icon = "rocket"),
            ProjectEntity(name = "Personnel", color = "#1565C0", icon = "person"),
            ProjectEntity(name = "Santé", color = "#EF6C00", icon = "heart"),
        )

    fun tags(): List<TagEntity> =
        listOf(
            TagEntity(name = "urgence", color = "#C62828"),
            TagEntity(name = "focus", color = "#6A1B9A"),
            TagEntity(name = "victoire-rapide", color = "#00897B"),
        )

    fun routines(nowMillis: Long): List<RoutineEntity> =
        listOf(
            RoutineEntity(
                title = "Revue du matin",
                frequency = RoutineFrequency.DAILY,
                customDays = null,
                reminderTime = "08:00",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RoutineEntity(
                title = "Planification hebdomadaire",
                frequency = RoutineFrequency.WEEKLY,
                customDays = null,
                reminderTime = "09:00",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RoutineEntity(
                title = "Pause étirements",
                frequency = RoutineFrequency.CUSTOM,
                customDays = "MON,WED,FRI",
                reminderTime = "15:00",
                isActive = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    fun tasks(projectIds: List<Long>, nowMillis: Long): List<TaskEntity> {
        val zoneId = ZoneId.systemDefault()
        val todayStart =
            LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = todayStart + DAY_MILLIS
        val nextWeek = todayStart + (7L * DAY_MILLIS)

        val productId = projectIds.getOrNull(0)
        val personalId = projectIds.getOrNull(1)
        val healthId = projectIds.getOrNull(2)

        return listOf(
            TaskEntity(
                title = "Définir les priorités du prochain sprint",
                description = "Aligner la roadmap et le backlog de la semaine.",
                dueDate = todayStart + 10L * HOUR_MILLIS,
                priority = TaskPriority.HIGH,
                status = TaskStatus.IN_PROGRESS,
                projectId = productId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Inbox zéro",
                description = "Traiter et archiver tous les messages ouverts.",
                dueDate = todayStart + 17L * HOUR_MILLIS,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.TODO,
                projectId = personalId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Course du soir",
                description = "30 minutes à allure facile.",
                dueDate = tomorrowStart + 19L * HOUR_MILLIS,
                priority = TaskPriority.LOW,
                status = TaskStatus.TODO,
                projectId = healthId,
                isRoutine = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Revoir les objectifs trimestriels",
                description = "Vérifier l’avancement et ajuster les jalons.",
                dueDate = nextWeek + 9L * HOUR_MILLIS,
                priority = TaskPriority.URGENT,
                status = TaskStatus.TODO,
                projectId = productId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Planifier les activités familiales du week-end",
                description = null,
                dueDate = null,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.DONE,
                projectId = personalId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
    }

    private const val HOUR_MILLIS: Long = 60L * 60L * 1000L
    private const val DAY_MILLIS: Long = 24L * HOUR_MILLIS
}
