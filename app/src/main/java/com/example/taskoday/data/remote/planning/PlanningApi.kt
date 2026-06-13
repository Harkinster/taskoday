package com.example.taskoday.data.remote.planning

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ChildMissionCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildQuestCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildRoutineCreateRequestDto
import com.example.taskoday.data.remote.dto.MissionCreateResponseDto
import com.example.taskoday.data.remote.dto.QuestCreateResponseDto
import com.example.taskoday.data.remote.dto.RemoteCompletionResponseDto
import com.example.taskoday.data.remote.dto.RoutineCreateResponseDto
import com.example.taskoday.data.remote.dto.RoutineItemDto
import com.example.taskoday.data.remote.dto.RoutineUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PlanningApi {
    @POST("children/{childId}/routines")
    suspend fun createRoutine(
        @Path("childId") childId: Long,
        @Body payload: ChildRoutineCreateRequestDto,
    ): ApiEnvelopeDto<RoutineCreateResponseDto>

    @POST("children/{childId}/missions")
    suspend fun createMission(
        @Path("childId") childId: Long,
        @Body payload: ChildMissionCreateRequestDto,
    ): ApiEnvelopeDto<MissionCreateResponseDto>

    @POST("children/{childId}/quests")
    suspend fun createQuest(
        @Path("childId") childId: Long,
        @Body payload: ChildQuestCreateRequestDto,
    ): ApiEnvelopeDto<QuestCreateResponseDto>

    @PATCH("routines/{routineId}")
    suspend fun updateRoutine(
        @Path("routineId") routineId: Long,
        @Body payload: RoutineUpdateRequestDto,
    ): ApiEnvelopeDto<RoutineItemDto>

    @DELETE("routines/{routineId}")
    suspend fun deleteRoutine(
        @Path("routineId") routineId: Long,
    )

    @POST("routines/{routineId}/complete")
    suspend fun completeRoutine(
        @Path("routineId") routineId: Long,
    ): ApiEnvelopeDto<RemoteCompletionResponseDto>

    @POST("routines/{routineId}/uncomplete")
    suspend fun uncompleteRoutine(
        @Path("routineId") routineId: Long,
    ): ApiEnvelopeDto<RemoteCompletionResponseDto>

    @POST("missions/{missionId}/complete")
    suspend fun completeMission(
        @Path("missionId") missionId: Long,
    ): ApiEnvelopeDto<RemoteCompletionResponseDto>

    @POST("quests/{questId}/complete")
    suspend fun completeQuest(
        @Path("questId") questId: Long,
    ): ApiEnvelopeDto<RemoteCompletionResponseDto>
}
