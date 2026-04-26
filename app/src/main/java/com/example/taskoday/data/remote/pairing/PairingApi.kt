package com.example.taskoday.data.remote.pairing

import com.example.taskoday.data.remote.dto.AttachChildRequestDto
import com.example.taskoday.data.remote.dto.PairingCodeResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PairingApi {
    @POST("pairing/generate-code")
    suspend fun generateCode(): PairingCodeResponseDto

    @GET("pairing/my-code")
    suspend fun getMyCode(): PairingCodeResponseDto

    @POST("pairing/attach-child")
    suspend fun attachChild(
        @Body payload: AttachChildRequestDto,
    )
}
