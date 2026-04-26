package com.example.taskoday.data.remote.missions

import com.example.taskoday.data.remote.dto.MissionCreateRequestDto
import com.example.taskoday.data.remote.dto.MissionItemDto
import com.example.taskoday.data.remote.dto.MissionUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface MissionsApi {
    @GET("children/{childId}/missions")
    suspend fun getMissions(
        @Path("childId") childId: Long,
    ): List<MissionItemDto>

    @POST("children/{childId}/missions")
    suspend fun createMission(
        @Path("childId") childId: Long,
        @Body payload: MissionCreateRequestDto,
    ): MissionItemDto

    @PATCH("missions/{missionId}")
    suspend fun updateMission(
        @Path("missionId") missionId: Long,
        @Body payload: MissionUpdateRequestDto,
    ): MissionItemDto

    @DELETE("missions/{missionId}")
    suspend fun deleteMission(
        @Path("missionId") missionId: Long,
    )

    @POST("missions/{missionId}/complete")
    suspend fun completeMission(
        @Path("missionId") missionId: Long,
    )
}
