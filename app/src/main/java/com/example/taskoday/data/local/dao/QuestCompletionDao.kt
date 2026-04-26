package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.QuestCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestCompletionDao {
    @Query(
        """
        SELECT q.*, CASE WHEN qc.questId IS NULL THEN 0 ELSE 1 END AS isCompletedForDay
        FROM quests q
        LEFT JOIN quest_completions qc
            ON qc.questId = q.id
           AND qc.dayStartMillis = :dayStartMillis
        WHERE q.isActive = 1
        ORDER BY
            CASE q.dayPart
                WHEN 'MATIN' THEN 0
                WHEN 'MATINEE' THEN 1
                WHEN 'MIDI' THEN 2
                WHEN 'APRES_MIDI' THEN 3
                WHEN 'SOIR' THEN 4
                WHEN 'SOIREE' THEN 5
                ELSE 6
            END,
            q.id ASC
        """,
    )
    fun observeForDay(dayStartMillis: Long): Flow<List<QuestWithCompletionRow>>

    @Upsert
    suspend fun upsert(completion: QuestCompletionEntity)

    @Query("DELETE FROM quest_completions WHERE questId = :questId AND dayStartMillis = :dayStartMillis")
    suspend fun deleteForDay(questId: Long, dayStartMillis: Long)

    @Query("DELETE FROM quest_completions WHERE questId < 0")
    suspend fun deleteRemoteCachedCompletions()
}
