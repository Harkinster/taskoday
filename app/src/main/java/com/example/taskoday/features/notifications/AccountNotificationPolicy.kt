package com.example.taskoday.features.notifications

import com.example.taskoday.domain.model.*
import java.time.*

data class NotificationScope(val userId: Long, val familyId: Long, val parent: Boolean, val generation: String)
enum class NoticeKind { PERSONAL_DUE, VALIDATION_NEEDED, MISSION_VALIDATED, MISSION_RETURNED, IMPORTANT_OVERDUE, DAILY_SUMMARY }
data class LocalNotice(val key: String, val kind: NoticeKind, val occurrenceId: Long, val cycle: Int, val createdMs: Long)
data class NotificationLedger(val cursorMs: Long = 0, val delivered: Map<String, Long> = emptyMap(), val pending: List<LocalNotice> = emptyList())
data class NotificationSnapshot(val familyId: Long, val occurrences: List<FamilyTaskTodayItem>, val events: List<FamilyTaskEvent>, val childIds: Set<Long>, val parentIds: Set<Long>)

object AccountNotificationPolicy {
    fun quiet(settings: FamilyNotificationSettings, now: ZonedDateTime): Boolean {
        if (!settings.quietEnabled) return false
        val start = settings.quietStartMinute.coerceIn(0, 1439)
        val end = settings.quietEndMinute.coerceIn(0, 1439)
        val minute = now.hour * 60 + now.minute
        return if (start < end) minute in start until end else minute >= start || minute < end
    }

    fun allowed(kind: NoticeKind, settings: FamilyNotificationSettings, parent: Boolean): Boolean = when (kind) {
        NoticeKind.PERSONAL_DUE -> settings.personalDue
        NoticeKind.VALIDATION_NEEDED -> parent && settings.validationNeeded
        NoticeKind.MISSION_VALIDATED, NoticeKind.MISSION_RETURNED -> settings.missionDecision
        NoticeKind.IMPORTANT_OVERDUE -> settings.importantOverdue
        NoticeKind.DAILY_SUMMARY -> parent && settings.dailySummaryEnabled
    }

    fun candidates(scope: NotificationScope, settings: FamilyNotificationSettings, data: NotificationSnapshot,
                   since: Instant, now: ZonedDateTime): List<LocalNotice> {
        require(data.familyId == scope.familyId)
        val instant = now.toInstant()
        val notices = mutableListOf<LocalNotice>()
        for (task in data.occurrences) {
            val own = task.scope == FamilyActionScope.PERSONAL && task.assignees.any { it.id == scope.userId }
            val open = task.status == FamilyTaskStatus.TODO || task.status == FamilyTaskStatus.IN_PROGRESS
            val deadline = deadline(task, now.zone, settings.hour, settings.minute)
            if (own && open && deadline != null && !deadline.isAfter(instant.plusSeconds(900)) && !deadline.isBefore(instant.minusSeconds(3600))) {
                notices += notice("due:${task.occurrenceId}:${task.cycleNumber}:${deadline.epochSecond}",NoticeKind.PERSONAL_DUE,task,instant)
            }
            val overdue = deadline(task, now.zone, 23, 59)
            if ((scope.parent || own) && open && task.priority in setOf(FamilyTaskPriority.HIGH,FamilyTaskPriority.URGENT) && overdue != null && overdue.isBefore(instant)) {
                notices += notice("late:${task.occurrenceId}:${task.cycleNumber}:${overdue.epochSecond}",NoticeKind.IMPORTANT_OVERDUE,task,instant)
            }
            if (scope.parent && task.kind == FamilyActionKind.MISSION && task.status == FamilyTaskStatus.PENDING_VALIDATION && task.completedByUser?.userId in data.childIds) {
                notices += notice("pending:${task.occurrenceId}:${task.cycleNumber}",NoticeKind.VALIDATION_NEEDED,task,instant)
            }
        }
        for (event in data.events) {
            if (event.familyId != scope.familyId || event.legacyInferred || event.occurredAt.isBefore(since) || event.occurredAt.isAfter(instant.plusSeconds(300))) continue
            if (event.kind != FamilyActionKind.MISSION || event.completedByUserId != scope.userId || event.actorUserId !in data.parentIds) continue
            val kind = when {
                event.eventType == FamilyTaskEventType.VALIDATE && event.statusTo == FamilyTaskStatus.VALIDATED -> NoticeKind.MISSION_VALIDATED
                event.eventType == FamilyTaskEventType.REOPEN && event.statusTo == FamilyTaskStatus.TODO -> NoticeKind.MISSION_RETURNED
                else -> null
            } ?: continue
            notices += LocalNotice("event:${event.id}:${kind.name}",kind,event.occurrenceId,
                (event.cycleNumber ?: 0) + if (kind == NoticeKind.MISSION_RETURNED) 1 else 0,event.occurredAt.toEpochMilli())
        }
        if (scope.parent && settings.dailySummaryEnabled && now.hour*60+now.minute in (settings.hour*60+settings.minute)..(settings.hour*60+settings.minute+119) &&
            data.occurrences.any { it.status in setOf(FamilyTaskStatus.TODO,FamilyTaskStatus.IN_PROGRESS,FamilyTaskStatus.PENDING_VALIDATION) }) {
            notices += LocalNotice("summary:${now.toLocalDate()}",NoticeKind.DAILY_SUMMARY,0,0,instant.toEpochMilli())
        }
        return notices.filter { allowed(it.kind,settings,scope.parent) }.distinctBy { it.key }
    }

    fun relevant(notice: LocalNotice, scope: NotificationScope, data: NotificationSnapshot, freshKeys: Set<String>, now: Instant): Boolean {
        if (notice.createdMs < now.minusSeconds(86400).toEpochMilli()) return false
        if (notice.kind !in setOf(NoticeKind.MISSION_VALIDATED,NoticeKind.MISSION_RETURNED)) return notice.key in freshKeys
        val task = data.occurrences.firstOrNull { it.occurrenceId == notice.occurrenceId } ?: return false
        return (task.scope == FamilyActionScope.HOUSE || task.assignees.any { it.id == scope.userId }) && task.cycleNumber == notice.cycle &&
            task.status == if (notice.kind == NoticeKind.MISSION_VALIDATED) FamilyTaskStatus.VALIDATED else FamilyTaskStatus.TODO
    }

    private fun notice(key: String, kind: NoticeKind, task: FamilyTaskTodayItem, now: Instant) =
        LocalNotice(key,kind,task.occurrenceId,task.cycleNumber,now.toEpochMilli())

    private fun deadline(task: FamilyTaskTodayItem, zone: ZoneId, hour: Int, minute: Int): Instant? {
        task.dueAt?.let { value ->
            runCatching { return OffsetDateTime.parse(value).toInstant() }
            runCatching { return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
        }
        return runCatching {
            val date = LocalDate.parse(task.dueDate ?: task.scheduledDate ?: return null)
            val time = task.dueTime?.let { LocalTime.parse(it) } ?: LocalTime.of(hour,minute)
            date.atTime(time).atZone(zone).toInstant()
        }.getOrNull()
    }
}
