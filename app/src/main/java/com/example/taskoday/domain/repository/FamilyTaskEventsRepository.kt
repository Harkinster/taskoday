package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.FamilyTaskEvent
import java.time.Instant

interface FamilyTaskEventsRepository {
    suspend fun fetchSince(startAt: Instant): Result<List<FamilyTaskEvent>>
}
