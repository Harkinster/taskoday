package com.example.taskoday.data.demo

import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory, deterministic data for the DEBUG-only visual/demo path. */
@Singleton
class DemoTaskDataSource
    @Inject
    constructor() {
        private val checkedByDay = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

        fun observeTasks(): Flow<List<Task>> = kotlinx.coroutines.flow.flowOf(tasks())

        fun observeMissionTasks(): Flow<List<Task>> =
            kotlinx.coroutines.flow.flowOf(tasks().filter { task -> !task.isDaily })

        fun observeTask(taskId: Long): Flow<Task?> =
            kotlinx.coroutines.flow.flowOf(tasks().firstOrNull { task -> task.id == taskId })

        fun observeTasksForDay(dayStartMillis: Long): Flow<List<TaskForDay>> =
            checkedByDay.map { checkedMap ->
                val checked = checkedMap[dayStartMillis] ?: defaultCheckedIds()
                tasks().map { task -> TaskForDay(task = task, isChecked = task.id in checked) }
            }

        fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>> =
            kotlinx.coroutines.flow.flowOf(
                tasks().filter { task ->
                    task.dueDate?.let { dueDate -> dueDate in startMillis..endMillis } == true
                },
            )

        fun setChecked(taskId: Long, dayStartMillis: Long, checked: Boolean) {
            checkedByDay.value = checkedByDay.value.toMutableMap().apply {
                val current = (get(dayStartMillis) ?: defaultCheckedIds()).toMutableSet()
                if (checked) current += taskId else current -= taskId
                put(dayStartMillis, current)
            }
        }

        fun clear() {
            checkedByDay.value = emptyMap()
        }

        private fun defaultCheckedIds(): Set<Long> = setOf(COMPLETED_TASK_ID)

        private fun tasks(): List<Task> {
            val today = DateTimeUtils.startOfDayMillis()
            val now = System.currentTimeMillis()
            return listOf(
                Task(
                    id = NORMAL_TASK_ID,
                    title = "Préparer le sac",
                    emoji = "✦",
                    description = "Tout ce qu’il faut pour demain",
                    dueDate = today + 18 * MILLIS_PER_HOUR,
                    priority = TaskPriority.NORMAL,
                    status = TaskStatus.TODO,
                    taskType = TaskType.DAILY,
                    dayPart = DayPart.SOIR,
                    isRoutine = true,
                    createdAt = now,
                    updatedAt = now,
                ),
                Task(
                    id = COMPLETED_TASK_ID,
                    title = "Donner à manger au chat",
                    emoji = "♡",
                    description = "Une petite attention pour la maison",
                    dueDate = today + 9 * MILLIS_PER_HOUR,
                    priority = TaskPriority.NORMAL,
                    status = TaskStatus.TODO,
                    taskType = TaskType.DAILY,
                    dayPart = DayPart.MATINEE,
                    isRoutine = true,
                    createdAt = now,
                    updatedAt = now,
                ),
                Task(
                    id = OVERDUE_TASK_ID,
                    title = "Ranger le bureau",
                    emoji = "◇",
                    description = "La tâche attend encore un peu d’attention",
                    dueDate = today - MILLIS_PER_HOUR,
                    priority = TaskPriority.HIGH,
                    status = TaskStatus.TODO,
                    taskType = TaskType.ONE_TIME,
                    dayPart = DayPart.APRES_MIDI,
                    scheduledDate = today,
                    createdAt = now,
                    updatedAt = now,
                ),
                Task(
                    id = MISSION_TASK_ID,
                    title = "Préparer la sortie familiale",
                    emoji = "✧",
                    description = "Une mission importante, étape par étape",
                    dueDate = today + 20 * MILLIS_PER_HOUR,
                    priority = TaskPriority.NORMAL,
                    status = TaskStatus.IN_PROGRESS,
                    taskType = TaskType.ONE_TIME,
                    dayPart = DayPart.APRES_MIDI,
                    scheduledDate = today,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        private companion object {
            const val NORMAL_TASK_ID = 7001L
            const val COMPLETED_TASK_ID = 7002L
            const val OVERDUE_TASK_ID = 7003L
            const val MISSION_TASK_ID = 7004L
            const val MILLIS_PER_HOUR = 60L * 60L * 1000L
        }
    }
