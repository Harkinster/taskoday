package com.example.taskoday.features.activity

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.model.FamilyTaskEventType
import com.example.taskoday.domain.model.FamilyTaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class JournalPeriod(val label: String) {
    TODAY("Aujourd'hui"),
    YESTERDAY("Hier"),
    LAST_WEEK("7 derniers jours"),
}

data class JournalSection(val period: JournalPeriod, val events: List<FamilyTaskEvent>)

fun journalSections(events: List<FamilyTaskEvent>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): List<JournalSection> {
    val recent = events.filter { event ->
        val day = event.occurredAt.atZone(zone).toLocalDate()
        !day.isAfter(today) && !day.isBefore(today.minusDays(6))
    }.sortedWith(compareByDescending<FamilyTaskEvent> { it.occurredAt }.thenByDescending { it.id })
    return JournalPeriod.entries.mapNotNull { period ->
        val group = recent.filter { event ->
            val day = event.occurredAt.atZone(zone).toLocalDate()
            when (period) {
                JournalPeriod.TODAY -> day == today
                JournalPeriod.YESTERDAY -> day == today.minusDays(1)
                JournalPeriod.LAST_WEEK -> day.isBefore(today.minusDays(1))
            }
        }
        group.takeIf { it.isNotEmpty() }?.let { JournalSection(period, it) }
    }
}

fun FamilyActionType.journalLabel(): String = when (this) {
    FamilyActionType.PERSONAL_ROUTINE -> "Routine"
    FamilyActionType.PERSONAL_MISSION -> "Mission"
    FamilyActionType.HOUSE_QUEST -> "Quête Maison"
    FamilyActionType.HOUSE_ROUTINE -> "Routine Maison"
    FamilyActionType.HOUSE_MISSION -> "Mission Maison"
}

fun FamilyTaskEvent.journalEventLabel(): String = when (eventType) {
    FamilyTaskEventType.COMPLETE -> if (statusTo == FamilyTaskStatus.PENDING_VALIDATION) "Terminée · en attente de validation" else "Terminée"
    FamilyTaskEventType.VALIDATE -> "Validée"
    FamilyTaskEventType.REOPEN -> "Rouverte"
}

fun FamilyTaskEvent.journalActorLabel(): String = when (eventType) {
    FamilyTaskEventType.COMPLETE -> "Faite par $actorName"
    FamilyTaskEventType.VALIDATE -> "Validée par $actorName"
    FamilyTaskEventType.REOPEN -> "Rouverte par $actorName"
}

fun Instant.journalTimeLabel(zone: ZoneId = ZoneId.systemDefault()): String =
    atZone(zone).format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))
