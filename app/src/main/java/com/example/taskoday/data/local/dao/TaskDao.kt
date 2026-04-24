package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        ORDER BY 
            CASE status
                WHEN 'TODO' THEN 0
                WHEN 'IN_PROGRESS' THEN 1
                ELSE 2
            END,
            COALESCE(dueDate, 9223372036854775807)
        """,
    )
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeById(taskId: Long): Flow<TaskEntity?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate BETWEEN :startMillis AND :endMillis
        ORDER BY dueDate ASC
        """,
    )
    fun observeDueBetween(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: TaskStatus, updatedAt: Long)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>
}
