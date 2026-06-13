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
    @SerializedName("repeat_type")
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
    @SerializedName("due_date")
    val scheduledDate: String,
    @SerializedName("xp_reward")
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
    @SerializedName("xp_reward")
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

data class RemoteCompletionResponseDto(
    @SerializedName("routine_id")
    val routineId: Long? = null,
    @SerializedName("mission_id")
    val missionId: Long? = null,
    @SerializedName("quest_id")
    val questId: Long? = null,
    @SerializedName("completed")
    val completed: Boolean = true,
    @SerializedName("award")
    val award: CompletionAwardDto? = null,
)

data class CompletionAwardDto(
    @SerializedName("guardian_xp_awarded")
    val guardianXpAwarded: Int = 0,
    @SerializedName("flammeches_awarded")
    val flammechesAwarded: Int = 0,
    @SerializedName("crystals_awarded")
    val crystalsAwarded: Int = 0,
)
