package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChildResponseDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("family_id")
    val familyId: Long,
    @SerializedName("user_id")
    val userId: Long?,
    @SerializedName("email")
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("birth_date")
    val birthDate: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class ChildProfileResponseDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("family_id")
    val familyId: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("email")
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("birth_date")
    val birthDate: String? = null,
    @SerializedName("role")
    val role: String? = null,
)

data class RoutineItemDto(
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
    @SerializedName("frequency")
    val frequency: String? = null,
    @SerializedName("days_of_week")
    val daysOfWeek: String? = null,
    @SerializedName("points_reward")
    val pointsReward: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)
