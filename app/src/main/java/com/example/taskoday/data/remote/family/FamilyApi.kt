package com.example.taskoday.data.remote.family

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.CreateFamilyRequestDto
import com.example.taskoday.data.remote.dto.FamilySummaryDto
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface FamilyApi {
    @GET("families/me")
    suspend fun getMyFamilies(): ApiEnvelopeDto<List<FamilySummaryDto>>

    @POST("families")
    suspend fun createFamily(
        @Body payload: CreateFamilyRequestDto,
    ): ApiEnvelopeDto<FamilySummaryDto>

    @GET("families/{familyId}/members")
    suspend fun getFamilyMembers(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("families/{familyId}/parent-invites")
    suspend fun createParentInvite(
        @Path("familyId") familyId: Long,
    ): ApiEnvelopeDto<JsonElement>

    @POST("family-invites/{code}/accept")
    suspend fun acceptParentInvite(
        @Path("code") code: String,
    ): ApiEnvelopeDto<JsonElement>
}
