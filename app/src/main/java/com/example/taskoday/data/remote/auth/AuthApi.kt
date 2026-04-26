package com.example.taskoday.data.remote.auth

import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.MeResponseDto
import com.example.taskoday.data.remote.dto.RegisterParentRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register-parent")
    suspend fun registerParent(
        @Body payload: RegisterParentRequestDto,
    ): TokenResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body payload: LoginRequestDto,
    ): TokenResponseDto

    @GET("auth/me")
    suspend fun me(): MeResponseDto
}
