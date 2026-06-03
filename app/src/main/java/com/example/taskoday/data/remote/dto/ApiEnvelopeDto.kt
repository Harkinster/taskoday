package com.example.taskoday.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiEnvelopeDto<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T,
    @SerializedName("message")
    val message: String? = null,
)
