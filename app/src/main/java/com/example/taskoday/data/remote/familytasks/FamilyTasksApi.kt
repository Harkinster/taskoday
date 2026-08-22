package com.example.taskoday.data.remote.familytasks

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.FamilyTaskCreateRequestDto
import com.example.taskoday.data.remote.dto.FamilyTaskUpdateRequestDto
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FamilyTasksApi {
    @GET("families/{familyId}/tasks")
    suspend fun getTasks(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @GET("families/{familyId}/tasks/today")
    suspend fun getTodayTasks(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @GET("families/{familyId}/task-occurrences")
    suspend fun getTaskOccurrences(
        @Path("familyId") familyId: Long,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): ApiEnvelopeDto<JsonElement>

    @GET("families/{familyId}/members")
    suspend fun getFamilyMembers(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("families/{familyId}/tasks")
    suspend fun createTask(
        @Path("familyId") familyId: Long,
        @Body payload: FamilyTaskCreateRequestDto,
    ): ApiEnvelopeDto<JsonElement>

    @PATCH("family-tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: Long,
        @Body payload: FamilyTaskUpdateRequestDto,
    ): ApiEnvelopeDto<JsonElement>

    @DELETE("family-tasks/{taskId}")
    suspend fun deleteTask(
        @Path("taskId") taskId: Long,
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
