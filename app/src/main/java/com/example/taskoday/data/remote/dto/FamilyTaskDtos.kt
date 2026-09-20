package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskOccurrencesRange
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

data class FamilyTasksTodayResponseDto(
    @SerializedName(value = "tasks", alternate = ["items", "occurrences"])
    val tasks: List<FamilyTaskOccurrenceDto> = emptyList(),
    @SerializedName(value = "date", alternate = ["today", "scheduled_date"])
    val date: String? = null,
)

data class FamilyTaskOccurrencesRangeResponseDto(
    @SerializedName(value = "family_id", alternate = ["familyId"])
    val familyId: Long? = null,
    @SerializedName(value = "start_date", alternate = ["startDate"])
    val startDate: String? = null,
    @SerializedName(value = "end_date", alternate = ["endDate"])
    val endDate: String? = null,
    @SerializedName(value = "items", alternate = ["tasks", "occurrences"])
    val items: List<FamilyTaskOccurrenceDto> = emptyList(),
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
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    @SerializedName(value = "due_time", alternate = ["dueTime"])
    val dueTime: String? = null,
    @SerializedName(value = "has_due_time", alternate = ["hasDueTime"])
    val hasDueTime: Boolean? = null,
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
    @SerializedName(value = "recurrence_interval", alternate = ["recurrenceInterval"])
    val recurrenceInterval: Int? = null,
    @SerializedName(value = "selected_weekdays", alternate = ["selectedWeekdays"])
    val selectedWeekdays: List<Int> = emptyList(),
)

data class FamilyTaskAssigneeDto(
    @SerializedName(value = "user_id", alternate = ["id", "child_id", "child_profile_id"])
    val id: Long? = null,
    @SerializedName(value = "display_name", alternate = ["name", "title", "email"])
    val displayName: String? = null,
)

data class FamilyTaskDefinitionDto(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName(value = "family_id", alternate = ["familyId"])
    val familyId: Long? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("assignees")
    val assignees: List<FamilyTaskAssigneeDto> = emptyList(),
    @SerializedName(value = "due_at", alternate = ["dueAt"])
    val dueAt: String? = null,
    @SerializedName(value = "due_date", alternate = ["dueDate"])
    val dueDate: String? = null,
    @SerializedName(value = "due_time", alternate = ["dueTime"])
    val dueTime: String? = null,
    @SerializedName(value = "has_due_time", alternate = ["hasDueTime"])
    val hasDueTime: Boolean? = null,
    @SerializedName("recurrence")
    val recurrence: String? = null,
    @SerializedName(value = "recurrence_interval", alternate = ["recurrenceInterval"])
    val recurrenceInterval: Int? = null,
    @SerializedName(value = "selected_weekdays", alternate = ["selectedWeekdays"])
    val selectedWeekdays: List<Int> = emptyList(),
    @SerializedName(value = "validation_required", alternate = ["validationRequired"])
    val validationRequired: Boolean? = null,
    @SerializedName(value = "gamification_enabled", alternate = ["gamificationEnabled"])
    val gamificationEnabled: Boolean? = null,
    @SerializedName("priority")
    val priority: String? = null,
    @SerializedName("active")
    val active: Boolean? = null,
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
    @SerializedName(value = "is_active", alternate = ["isActive", "active"])
    val isActive: Boolean? = null,
)

@JsonAdapter(FamilyTaskCreateRequestDtoJsonAdapter::class)
data class FamilyTaskCreateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("due_date")
    val dueDate: String,
    @SerializedName("due_time")
    val dueTime: JsonElement,
    @SerializedName("recurrence")
    val recurrence: String,
    @SerializedName("recurrence_interval")
    val recurrenceInterval: Int = 1,
    @SerializedName("selected_weekdays")
    val selectedWeekdays: List<Int>? = null,
    @SerializedName("assignee_user_ids")
    val assigneeUserIds: List<Long>,
    @SerializedName("validation_required")
    val validationRequired: Boolean,
    @SerializedName("gamification_enabled")
    val gamificationEnabled: Boolean,
)

@JsonAdapter(FamilyTaskUpdateRequestDtoJsonAdapter::class)
data class FamilyTaskUpdateRequestDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("due_date")
    val dueDate: String,
    @SerializedName("due_time")
    val dueTime: JsonElement,
    @SerializedName("recurrence")
    val recurrence: String,
    @SerializedName("recurrence_interval")
    val recurrenceInterval: Int = 1,
    @SerializedName("selected_weekdays")
    val selectedWeekdays: List<Int>? = null,
    @SerializedName("assignee_user_ids")
    val assigneeUserIds: List<Long>,
    @SerializedName("validation_required")
    val validationRequired: Boolean,
    @SerializedName("gamification_enabled")
    val gamificationEnabled: Boolean,
)

class FamilyTaskCreateRequestDtoJsonAdapter : TypeAdapter<FamilyTaskCreateRequestDto>() {
    override fun write(
        out: JsonWriter,
        value: FamilyTaskCreateRequestDto?,
    ) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("title").value(value.title)
        value.description?.let { description -> out.name("description").value(description) }
        out.name("priority").value(value.priority)
        out.name("due_date").value(value.dueDate)
        out.writeDueTime(value.dueTime)
        out.name("recurrence").value(value.recurrence)
        out.name("recurrence_interval").value(value.recurrenceInterval.coerceAtLeast(1))
        value.selectedWeekdays?.let { selectedWeekdays -> out.writeIntArray("selected_weekdays", selectedWeekdays) }
        out.writeLongArray("assignee_user_ids", value.assigneeUserIds)
        out.name("validation_required").value(value.validationRequired)
        out.name("gamification_enabled").value(value.gamificationEnabled)
        out.endObject()
    }

    override fun read(reader: JsonReader): FamilyTaskCreateRequestDto =
        throw UnsupportedOperationException("FamilyTaskCreateRequestDto is write-only.")
}

class FamilyTaskUpdateRequestDtoJsonAdapter : TypeAdapter<FamilyTaskUpdateRequestDto>() {
    override fun write(
        out: JsonWriter,
        value: FamilyTaskUpdateRequestDto?,
    ) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("title").value(value.title)
        out.name("description").value(value.description)
        out.name("priority").value(value.priority)
        out.name("due_date").value(value.dueDate)
        out.writeDueTime(value.dueTime)
        out.name("recurrence").value(value.recurrence)
        out.name("recurrence_interval").value(value.recurrenceInterval.coerceAtLeast(1))
        out.writeIntArray("selected_weekdays", value.selectedWeekdays.orEmpty())
        out.writeLongArray("assignee_user_ids", value.assigneeUserIds)
        out.name("validation_required").value(value.validationRequired)
        out.name("gamification_enabled").value(value.gamificationEnabled)
        out.endObject()
    }

    override fun read(reader: JsonReader): FamilyTaskUpdateRequestDto =
        throw UnsupportedOperationException("FamilyTaskUpdateRequestDto is write-only.")
}

fun JsonElement.toFamilyTasksTodayResponseDto(gson: Gson): FamilyTasksTodayResponseDto =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyTaskOccurrenceDto>>() {}.type
            FamilyTasksTodayResponseDto(tasks = gson.fromJson(this, listType))
        }
        isJsonObject -> gson.fromJson(this, FamilyTasksTodayResponseDto::class.java)
        else -> FamilyTasksTodayResponseDto()
    }

fun JsonElement.toFamilyTaskOccurrencesRangeResponseDto(gson: Gson): FamilyTaskOccurrencesRangeResponseDto =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyTaskOccurrenceDto>>() {}.type
            FamilyTaskOccurrencesRangeResponseDto(items = gson.fromJson(this, listType))
        }
        isJsonObject -> gson.fromJson(this, FamilyTaskOccurrencesRangeResponseDto::class.java)
        else -> FamilyTaskOccurrencesRangeResponseDto()
    }

fun JsonElement.toFamilyTaskDefinitionDtos(gson: Gson): List<FamilyTaskDefinitionDto> =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyTaskDefinitionDto>>() {}.type
            gson.fromJson(this, listType)
        }
        isJsonObject -> {
            val listType = object : TypeToken<List<FamilyTaskDefinitionDto>>() {}.type
            val nestedTasks =
                asJsonObject.get("tasks")
                    ?: asJsonObject.get("items")
            if (nestedTasks?.isJsonArray == true) {
                gson.fromJson(nestedTasks, listType)
            } else {
                emptyList()
            }
        }
        else -> emptyList()
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
        dueDate = dueDate.normalizedDateOrNull(),
        dueTime = dueTime.normalizedTimeOrNull(),
        hasDueTime = resolveHasDueTime(hasDueTime = hasDueTime, dueTime = dueTime, dueAt = dueAt),
        dueAt = dueAt?.trim()?.takeIf { it.isNotBlank() },
        status = FamilyTaskStatus.fromBackend(status),
        validationRequired = validationRequired ?: false,
        gamificationEnabled = gamificationEnabled ?: false,
        priority = FamilyTaskPriority.fromBackend(priority),
        recurrenceLabel = recurrenceLabel?.trim()?.takeIf { it.isNotBlank() },
        recurrence = FamilyTaskRecurrence.fromBackend(recurrenceLabel),
        recurrenceInterval = recurrenceInterval?.coerceAtLeast(1) ?: 1,
        selectedWeekdays = selectedWeekdays.filter { it in 1..7 }.distinct().sorted(),
    )
}

fun FamilyTaskOccurrencesRangeResponseDto.toDomain(
    fallbackFamilyId: Long,
    fallbackStartDate: String,
    fallbackEndDate: String,
): FamilyTaskOccurrencesRange =
    FamilyTaskOccurrencesRange(
        familyId = familyId ?: fallbackFamilyId,
        startDate = startDate.normalizedDateOrNull() ?: fallbackStartDate,
        endDate = endDate.normalizedDateOrNull() ?: fallbackEndDate,
        occurrences = items.map { item -> item.toDomain() },
    )

fun FamilyTaskDefinitionDto.toDomain(): FamilyTaskDefinition {
    val resolvedId = id ?: 0L
    return FamilyTaskDefinition(
        id = resolvedId,
        familyId = familyId ?: 0L,
        title = title?.trim()?.takeIf { it.isNotBlank() } ?: "Tache sans titre",
        description = description?.trim()?.takeIf { it.isNotBlank() },
        assignees = assignees.mapNotNull { assignee -> assignee.toDomainOrNull() },
        dueDate = dueDate.normalizedDateOrNull(),
        dueTime = dueTime.normalizedTimeOrNull(),
        hasDueTime = resolveHasDueTime(hasDueTime = hasDueTime, dueTime = dueTime, dueAt = dueAt),
        dueAt = dueAt?.trim()?.takeIf { it.isNotBlank() },
        recurrence = FamilyTaskRecurrence.fromBackend(recurrence),
        recurrenceInterval = recurrenceInterval?.coerceAtLeast(1) ?: 1,
        selectedWeekdays = selectedWeekdays.filter { day -> day in 1..7 }.distinct().sorted(),
        validationRequired = validationRequired ?: false,
        gamificationEnabled = gamificationEnabled ?: false,
        priority = FamilyTaskPriority.fromBackend(priority),
        active = active ?: true,
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
        isActive = isActive ?: true,
    )

fun FamilyTaskCreateInput.toRequestDto(): FamilyTaskCreateRequestDto =
    FamilyTaskCreateRequestDto(
        title = title,
        description = description,
        priority = priority.backendValue(),
        dueDate = dueDate,
        dueTime = dueTime.toDueTimeJsonElement(),
        recurrence = recurrence.name,
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        selectedWeekdays = selectedWeekdays.takeIf { recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS },
        assigneeUserIds = assigneeUserIds,
        validationRequired = validationRequired,
        gamificationEnabled = gamificationEnabled,
    )

fun FamilyTaskCreateInput.toUpdateRequestDto(): FamilyTaskUpdateRequestDto =
    FamilyTaskUpdateRequestDto(
        title = title,
        description = description.orEmpty(),
        priority = priority.backendValue(),
        dueDate = dueDate,
        dueTime = dueTime.toDueTimeJsonElement(),
        recurrence = recurrence.name,
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        selectedWeekdays =
            if (recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS) {
                selectedWeekdays
            } else {
                emptyList()
            },
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

private fun JsonWriter.writeDueTime(dueTime: JsonElement) {
    name("due_time")
    val previousSerializeNulls = serializeNulls
    serializeNulls = true
    if (dueTime.isJsonNull) {
        nullValue()
    } else {
        value(dueTime.asString)
    }
    serializeNulls = previousSerializeNulls
}

private fun JsonWriter.writeIntArray(
    name: String,
    values: List<Int>,
) {
    name(name)
    beginArray()
    values.forEach { item -> value(item.toLong()) }
    endArray()
}

private fun JsonWriter.writeLongArray(
    name: String,
    values: List<Long>,
) {
    name(name)
    beginArray()
    values.forEach { item -> value(item) }
    endArray()
}

private fun String?.normalizedDateOrNull(): String? {
    val trimmed = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return trimmed.substringBefore("T").takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
}

private fun String?.normalizedTimeOrNull(): String? {
    val trimmed = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val candidate =
        trimmed
            .substringAfter("T", trimmed)
            .substringBefore("Z")
            .substringBefore("+")
    return candidate.takeIf { it.length >= 5 }?.take(5)
}

private fun resolveHasDueTime(
    hasDueTime: Boolean?,
    dueTime: String?,
    dueAt: String?,
): Boolean =
    hasDueTime
        ?: dueTime.normalizedTimeOrNull()?.isNotBlank()
        ?: dueAt.normalizedTimeOrNull()?.let { time -> time != "00:00" }
        ?: false

private fun String?.toDueTimeJsonElement(): JsonElement =
    normalizedTimeOrNull()?.let { time -> JsonPrimitive(time) } ?: JsonNull.INSTANCE
