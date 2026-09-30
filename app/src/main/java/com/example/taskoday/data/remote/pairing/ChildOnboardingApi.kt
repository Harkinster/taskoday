package com.example.taskoday.data.remote.pairing

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.PairingCodeResponseDto
import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Uses the unauthenticated session client, never the parent token interceptor or authenticator. */
interface ChildOnboardingApi {
    @POST("auth/register-child")
    suspend fun registerChild(@Body payload: RegisterChildRequestDto): TokenResponseDto

    @POST("auth/login")
    suspend fun login(@Body payload: LoginRequestDto): TokenResponseDto

    @POST("pairing/generate-code")
    suspend fun generateCode(@Header("Authorization") authorization: String): ApiEnvelopeDto<PairingCodeResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body payload: RefreshTokenRequestDto)
}
