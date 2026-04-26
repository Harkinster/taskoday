package com.example.taskoday.data.remote.children

import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.RoutineItemDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ChildrenApi {
    @GET("children")
    suspend fun getChildren(): List<ChildResponseDto>

    @GET("children/{childId}")
    suspend fun getChild(
        @Path("childId") childId: Long,
    ): ChildResponseDto

    @GET("children/{childId}/profile")
    suspend fun getProfile(
        @Path("childId") childId: Long,
    ): ChildProfileResponseDto

    @GET("children/{childId}/routines")
    suspend fun getRoutines(
        @Path("childId") childId: Long,
    ): List<RoutineItemDto>
}
