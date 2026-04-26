package com.example.taskoday.data.remote.quests

import com.example.taskoday.data.remote.dto.QuestCreateRequestDto
import com.example.taskoday.data.remote.dto.QuestItemDto
import com.example.taskoday.data.remote.dto.QuestUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface QuestsApi {
    @GET("children/{childId}/quests")
    suspend fun getQuests(
        @Path("childId") childId: Long,
    ): List<QuestItemDto>

    @POST("children/{childId}/quests")
    suspend fun createQuest(
        @Path("childId") childId: Long,
        @Body payload: QuestCreateRequestDto,
    ): QuestItemDto

    @PATCH("quests/{questId}")
    suspend fun updateQuest(
        @Path("questId") questId: Long,
        @Body payload: QuestUpdateRequestDto,
    ): QuestItemDto

    @DELETE("quests/{questId}")
    suspend fun deleteQuest(
        @Path("questId") questId: Long,
    )

    @POST("quests/{questId}/complete")
    suspend fun completeQuest(
        @Path("questId") questId: Long,
    )
}
