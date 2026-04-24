package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.ProjectDao
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Project
import com.example.taskoday.domain.repository.ProjectRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepositoryImpl
    @Inject
    constructor(
        private val projectDao: ProjectDao,
    ) : ProjectRepository {
        override fun observeProjects(): Flow<List<Project>> =
            projectDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun upsertProject(project: Project): Long = projectDao.upsert(project.toEntity())
    }
