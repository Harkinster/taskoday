package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.ChildProfileDashboard
import com.example.taskoday.domain.model.ChildProfile

interface ProfileRepository {
    suspend fun updateMyProfile(displayName: String, birthDate: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Profile update is unavailable."))

    suspend fun fetchActiveChildProfile(): ChildProfile

    suspend fun fetchActiveChildDashboard(): ChildProfileDashboard
}
