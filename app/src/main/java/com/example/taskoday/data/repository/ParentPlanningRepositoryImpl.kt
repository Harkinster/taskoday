package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ChildMissionCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildQuestCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildRoutineCreateRequestDto
import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ParentPlanningRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentPlanningRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val childrenApi: ChildrenApi,
        private val planningApi: PlanningApi,
    ) : ParentPlanningRepository {
        override suspend fun isCurrentUserParent(): Boolean {
            val user = authRepository.fetchMe()
            return user.role.equals("PARENT", ignoreCase = true)
        }

        override suspend fun fetchChildren(): List<ParentChild> =
            childrenApi.getChildren().map { child ->
                ParentChild(
                    id = child.id,
                    displayName = child.displayName,
                    email = child.email,
                )
            }

        override suspend fun getSelectedChildId(): Long? = authRepository.getActiveChildId()

        override fun setSelectedChildId(childId: Long) {
            authRepository.setActiveChildId(childId)
        }

        override suspend fun createRoutine(
            childId: Long,
            title: String,
            description: String?,
            dayPart: DayPart,
            selectedWeekdays: Set<Int>,
            points: Int,
        ) {
            val isDaily = selectedWeekdays.isEmpty() || selectedWeekdays.size == 7
            planningApi.createRoutine(
                childId = childId,
                payload =
                    ChildRoutineCreateRequestDto(
                        title = title.trim(),
                        description = description?.trim().takeUnless { it.isNullOrBlank() },
                        dayPart = dayPart.name,
                        frequency = if (isDaily) "DAILY" else "SELECTED_DAYS",
                        daysOfWeek =
                            if (isDaily) {
                                null
                            } else {
                                selectedWeekdays.sorted().joinToString(",")
                            },
                        pointsReward = points.coerceIn(0, 100),
                        isActive = true,
                    ),
            )
        }

        override suspend fun createMission(
            childId: Long,
            title: String,
            description: String?,
            dayPart: DayPart,
            scheduledDate: LocalDate,
            points: Int,
        ) {
            planningApi.createMission(
                childId = childId,
                payload =
                    ChildMissionCreateRequestDto(
                        title = title.trim(),
                        description = description?.trim().takeUnless { it.isNullOrBlank() },
                        dayPart = dayPart.name,
                        scheduledDate = scheduledDate.toString(),
                        pointsReward = points.coerceIn(0, 100),
                        isActive = true,
                    ),
            )
        }

        override suspend fun createQuest(
            childId: Long,
            title: String,
            description: String?,
            dayPart: DayPart,
            points: Int,
        ) {
            planningApi.createQuest(
                childId = childId,
                payload =
                    ChildQuestCreateRequestDto(
                        title = title.trim(),
                        description = description?.trim().takeUnless { it.isNullOrBlank() },
                        dayPart = dayPart.name,
                        pointsReward = points.coerceIn(0, 100),
                        isActive = true,
                    ),
            )
        }
    }
