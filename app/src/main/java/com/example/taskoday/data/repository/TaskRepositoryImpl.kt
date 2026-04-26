package com.example.taskoday.data.repository

import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.local.dao.TaskCheckDao
import com.example.taskoday.data.local.dao.TaskDao
import com.example.taskoday.data.local.entity.TaskCheckEntity
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val taskCheckDao: TaskCheckDao,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> = taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override fun observeMissionTasks(): Flow<List<Task>> =
            taskDao.observeMissionList().map { entities -> entities.map { it.toDomain() } }

        override fun observeTask(taskId: Long): Flow<Task?> = taskDao.observeById(taskId).map { it?.toDomain() }

        override fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>> =
            taskDao.observeDueBetween(startMillis, endMillis).map { entities -> entities.map { it.toDomain() } }

        override fun observeTasksForDay(dayStartMillis: Long): Flow<List<TaskForDay>> =
            taskDao.observeForDay(dayStartMillis, weekdayToken(dayStartMillis)).map { rows ->
                rows.map { row ->
                    TaskForDay(
                        task = row.task.toDomain(),
                        isChecked = row.isChecked,
                    )
                }
            }

        override suspend fun upsertTask(task: Task): Long = taskDao.upsert(task.toEntity())

        override suspend fun deleteTask(taskId: Long) {
            taskDao.deleteById(taskId)
        }

        override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) {
            taskDao.updateStatus(taskId = taskId, status = status, updatedAt = System.currentTimeMillis())
        }

        override suspend fun setTaskCheckedForDay(taskId: Long, dayStartMillis: Long, checked: Boolean) {
            if (checked) {
                taskCheckDao.upsert(
                    TaskCheckEntity(
                        taskId = taskId,
                        dayStartMillis = dayStartMillis,
                        isChecked = true,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                taskCheckDao.deleteForDay(taskId = taskId, dayStartMillis = dayStartMillis)
            }
        }

        override suspend fun cleanupOldTaskChecks(minDayStartMillis: Long) {
            taskCheckDao.deleteChecksBefore(minDayStartMillis)
        }

        override suspend fun clearRemoteRoutineCache() {
            taskCheckDao.deleteRemoteCachedRoutineChecks()
            taskDao.deleteRemoteCachedRoutineTasks()
        }

        override suspend fun clearRemoteMissionCache() {
            taskCheckDao.deleteRemoteCachedMissionChecks()
            taskDao.deleteRemoteCachedMissionTasks()
        }

        override suspend fun clearRemoteCache() {
            taskCheckDao.deleteRemoteCachedChecks()
            taskDao.deleteRemoteCachedTasks()
        }

        private fun weekdayToken(dayStartMillis: Long): String {
            val isoDay = DateTimeUtils.dayOfWeekIso(dayStartMillis)
            return ",$isoDay,"
        }
    }
