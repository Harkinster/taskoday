package com.example.taskoday.data.local

import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.QuestEntity
import com.example.taskoday.data.local.entity.RewardEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.RoutineFrequency
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import java.time.LocalDate
import java.time.ZoneId

object SeedData {
    fun projects(): List<ProjectEntity> =
        listOf(
            ProjectEntity(name = "Maison", color = "#7EC8A5", icon = "home"),
            ProjectEntity(name = "Ecole", color = "#8FB3FF", icon = "school"),
            ProjectEntity(name = "Famille", color = "#FFB38A", icon = "family"),
        )

    fun tags(): List<TagEntity> =
        listOf(
            TagEntity(name = "routine", color = "#6BCB77"),
            TagEntity(name = "fun", color = "#4D96FF"),
            TagEntity(name = "important", color = "#FF6B6B"),
        )

    fun routines(nowMillis: Long): List<RoutineEntity> =
        listOf(
            RoutineEntity(
                title = "Routine du matin",
                frequency = RoutineFrequency.DAILY,
                customDays = null,
                reminderTime = "07:30",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RoutineEntity(
                title = "Rangement du soir",
                frequency = RoutineFrequency.DAILY,
                customDays = null,
                reminderTime = "18:30",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    fun tasks(projectIds: List<Long>, nowMillis: Long): List<TaskEntity> {
        val zoneId = ZoneId.systemDefault()
        val todayStart =
            LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = todayStart + DAY_MILLIS

        val homeProjectId = projectIds.getOrNull(0)
        val schoolProjectId = projectIds.getOrNull(1)
        val familyProjectId = projectIds.getOrNull(2)

        return listOf(
            TaskEntity(
                title = "Se brosser les dents",
                emoji = "\uD83E\uDE65",
                description = "2 minutes avec le minuteur",
                dueDate = todayStart + 7L * HOUR_MILLIS + 30L * MINUTE_MILLIS,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.TODO,
                taskType = TaskType.DAILY,
                dayPart = DayPart.MATIN,
                scheduledDate = null,
                routineDays = null, // every day
                projectId = homeProjectId,
                isRoutine = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Faire le cartable",
                emoji = "\uD83C\uDF92",
                description = "Verifier agenda + gourde",
                dueDate = todayStart + 8L * HOUR_MILLIS,
                priority = TaskPriority.HIGH,
                status = TaskStatus.TODO,
                taskType = TaskType.DAILY,
                dayPart = DayPart.SOIR,
                scheduledDate = null,
                routineDays = ",1,2,3,4,5,",
                projectId = schoolProjectId,
                isRoutine = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Faire les devoirs",
                emoji = "\uD83D\uDCDA",
                description = "Lundi et jeudi",
                dueDate = todayStart + 18L * HOUR_MILLIS,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.TODO,
                taskType = TaskType.DAILY,
                dayPart = DayPart.SOIREE,
                scheduledDate = null,
                routineDays = ",1,4,",
                projectId = schoolProjectId,
                isRoutine = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Donner le mot a la maitresse",
                emoji = "\uD83D\uDCDD",
                description = null,
                dueDate = todayStart + 9L * HOUR_MILLIS,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.TODO,
                taskType = TaskType.ONE_TIME,
                dayPart = DayPart.MATINEE,
                scheduledDate = todayStart,
                routineDays = null,
                projectId = schoolProjectId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Rendez-vous medecin",
                emoji = "\uD83E\uDE7A",
                description = "Controle du vendredi",
                dueDate = tomorrowStart + 12L * HOUR_MILLIS,
                priority = TaskPriority.HIGH,
                status = TaskStatus.TODO,
                taskType = TaskType.ONE_TIME,
                dayPart = DayPart.MIDI,
                scheduledDate = tomorrowStart,
                routineDays = null,
                projectId = familyProjectId,
                isRoutine = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            TaskEntity(
                title = "Pause jeu",
                emoji = "\uD83C\uDFAE",
                description = null,
                dueDate = todayStart + 15L * HOUR_MILLIS,
                priority = TaskPriority.LOW,
                status = TaskStatus.TODO,
                taskType = TaskType.DAILY,
                dayPart = DayPart.APRES_MIDI,
                scheduledDate = null,
                routineDays = null,
                projectId = homeProjectId,
                isRoutine = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
    }

    fun quests(nowMillis: Long): List<QuestEntity> =
        listOf(
            QuestEntity(
                title = "Ranger 5 jouets",
                description = "Petite quête bonus",
                emoji = "\uD83E\uDDF8",
                pointsReward = 3,
                isActive = true,
                dayPart = DayPart.APRES_MIDI,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            QuestEntity(
                title = "Lire 10 minutes",
                description = "Choisir un livre préféré",
                emoji = "\uD83D\uDCDA",
                pointsReward = 3,
                isActive = true,
                dayPart = DayPart.SOIREE,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            QuestEntity(
                title = "Aider à mettre la table",
                description = null,
                emoji = "\uD83C\uDF7D\uFE0F",
                pointsReward = 3,
                isActive = true,
                dayPart = DayPart.MIDI,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    fun rewards(nowMillis: Long): List<RewardEntity> =
        listOf(
            RewardEntity(
                title = "Choisir le dessert",
                description = null,
                cost = 10,
                emoji = "\uD83C\uDF70",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RewardEntity(
                title = "15 min de jeu vidéo",
                description = null,
                cost = 20,
                emoji = "\uD83C\uDFAE",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RewardEntity(
                title = "Choisir un dessin animé",
                description = null,
                cost = 30,
                emoji = "\uD83D\uDCFA",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
            RewardEntity(
                title = "Petite surprise",
                description = "Boîte mystère",
                cost = 50,
                emoji = "\uD83C\uDF81",
                isActive = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    private const val MINUTE_MILLIS: Long = 60L * 1000L
    private const val HOUR_MILLIS: Long = 60L * 60L * 1000L
    private const val DAY_MILLIS: Long = 24L * HOUR_MILLIS
}
