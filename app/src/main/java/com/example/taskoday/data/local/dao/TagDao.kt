package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Upsert
    suspend fun upsert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>
}
