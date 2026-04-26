package com.example.taskoday.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class UnauthorizedInterceptor
    @Inject
    constructor(
        private val tokenStorage: TokenStorage,
        private val sessionEventBus: SessionEventBus,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val hadToken = !tokenStorage.getAccessToken().isNullOrBlank()

            if (hadToken && response.code == 401) {
                tokenStorage.clear()
                sessionEventBus.notifyUnauthorized()
            }

            return response
        }
    }
