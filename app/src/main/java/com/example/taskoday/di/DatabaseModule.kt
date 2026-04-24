package com.example.taskoday.di

import android.content.Context
import androidx.room.Room
import com.example.taskoday.data.local.AppDatabase
import com.example.taskoday.data.local.SeedDatabaseCallback
import com.example.taskoday.data.local.dao.ProjectDao
import com.example.taskoday.data.local.dao.RoutineDao
import com.example.taskoday.data.local.dao.TagDao
import com.example.taskoday.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seedDatabaseCallback: SeedDatabaseCallback,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(seedDatabaseCallback)
            .build()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao = database.routineDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()
}
