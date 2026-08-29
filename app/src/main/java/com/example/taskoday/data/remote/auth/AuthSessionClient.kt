package com.example.taskoday.data.remote.auth

import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface AuthSessionClient {
    fun refresh(refreshToken: String): AuthRefreshResult

    fun logout(refreshToken: String)
}

sealed interface AuthRefreshResult {
    data class Success(
        val response: TokenResponseDto,
    ) : AuthRefreshResult

    data class HttpFailure(
        val statusCode: Int,
    ) : AuthRefreshResult

    data object NetworkFailure : AuthRefreshResult
}

@Singleton
class RetrofitAuthSessionClient
    @Inject
    constructor(
        private val api: AuthSessionApi,
    ) : AuthSessionClient {
        override fun refresh(refreshToken: String): AuthRefreshResult =
            try {
                val response = api.refresh(RefreshTokenRequestDto(refreshToken)).execute()
                val body = response.body()
                val result =
                    if (response.isSuccessful && body != null) {
                        AuthRefreshResult.Success(body)
                    } else {
                        AuthRefreshResult.HttpFailure(response.code())
                    }
                response.errorBody()?.close()
                result
            } catch (_: IOException) {
                AuthRefreshResult.NetworkFailure
            }

        override fun logout(refreshToken: String) {
            runCatching {
                api.logout(RefreshTokenRequestDto(refreshToken)).enqueue(
                    object : Callback<Unit> {
                        override fun onResponse(
                            call: Call<Unit>,
                            response: Response<Unit>,
                        ) {
                            response.raw().close()
                        }

                        override fun onFailure(
                            call: Call<Unit>,
                            throwable: Throwable,
                        ) = Unit
                    },
                )
            }
        }
    }
