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

            if (
                token.isNullOrBlank() ||
                originalRequest.header(AUTHORIZATION_HEADER) != null ||
                originalRequest.isPublicAuthEndpoint()
            ) {
                return chain.proceed(originalRequest)
            }

            return chain.proceed(originalRequest.withBearerToken(token))
        }
    }

internal const val AUTHORIZATION_HEADER = "Authorization"
private const val TOKEN_PREFIX = "Bearer"

internal fun okhttp3.Request.withBearerToken(token: String): okhttp3.Request =
    newBuilder()
        .header(AUTHORIZATION_HEADER, "$TOKEN_PREFIX $token")
        .build()

internal fun okhttp3.Request.bearerToken(): String? =
    header(AUTHORIZATION_HEADER)
        ?.takeIf { it.startsWith("$TOKEN_PREFIX ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.takeIf { it.isNotBlank() }

internal fun okhttp3.Request.isPublicAuthEndpoint(): Boolean {
    val path = url.encodedPath.trimEnd('/')
    return PUBLIC_AUTH_PATH_SUFFIXES.any(path::endsWith)
}

private val PUBLIC_AUTH_PATH_SUFFIXES =
    listOf(
        "/auth/login",
        "/auth/register-parent",
        "/auth/register-child",
        "/auth/refresh",
        "/auth/logout",
    )
