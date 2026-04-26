package com.example.taskoday.data.remote.planning

import com.example.taskoday.data.remote.dto.CompletionToggleResponseDto
import com.example.taskoday.data.remote.dto.ChildMissionCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildQuestCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildRoutineCreateRequestDto
import com.example.taskoday.data.remote.dto.MissionCreateResponseDto
import com.example.taskoday.data.remote.dto.PlanningResponseDto
import com.example.taskoday.data.remote.dto.QuestCreateResponseDto
import com.example.taskoday.data.remote.dto.RoutineCreateResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface PlanningApi {
    @POST("children/{childId}/routines")
    suspend fun createRoutine(
        @Path("childId") childId: Long,
        @Body payload: ChildRoutineCreateRequestDto,
    ): RoutineCreateResponseDto

    @POST("children/{childId}/missions")
    suspend fun createMission(
        @Path("childId") childId: Long,
        @Body payload: ChildMissionCreateRequestDto,
    ): MissionCreateResponseDto

    @POST("children/{childId}/quests")
    suspend fun createQuest(
        @Path("childId") childId: Long,
        @Body payload: ChildQuestCreateRequestDto,
    ): QuestCreateResponseDto

    @GET("children/{childId}/planning")
    suspend fun getPlanning(
        @Path("childId") childId: Long,
        @Query("date") date: String,
    ): PlanningResponseDto

    @PATCH("planning/{itemType}/{itemId}/complete")
    suspend fun completeItem(
        @Path("itemType") itemType: String,
        @Path("itemId") itemId: Long,
        @Query("date") date: String,
    ): CompletionToggleResponseDto

    @PATCH("planning/{itemType}/{itemId}/uncomplete")
    suspend fun uncompleteItem(
        @Path("itemType") itemType: String,
        @Path("itemId") itemId: Long,
        @Query("date") date: String,
    ): CompletionToggleResponseDto
}
