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
        WHERE id > 0
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

    @Query(
        """
        SELECT * FROM tasks
        WHERE taskType = 'ONE_TIME'
           OR (id < 0 AND ((-id) % 10) = 2)
        ORDER BY
            CASE status
                WHEN 'TODO' THEN 0
                WHEN 'IN_PROGRESS' THEN 1
                ELSE 2
            END,
            COALESCE(dueDate, 9223372036854775807)
        """,
    )
    fun observeMissionList(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeById(taskId: Long): Flow<TaskEntity?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE id > 0
          AND dueDate BETWEEN :startMillis AND :endMillis
        ORDER BY dueDate ASC
        """,
    )
    fun observeDueBetween(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT t.*, COALESCE(tc.isChecked, 0) AS isChecked
        FROM tasks t
        LEFT JOIN task_checks tc
            ON tc.taskId = t.id
           AND tc.dayStartMillis = :dayStartMillis
        WHERE (
                t.taskType = 'DAILY'
            AND (
                    t.routineDays IS NULL
                 OR t.routineDays = ''
                 OR instr(t.routineDays, :weekdayToken) > 0
                )
        )
        OR (
                t.taskType = 'ONE_TIME'
            AND t.scheduledDate = :dayStartMillis
        )
        ORDER BY
            CASE t.dayPart
                WHEN 'MATIN' THEN 0
                WHEN 'MATINEE' THEN 1
                WHEN 'MIDI' THEN 2
                WHEN 'APRES_MIDI' THEN 3
                WHEN 'SOIR' THEN 4
                WHEN 'SOIREE' THEN 5
                ELSE 6
            END,
            COALESCE(t.dueDate, 9223372036854775807)
        """,
    )
    fun observeForDay(dayStartMillis: Long, weekdayToken: String): Flow<List<TaskWithCheckRow>>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: TaskStatus, updatedAt: Long)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>

    @Query("DELETE FROM tasks WHERE id < 0")
    suspend fun deleteRemoteCachedTasks()

    @Query("DELETE FROM tasks WHERE id < 0 AND ((-id) % 10) = 1")
    suspend fun deleteRemoteCachedRoutineTasks()

    @Query("DELETE FROM tasks WHERE id < 0 AND ((-id) % 10) = 2")
    suspend fun deleteRemoteCachedMissionTasks()
}
