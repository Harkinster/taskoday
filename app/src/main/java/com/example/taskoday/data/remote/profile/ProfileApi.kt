package com.example.taskoday.data.remote.profile

import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.ChildStatsDto
import com.example.taskoday.data.remote.dto.XpHistoryItemDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ProfileApi {
    @GET("children/{childId}/profile")
    suspend fun getProfile(
        @Path("childId") childId: Long,
    ): ChildProfileResponseDto

    @GET("children/{childId}/xp-history")
    suspend fun getXpHistory(
        @Path("childId") childId: Long,
    ): List<XpHistoryItemDto>

    @GET("children/{childId}/stats")
    suspend fun getStats(
        @Path("childId") childId: Long,
    ): ChildStatsDto
}
