package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun upsertProject(project: Project): Long
}
