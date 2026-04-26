package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlanningResponseDto(
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("planning_date")
    val planningDate: String,
    @SerializedName("sections")
    val sections: List<PlanningSectionDto>,
)

data class ChildRoutineCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("frequency")
    val frequency: String,
    @SerializedName("days_of_week")
    val daysOfWeek: String?,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class ChildMissionCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("scheduled_date")
    val scheduledDate: String,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class ChildQuestCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class RoutineCreateResponseDto(
    @SerializedName("id")
    val id: Long,
)

data class MissionCreateResponseDto(
    @SerializedName("id")
    val id: Long,
)

data class QuestCreateResponseDto(
    @SerializedName("id")
    val id: Long,
)

data class PlanningSectionDto(
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("items")
    val items: List<PlanningItemDto>,
)

data class PlanningItemDto(
    @SerializedName("item_type")
    val itemType: String,
    @SerializedName("item_id")
    val itemId: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_optional")
    val isOptional: Boolean,
    @SerializedName("frequency")
    val frequency: String?,
    @SerializedName("days_of_week")
    val daysOfWeek: String?,
    @SerializedName("scheduled_date")
    val scheduledDate: String?,
)

data class CompletionToggleResponseDto(
    @SerializedName("item_type")
    val itemType: String,
    @SerializedName("item_id")
    val itemId: Long,
    @SerializedName("child_id")
    val childId: Long,
    @SerializedName("completion_date")
    val completionDate: String,
    @SerializedName("completed")
    val completed: Boolean,
)
