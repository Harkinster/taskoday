package com.example.taskoday.features.notifications

import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.google.gson.Gson
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiNotificationSnapshotSource @Inject constructor(private val api: FamilyTasksApi, private val gson: Gson) : NotificationSnapshotSource {
    override suspend fun load(scope: NotificationScope, since: Instant, now: ZonedDateTime): NotificationSnapshot {
        val start = now.toLocalDate().minusDays(7).toString()
        val end = now.toLocalDate().plusDays(1).toString()
        val range = api.getTaskOccurrences(scope.familyId,start,end)
        check(range.success && range.data.asJsonObject["family_id"].asLong==scope.familyId)
        val tasks = range.data.toFamilyTaskOccurrencesRangeResponseDto(gson).toDomain(scope.familyId,start,end).occurrences
        val overdue = api.getOverdueOccurrences(scope.familyId)
        check(overdue.success && overdue.data.asJsonObject["family_id"].asLong==scope.familyId)
        val late = overdue.data.toFamilyTaskOccurrencesRangeResponseDto(gson).toDomain(scope.familyId,start,end).occurrences
        val membership = api.getFamilyMembers(scope.familyId)
        check(membership.success)
        val members = membership.data.toFamilyTaskMemberDtos(gson).map { it.toDomain() }.filter { it.isActive }
        check(members.any { it.userId==scope.userId })
        val events = mutableListOf<com.example.taskoday.domain.model.FamilyTaskEvent>()
        var offset = 0
        for (pageNumber in 0 until 20) {
            val response = api.getTaskEvents(scope.familyId,since.toString(),100,offset)
            check(response.success)
            val page = response.data.toFamilyTaskEventsResponseDto(gson)
            check(page.familyId==scope.familyId && page.items.all { it.familyId==scope.familyId })
            events += page.items.map { it.toDomain() };offset += page.items.size
            if (page.items.size<100) break
            check(pageNumber<19) { "Notification event window exceeds the bounded scan." }
        }
        return NotificationSnapshot(scope.familyId,(tasks+late).distinctBy { it.occurrenceId },events.distinctBy { it.id },
            members.filter { it.role==FamilyTaskMemberRole.CHILD }.map { it.userId }.toSet(),
            members.filter { it.role==FamilyTaskMemberRole.PARENT }.map { it.userId }.toSet())
    }
}
