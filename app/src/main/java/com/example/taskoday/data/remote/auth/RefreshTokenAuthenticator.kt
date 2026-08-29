package com.example.taskoday.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class RefreshTokenAuthenticator
    @Inject
    constructor(
        private val tokenStorage: TokenStorage,
        private val authSessionClient: AuthSessionClient,
        private val sessionEventBus: SessionEventBus,
    ) : Authenticator {
        private val refreshLock = Any()

        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.request.isPublicAuthEndpoint()) return null

            val failedAccessToken = response.request.bearerToken() ?: return null
            if (response.responseCount() > MAX_AUTH_ATTEMPTS) {
                invalidateSessionIfCurrent(failedAccessToken)
                return null
            }

            return synchronized(refreshLock) {
                val storedTokens = tokenStorage.getSessionTokens()
                if (storedTokens == null) {
                    notifyInvalidSession()
                    return@synchronized null
                }

                if (storedTokens.accessToken != failedAccessToken) {
                    return@synchronized response.request.withBearerToken(storedTokens.accessToken)
                }

                val refreshToken = storedTokens.refreshToken
                if (refreshToken.isNullOrBlank()) {
                    invalidateSessionIfCurrent(failedAccessToken)
                    return@synchronized null
                }

                when (val refreshResult = authSessionClient.refresh(refreshToken)) {
                    is AuthRefreshResult.Success -> {
                        val refreshed = refreshResult.response
                        val rotatedRefreshToken = refreshed.refreshToken
                        if (refreshed.accessToken.isBlank() || rotatedRefreshToken.isNullOrBlank()) {
                            return@synchronized null
                        }
                        tokenStorage.saveSessionTokens(
                            accessToken = refreshed.accessToken,
                            refreshToken = rotatedRefreshToken,
                            accessExpiresInSeconds = refreshed.expiresIn,
                            refreshExpiresInSeconds = refreshed.refreshExpiresIn,
                        )
                        response.request.withBearerToken(refreshed.accessToken)
                    }

                    is AuthRefreshResult.HttpFailure -> {
                        if (refreshResult.statusCode == 401) {
                            invalidateSessionIfCurrent(failedAccessToken)
                        }
                        null
                    }

                    AuthRefreshResult.NetworkFailure -> null
                }
            }
        }

        private fun invalidateSessionIfCurrent(failedAccessToken: String) {
            if (tokenStorage.getAccessToken() == failedAccessToken) {
                tokenStorage.clear()
                notifyInvalidSession()
            }
        }

        private fun notifyInvalidSession() {
            sessionEventBus.notifyUnauthorized()
        }

        private companion object {
            const val MAX_AUTH_ATTEMPTS = 1
        }
    }

private fun Response.responseCount(): Int {
    var count = 1
    var current = priorResponse
    while (current != null) {
        count += 1
        current = current.priorResponse
    }
    return count
}
