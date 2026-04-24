package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Upsert
    suspend fun upsert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>): List<Long>
}
