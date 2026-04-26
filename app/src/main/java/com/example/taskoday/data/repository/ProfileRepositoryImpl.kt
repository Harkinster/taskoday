package com.example.taskoday.data.repository

import android.util.Log
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.ChildStatsDto
import com.example.taskoday.data.remote.dto.XpHistoryItemDto
import com.example.taskoday.data.remote.profile.ProfileApi
import com.example.taskoday.domain.model.ChildProfile
import com.example.taskoday.domain.model.ChildProfileDashboard
import com.example.taskoday.domain.model.ChildStats
import com.example.taskoday.domain.model.XpHistoryEntry
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class ProfileRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val childrenApi: ChildrenApi,
        private val profileApi: ProfileApi,
    ) : ProfileRepository {
        override suspend fun fetchActiveChildProfile(): ChildProfile = fetchActiveChildDashboard().profile

        override suspend fun fetchActiveChildDashboard(): ChildProfileDashboard {
            val childId =
                authRepository.getActiveChildId(forceRefresh = true)
                    ?: throw IllegalStateException("Aucun enfant actif disponible.")

            val profile = fetchProfile(childId)

            val stats =
                runCatching { profileApi.getStats(childId).toDomain() }
                    .getOrElse { error ->
                        Log.w(TAG, "Stats indisponibles, fallback valeur locale", error)
                        ChildStats()
                    }

            val xpHistory =
                runCatching { profileApi.getXpHistory(childId).mapNotNull { it.toDomainOrNull() } }
                    .getOrElse { error ->
                        Log.w(TAG, "Historique XP indisponible, fallback vide", error)
                        emptyList()
                    }

            return ChildProfileDashboard(
                profile = profile,
                stats = stats,
                xpHistory = xpHistory,
            )
        }

        private suspend fun fetchProfile(childId: Long): ChildProfile {
            val profileDto =
                runCatching { profileApi.getProfile(childId) }
                    .recoverCatching { throwable ->
                        if (throwable is HttpException && throwable.code() == 404) {
                            childrenApi.getChild(childId).toProfileDto()
                        } else {
                            throw throwable
                        }
                    }.recoverCatching { throwable ->
                        if (throwable is HttpException && throwable.code() == 403) {
                            childrenApi.getChild(childId).toProfileDto()
                        } else {
                            throw throwable
                        }
                    }.getOrThrow()

            return profileDto.toDomain()
        }

        private companion object {
            const val TAG = "ProfileRepository"
        }
    }

private fun ChildProfileResponseDto.toDomain(): ChildProfile =
    ChildProfile(
        id = id,
        familyId = familyId,
        userId = userId,
        email = email,
        displayName = displayName,
        birthDate = birthDate,
        role = role,
    )

private fun ChildResponseDto.toProfileDto(): ChildProfileResponseDto =
    ChildProfileResponseDto(
        id = id,
        familyId = familyId,
        userId = userId,
        email = email,
        displayName = displayName,
        birthDate = birthDate,
        role = role,
    )

private fun ChildStatsDto.toDomain(): ChildStats =
    ChildStats(
        totalXp = totalXp ?: 0,
        missionsCompleted = missionsCompleted ?: 0,
        missionsTotal = missionsTotal ?: 0,
        questsCompleted = questsCompleted ?: 0,
        questsTotal = questsTotal ?: 0,
        streakDays = streakDays ?: 0,
        successRatePercent = successRatePercent ?: 0,
    )

private fun XpHistoryItemDto.toDomainOrNull(): XpHistoryEntry? {
    val resolvedAmount = amount ?: xp ?: return null
    val resolvedDate = date ?: "Date inconnue"
    val resolvedReason = reason ?: description
    return XpHistoryEntry(
        dateLabel = resolvedDate,
        amount = resolvedAmount,
        reason = resolvedReason,
    )
}
