package com.example.taskoday.data.repository

import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.demo.DemoModeStore
import com.example.taskoday.data.demo.DemoTaskDataSource
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImpl
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val taskCheckDao: TaskCheckDao,
        private val demoModeStore: DemoModeStore,
        private val demoDataSource: DemoTaskDataSource,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeTasks() else taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }
            }

        override fun observeMissionTasks(): Flow<List<Task>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeMissionTasks() else taskDao.observeMissionList().map { entities -> entities.map { it.toDomain() } }
            }

        override fun observeTask(taskId: Long): Flow<Task?> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeTask(taskId) else taskDao.observeById(taskId).map { it?.toDomain() }
            }

        override fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeTasksDueBetween(startMillis, endMillis) else taskDao.observeDueBetween(startMillis, endMillis).map { entities -> entities.map { it.toDomain() } }
            }

        override fun observeTasksForDay(dayStartMillis: Long): Flow<List<TaskForDay>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) {
                    demoDataSource.observeTasksForDay(dayStartMillis)
                } else {
                    taskDao.observeForDay(dayStartMillis, weekdayToken(dayStartMillis)).map { rows ->
                        rows.map { row ->
                            TaskForDay(
                                task = row.task.toDomain(),
                                isChecked = row.isChecked,
                            )
                        }
                    }
                }
            }

        override suspend fun upsertTask(task: Task): Long =
            if (demoModeStore.isEnabled) task.id else taskDao.upsert(task.toEntity())

        override suspend fun deleteTask(taskId: Long) {
            if (!demoModeStore.isEnabled) taskDao.deleteById(taskId)
        }

        override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) {
            if (!demoModeStore.isEnabled) {
                taskDao.updateStatus(taskId = taskId, status = status, updatedAt = System.currentTimeMillis())
            }
        }

        override suspend fun setTaskCheckedForDay(taskId: Long, dayStartMillis: Long, checked: Boolean) {
            if (demoModeStore.isEnabled) {
                demoDataSource.setChecked(taskId = taskId, dayStartMillis = dayStartMillis, checked = checked)
                return
            }
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
            if (!demoModeStore.isEnabled) taskCheckDao.deleteChecksBefore(minDayStartMillis)
        }

        override suspend fun clearRemoteRoutineCache() {
            if (!demoModeStore.isEnabled) {
                taskCheckDao.deleteRemoteCachedRoutineChecks()
                taskDao.deleteRemoteCachedRoutineTasks()
            }
        }

        override suspend fun clearRemoteMissionCache() {
            if (!demoModeStore.isEnabled) {
                taskCheckDao.deleteRemoteCachedMissionChecks()
                taskDao.deleteRemoteCachedMissionTasks()
            }
        }

        override suspend fun clearRemoteCache() {
            if (!demoModeStore.isEnabled) {
                taskCheckDao.deleteRemoteCachedChecks()
                taskDao.deleteRemoteCachedTasks()
            }
        }

        private fun weekdayToken(dayStartMillis: Long): String {
            val isoDay = DateTimeUtils.dayOfWeekIso(dayStartMillis)
            return ",$isoDay,"
        }
    }
