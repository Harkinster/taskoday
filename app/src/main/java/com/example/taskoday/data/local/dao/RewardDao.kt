package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.RewardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE isActive = 1 ORDER BY cost ASC, id ASC")
    fun observeActive(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE id = :rewardId LIMIT 1")
    suspend fun getById(rewardId: Long): RewardEntity?

    @Upsert
    suspend fun upsert(reward: RewardEntity): Long

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewards: List<RewardEntity>): List<Long>
}
