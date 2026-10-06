package com.example.taskoday.domain.model

import java.time.Instant

enum class FamilyTaskEventType { START, JOIN, COMPLETE, VALIDATE, REOPEN, RESCHEDULE, FAIL }

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
    val scope: FamilyActionScope = category.scope,
    val kind: FamilyActionKind = category.kind,
    val cycleNumber: Int? = null,
    val metadata: Map<String, String> = emptyMap(),
    val contributorUserIds: List<Long> = emptyList(),
    val contributors: List<FamilyTaskActor> = emptyList(),
)
