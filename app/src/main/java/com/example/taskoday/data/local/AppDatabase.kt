package com.example.taskoday.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taskoday.data.local.dao.ProjectDao
import com.example.taskoday.data.local.dao.RoutineDao
import com.example.taskoday.data.local.dao.TagDao
import com.example.taskoday.data.local.dao.TaskDao
import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.data.local.entity.TaskTagCrossRefEntity

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        RoutineEntity::class,
        TagEntity::class,
        TaskTagCrossRefEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun routineDao(): RoutineDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME: String = "taskoday.db"
    }
}
