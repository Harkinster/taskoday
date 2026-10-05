package com.example.taskoday.domain.model

import java.time.Instant

enum class FamilyTaskEventType { COMPLETE, VALIDATE, REOPEN }

data class FamilyTaskEvent(
    val id: Long,
    val familyId: Long,
    val taskId: Long,
    val occurrenceId: Long,
    val category: FamilyActionType,
    val title: String,
    val eventType: FamilyTaskEventType,
    val statusFrom: FamilyTaskStatus,
    val statusTo: FamilyTaskStatus,
    val actorUserId: Long,
    val actorName: String,
    val completedByUserId: Long?,
    val participantUserIds: List<Long>,
    val occurredAt: Instant,
    val legacyInferred: Boolean,
)
