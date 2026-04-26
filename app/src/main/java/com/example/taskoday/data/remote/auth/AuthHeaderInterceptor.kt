package com.example.taskoday.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthHeaderInterceptor
    @Inject
    constructor(
        private val tokenStorage: TokenStorage,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val token = tokenStorage.getAccessToken()

            if (token.isNullOrBlank() || originalRequest.header(HEADER_AUTHORIZATION) != null) {
                return chain.proceed(originalRequest)
            }

            val authenticatedRequest =
                originalRequest
                    .newBuilder()
                    .header(HEADER_AUTHORIZATION, "$TOKEN_PREFIX $token")
                    .build()

            return chain.proceed(authenticatedRequest)
        }

        private companion object {
            const val HEADER_AUTHORIZATION = "Authorization"
            const val TOKEN_PREFIX = "Bearer"
        }
    }
