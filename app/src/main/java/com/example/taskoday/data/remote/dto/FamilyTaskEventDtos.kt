package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.model.FamilyTaskEventType
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class FamilyTaskEventsResponseDto(
    @SerializedName("family_id") val familyId: Long,
    @SerializedName("items") val items: List<FamilyTaskEventDto> = emptyList(),
)

data class FamilyTaskEventActorDto(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("display_name") val displayName: String? = null,
)

data class FamilyTaskEventDto(
    @SerializedName("id") val id: Long,
    @SerializedName("family_id") val familyId: Long,
    @SerializedName("task_id") val taskId: Long,
    @SerializedName("occurrence_id") val occurrenceId: Long,
    @SerializedName("category") val category: String,
    @SerializedName("scope") val scope: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("status_from") val statusFrom: String,
    @SerializedName("status_to") val statusTo: String,
    @SerializedName("actor_user_id") val actorUserId: Long,
    @SerializedName("actor_user") val actorUser: FamilyTaskEventActorDto? = null,
    @SerializedName("completed_by_user_id") val completedByUserId: Long? = null,
    @SerializedName("participant_user_ids") val participantUserIds: List<Long> = emptyList(),
    @SerializedName("occurred_at") val occurredAt: String,
    @SerializedName("legacy_inferred") val legacyInferred: Boolean = false,
)

fun JsonElement.toFamilyTaskEventsResponseDto(gson: Gson): FamilyTaskEventsResponseDto =
    gson.fromJson(this, FamilyTaskEventsResponseDto::class.java)

fun FamilyTaskEventDto.toDomain(): FamilyTaskEvent = FamilyTaskEvent(
    id = id,
    familyId = familyId,
    taskId = taskId,
    occurrenceId = occurrenceId,
    category = FamilyActionType.fromWire(scope, kind, category),
    title = title,
    eventType = FamilyTaskEventType.valueOf(eventType),
    statusFrom = FamilyTaskStatus.fromBackend(statusFrom),
    statusTo = FamilyTaskStatus.fromBackend(statusTo),
    actorUserId = actorUserId,
    actorName = actorUser?.displayName?.takeIf { it.isNotBlank() } ?: "Membre de la famille",
    completedByUserId = completedByUserId,
    participantUserIds = participantUserIds,
    occurredAt = parseFamilyEventInstant(occurredAt),
    legacyInferred = legacyInferred,
)

private fun parseFamilyEventInstant(raw: String): Instant =
    runCatching { Instant.parse(raw) }.getOrElse {
        runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrElse {
            LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)
        }
    }
