package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.ParentChild
import java.time.LocalDate

interface ParentPlanningRepository {
    suspend fun isCurrentUserParent(): Boolean

    suspend fun fetchChildren(): List<ParentChild>

    suspend fun getSelectedChildId(): Long?

    fun setSelectedChildId(childId: Long)

    suspend fun createRoutine(
        childId: Long,
        title: String,
        description: String?,
        dayPart: DayPart,
        selectedWeekdays: Set<Int>,
        points: Int,
    )

    suspend fun createMission(
        childId: Long,
        title: String,
        description: String?,
        dayPart: DayPart,
        scheduledDate: LocalDate,
        points: Int,
    )

    suspend fun createQuest(
        childId: Long,
        title: String,
        description: String?,
        dayPart: DayPart,
        points: Int,
    )
}
