package com.example.taskoday.di

import com.example.taskoday.data.repository.ProjectRepositoryImpl
import com.example.taskoday.data.repository.RoutineRepositoryImpl
import com.example.taskoday.data.repository.TagRepositoryImpl
import com.example.taskoday.data.repository.TaskRepositoryImpl
import com.example.taskoday.domain.repository.ProjectRepository
import com.example.taskoday.domain.repository.RoutineRepository
import com.example.taskoday.domain.repository.TagRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
