package com.example.taskoday.data.remote.auth

import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthSessionApi {
    @POST("auth/refresh")
    fun refresh(
        @Body payload: RefreshTokenRequestDto,
    ): Call<TokenResponseDto>

    @POST("auth/logout")
    fun logout(
        @Body payload: RefreshTokenRequestDto,
    ): Call<Unit>
}
