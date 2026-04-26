package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taskoday.data.local.entity.PointsTransactionEntity
import com.example.taskoday.domain.model.PointsSourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface PointsTransactionDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM points_transactions")
    fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM points_transactions")
    suspend fun getBalance(): Int

    @Query("SELECT * FROM points_transactions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PointsTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PointsTransactionEntity): Long

    @Query(
        """
        SELECT * FROM points_transactions
        WHERE sourceType = :sourceType
          AND sourceId = :sourceId
          AND dayStartMillis = :dayStartMillis
        LIMIT 1
        """,
    )
    suspend fun findBySourceForDay(
        sourceType: PointsSourceType,
        sourceId: Long,
        dayStartMillis: Long,
    ): PointsTransactionEntity?

    @Query(
        """
        DELETE FROM points_transactions
        WHERE sourceType = :sourceType
          AND sourceId = :sourceId
          AND dayStartMillis = :dayStartMillis
        """,
    )
    suspend fun deleteBySourceForDay(
        sourceType: PointsSourceType,
        sourceId: Long,
        dayStartMillis: Long,
    )
}
