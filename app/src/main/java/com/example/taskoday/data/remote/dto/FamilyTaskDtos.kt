package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class FamilyTasksTodayResponseDto(
    @SerializedName(value = "tasks", alternate = ["items", "occurrences"])
    val tasks: List<FamilyTaskOccurrenceDto> = emptyList(),
    @SerializedName(value = "date", alternate = ["today", "scheduled_date"])
    val date: String? = null,
)

data class FamilyTaskOccurrenceDto(
    @SerializedName(value = "task_id", alternate = ["taskId"])
    val taskId: Long? = null,
    @SerializedName(value = "occurrence_id", alternate = ["occurrenceId", "id"])
    val occurrenceId: Long? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("assignees")
    val assignees: List<FamilyTaskAssigneeDto> = emptyList(),
    @SerializedName(value = "scheduled_date", alternate = ["scheduledDate"])
    val scheduledDate: String? = null,
    @SerializedName(value = "due_at", alternate = ["dueAt"])
    val dueAt: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName(value = "validation_required", alternate = ["validationRequired"])
    val validationRequired: Boolean? = null,
    @SerializedName(value = "gamification_enabled", alternate = ["gamificationEnabled"])
    val gamificationEnabled: Boolean? = null,
    @SerializedName("priority")
    val priority: String? = null,
    @SerializedName(value = "recurrence", alternate = ["recurrence_label", "repeat_label"])
    val recurrenceLabel: String? = null,
)

data class FamilyTaskAssigneeDto(
    @SerializedName(value = "id", alternate = ["child_id", "child_profile_id", "user_id"])
    val id: Long? = null,
    @SerializedName(value = "display_name", alternate = ["name", "title", "email"])
    val displayName: String? = null,
)

data class FamilyTaskMemberDto(
    @SerializedName(value = "user_id", alternate = ["id"])
    val userId: Long,
    @SerializedName(value = "display_name", alternate = ["name"])
    val displayName: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("role")
    val role: String? = null,
)

data class FamilyTaskCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("due_at")
    val dueAt: String,
    @SerializedName("recurrence")
    val recurrence: String,
    @SerializedName("selected_weekdays")
    val selectedWeekdays: List<Int>? = null,
    @SerializedName("assignee_user_ids")
    val assigneeUserIds: List<Long>,
    @SerializedName("validation_required")
    val validationRequired: Boolean,
    @SerializedName("gamification_enabled")
    val gamificationEnabled: Boolean,
)

fun JsonElement.toFamilyTasksTodayResponseDto(gson: Gson): FamilyTasksTodayResponseDto =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyTaskOccurrenceDto>>() {}.type
            FamilyTasksTodayResponseDto(tasks = gson.fromJson(this, listType))
        }
        isJsonObject -> gson.fromJson(this, FamilyTasksTodayResponseDto::class.java)
        else -> FamilyTasksTodayResponseDto()
    }

fun JsonElement.toFamilyTaskMemberDtos(gson: Gson): List<FamilyTaskMemberDto> =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyTaskMemberDto>>() {}.type
            gson.fromJson(this, listType)
        }
        isJsonObject -> {
            val listType = object : TypeToken<List<FamilyTaskMemberDto>>() {}.type
            val nestedMembers =
                asJsonObject.get("children")
                    ?: asJsonObject.get("members")
            if (nestedMembers?.isJsonArray == true) {
                gson.fromJson(nestedMembers, listType)
            } else {
                emptyList()
            }
        }
        else -> emptyList()
    }

fun FamilyTaskOccurrenceDto.toDomain(): FamilyTaskTodayItem {
    val resolvedOccurrenceId = occurrenceId ?: taskId ?: 0L
    return FamilyTaskTodayItem(
        taskId = taskId ?: resolvedOccurrenceId,
        occurrenceId = resolvedOccurrenceId,
        title = title?.trim()?.takeIf { it.isNotBlank() } ?: "Tâche sans titre",
        assignees = assignees.mapNotNull { assignee -> assignee.toDomainOrNull() },
        scheduledDate = scheduledDate?.trim()?.takeIf { it.isNotBlank() },
        dueAt = dueAt?.trim()?.takeIf { it.isNotBlank() },
        status = FamilyTaskStatus.fromBackend(status),
        validationRequired = validationRequired ?: false,
        gamificationEnabled = gamificationEnabled ?: false,
        priority = FamilyTaskPriority.fromBackend(priority),
        recurrenceLabel = recurrenceLabel?.trim()?.takeIf { it.isNotBlank() },
    )
}

fun FamilyTaskMemberDto.toDomain(): FamilyTaskMember =
    FamilyTaskMember(
        userId = userId,
        displayName =
            displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")
                ?: "Membre #$userId",
        email = email,
        role =
            if (role.equals("PARENT", ignoreCase = true)) {
                FamilyTaskMemberRole.PARENT
            } else {
                FamilyTaskMemberRole.CHILD
            },
    )

fun FamilyTaskCreateInput.toRequestDto(): FamilyTaskCreateRequestDto =
    FamilyTaskCreateRequestDto(
        title = title,
        description = description,
        priority = priority.backendValue(),
        dueAt = dueAt,
        recurrence = recurrence.name,
        selectedWeekdays = selectedWeekdays.takeIf { recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS },
        assigneeUserIds = assigneeUserIds,
        validationRequired = validationRequired,
        gamificationEnabled = gamificationEnabled,
    )

private fun FamilyTaskAssigneeDto.toDomainOrNull(): FamilyTaskAssignee? {
    val label =
        displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: id?.let { "Membre #$it" }
            ?: return null
    return FamilyTaskAssignee(
        id = id,
        displayName = label,
    )
}

private fun FamilyTaskPriority.backendValue(): String =
    when (this) {
        FamilyTaskPriority.LOW -> "LOW"
        FamilyTaskPriority.NORMAL,
        FamilyTaskPriority.UNKNOWN,
        -> "NORMAL"
        FamilyTaskPriority.HIGH -> "HIGH"
        FamilyTaskPriority.URGENT -> "URGENT"
    }
