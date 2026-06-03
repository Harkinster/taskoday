package com.example.taskoday.data.remote.planning

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ChildMissionCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildQuestCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildRoutineCreateRequestDto
import com.example.taskoday.data.remote.dto.MissionCreateResponseDto
import com.example.taskoday.data.remote.dto.QuestCreateResponseDto
import com.example.taskoday.data.remote.dto.RemoteCompletionResponseDto
import com.example.taskoday.data.remote.dto.RoutineCreateResponseDto
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.POST

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
