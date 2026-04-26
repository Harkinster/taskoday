package com.example.taskoday.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.taskoday.data.local.entity.TaskCheckEntity

@Dao
interface TaskCheckDao {
    @Upsert
    suspend fun upsert(taskCheck: TaskCheckEntity)

    @Query("DELETE FROM task_checks WHERE taskId = :taskId AND dayStartMillis = :dayStartMillis")
    suspend fun deleteForDay(taskId: Long, dayStartMillis: Long)

    @Query("DELETE FROM task_checks WHERE dayStartMillis < :minDayStartMillis")
    suspend fun deleteChecksBefore(minDayStartMillis: Long)

    @Query("DELETE FROM task_checks WHERE taskId < 0")
    suspend fun deleteRemoteCachedChecks()

    @Query("DELETE FROM task_checks WHERE taskId < 0 AND ((-taskId) % 10) = 1")
    suspend fun deleteRemoteCachedRoutineChecks()

    @Query("DELETE FROM task_checks WHERE taskId < 0 AND ((-taskId) % 10) = 2")
    suspend fun deleteRemoteCachedMissionChecks()
}
