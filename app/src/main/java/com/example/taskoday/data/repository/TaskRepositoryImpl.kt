package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.TaskDao
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl
    @Inject
    constructor(
        private val taskDao: TaskDao,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> = taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override fun observeTask(taskId: Long): Flow<Task?> = taskDao.observeById(taskId).map { it?.toDomain() }

        override fun observeTasksDueBetween(startMillis: Long, endMillis: Long): Flow<List<Task>> =
            taskDao.observeDueBetween(startMillis, endMillis).map { entities -> entities.map { it.toDomain() } }

        override suspend fun upsertTask(task: Task): Long = taskDao.upsert(task.toEntity())

        override suspend fun deleteTask(taskId: Long) {
            taskDao.deleteById(taskId)
        }

        override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) {
            taskDao.updateStatus(taskId = taskId, status = status, updatedAt = System.currentTimeMillis())
        }
    }
