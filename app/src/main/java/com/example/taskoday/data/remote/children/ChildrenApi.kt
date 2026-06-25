package com.example.taskoday.data.remote.children

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ChildCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.ChildUpdateRequestDto
import com.example.taskoday.data.remote.dto.ChildUpdateResponseDto
import com.example.taskoday.data.remote.dto.RoutineItemDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

interface ChildrenApi {
    @GET("children")
    suspend fun getChildren(): ApiEnvelopeDto<List<ChildResponseDto>>

    @POST("children")
    suspend fun createChild(
        @Body payload: ChildCreateRequestDto,
    ): ApiEnvelopeDto<ChildResponseDto>

    @GET("children/{childId}")
    suspend fun getChild(
        @Path("childId") childId: Long,
    ): ApiEnvelopeDto<ChildResponseDto>

    @GET("children/{childId}/profile")
    suspend fun getProfile(
        @Path("childId") childId: Long,
    ): ApiEnvelopeDto<ChildProfileResponseDto>

    @GET("children/{childId}/routines")
    suspend fun getRoutines(
        @Path("childId") childId: Long,
    ): ApiEnvelopeDto<List<RoutineItemDto>>

    @PATCH("children/{childId}")
    suspend fun updateChild(
        @Path("childId") childId: Long,
        @Body payload: ChildUpdateRequestDto,
    ): ApiEnvelopeDto<ChildUpdateResponseDto>
}
