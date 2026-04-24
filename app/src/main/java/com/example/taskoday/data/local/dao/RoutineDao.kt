package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isActive = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<RoutineEntity>>

    @Upsert
    suspend fun upsert(routine: RoutineEntity): Long

    @Query("UPDATE routines SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :routineId")
    suspend fun setActive(routineId: Long, isActive: Boolean, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routines: List<RoutineEntity>): List<Long>
}
