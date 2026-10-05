package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyTaskEventsResponseDto
import com.example.taskoday.data.remote.familytasks.FamilyTasksApi
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTaskEventsRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

@Singleton
class FamilyTaskEventsRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: FamilyTasksApi,
    private val gson: Gson,
) : FamilyTaskEventsRepository {
    override suspend fun fetchSince(startAt: Instant): Result<List<FamilyTaskEvent>> = runCatching {
        val familyId = authRepository.getActiveFamilyId() ?: error("Aucune famille active.")
        val events = mutableListOf<FamilyTaskEvent>()
        var offset = 0
        do {
            val page = api.getTaskEvents(familyId, startAt.toString(), PAGE_SIZE, offset)
                .data.toFamilyTaskEventsResponseDto(gson)
            check(page.familyId == familyId) { "Réponse d'une autre famille." }
            events += page.items.filter { it.familyId == familyId }.map { it.toDomain() }
            offset += page.items.size
        } while (page.items.size == PAGE_SIZE)
        events.distinctBy { it.id }.sortedWith(compareByDescending<FamilyTaskEvent> { it.occurredAt }.thenByDescending { it.id })
    }

    private companion object { const val PAGE_SIZE = 100 }
}
