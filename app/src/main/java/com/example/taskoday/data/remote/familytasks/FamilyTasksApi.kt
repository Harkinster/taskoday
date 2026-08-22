package com.example.taskoday.data.remote.familytasks

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.FamilyTaskCreateRequestDto
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FamilyTasksApi {
    @GET("families/{familyId}/tasks/today")
    suspend fun getTodayTasks(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @GET("families/{familyId}/children")
    suspend fun getFamilyChildren(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("families/{familyId}/tasks")
    suspend fun createTask(
        @Path("familyId") familyId: Long,
        @Body payload: FamilyTaskCreateRequestDto,
    ): ApiEnvelopeDto<JsonElement>

    @POST("task-occurrences/{occurrenceId}/complete")
    suspend fun completeOccurrence(
        @Path("occurrenceId") occurrenceId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("task-occurrences/{occurrenceId}/validate")
    suspend fun validateOccurrence(
        @Path("occurrenceId") occurrenceId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("task-occurrences/{occurrenceId}/reopen")
    suspend fun reopenOccurrence(
        @Path("occurrenceId") occurrenceId: Long,
    ): ApiEnvelopeDto<JsonElement>
}
