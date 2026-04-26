package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query(
        """
        SELECT * FROM quests
        WHERE isActive = 1
        ORDER BY
            CASE dayPart
                WHEN 'MATIN' THEN 0
                WHEN 'MATINEE' THEN 1
                WHEN 'MIDI' THEN 2
                WHEN 'APRES_MIDI' THEN 3
                WHEN 'SOIR' THEN 4
                WHEN 'SOIREE' THEN 5
                ELSE 6
            END,
            id ASC
        """,
    )
    fun observeActive(): Flow<List<QuestEntity>>

    @Upsert
    suspend fun upsert(quest: QuestEntity): Long

    @Query("DELETE FROM quests WHERE id = :questId")
    suspend fun deleteById(questId: Long)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(quests: List<QuestEntity>): List<Long>

    @Query("DELETE FROM quests WHERE id < 0")
    suspend fun deleteRemoteCachedQuests()
}
