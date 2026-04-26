package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MissionItemDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("child_profile_id")
    val childProfileId: Long? = null,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String? = null,
    @SerializedName("scheduled_date")
    val scheduledDate: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("is_completed")
    val isCompleted: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class MissionCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("scheduled_date")
    val scheduledDate: String,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class MissionUpdateRequestDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String? = null,
    @SerializedName("scheduled_date")
    val scheduledDate: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
)

data class QuestItemDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("child_profile_id")
    val childProfileId: Long? = null,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String? = null,
    @SerializedName("scheduled_date")
    val scheduledDate: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("is_completed")
    val isCompleted: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class QuestCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String,
    @SerializedName("scheduled_date")
    val scheduledDate: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int,
    @SerializedName("is_active")
    val isActive: Boolean = true,
)

data class QuestUpdateRequestDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("day_part")
    val dayPart: String? = null,
    @SerializedName("scheduled_date")
    val scheduledDate: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
)

data class ChildStatsDto(
    @SerializedName("total_xp")
    val totalXp: Int? = null,
    @SerializedName("missions_completed")
    val missionsCompleted: Int? = null,
    @SerializedName("missions_total")
    val missionsTotal: Int? = null,
    @SerializedName("quests_completed")
    val questsCompleted: Int? = null,
    @SerializedName("quests_total")
    val questsTotal: Int? = null,
    @SerializedName("streak_days")
    val streakDays: Int? = null,
    @SerializedName("success_rate_percent")
    val successRatePercent: Int? = null,
)

data class XpHistoryItemDto(
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("amount")
    val amount: Int? = null,
    @SerializedName("xp")
    val xp: Int? = null,
    @SerializedName("reason")
    val reason: String? = null,
    @SerializedName("description")
    val description: String? = null,
)

data class PairingCodeResponseDto(
    @SerializedName("code")
    val code: String,
    @SerializedName("family_id")
    val familyId: Long? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
)

data class AttachChildRequestDto(
    @SerializedName("code")
    val code: String,
    @SerializedName("family_id")
    val familyId: Long? = null,
)
