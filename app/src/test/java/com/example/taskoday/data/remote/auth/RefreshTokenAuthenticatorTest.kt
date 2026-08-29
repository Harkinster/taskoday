package com.example.taskoday.data.remote.auth

import com.example.taskoday.data.remote.dto.TokenResponseDto
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshTokenAuthenticatorTest {
    @Test
    fun `401 refresh success stores rotated pair and retries once`() {
        val storage = MemorySessionTokenStorage(accessToken = "access-a", refreshToken = "refresh-a")
        val client = FakeAuthSessionClient(success("access-b", "refresh-b"))
        val authenticator = authenticator(storage, client)

        val retry = authenticator.authenticate(null, unauthorizedResponse("access-a"))

        assertEquals("Bearer access-b", retry?.header(AUTHORIZATION_HEADER))
        assertEquals("access-b", storage.getSessionTokens()?.accessToken)
        assertEquals("refresh-b", storage.getSessionTokens()?.refreshToken)
        assertEquals(listOf("refresh-a"), client.refreshTokens)
        assertEquals(1, storage.sessionWriteCount)
    }

    @Test
    fun `refresh 401 clears both tokens`() {
        val storage = MemorySessionTokenStorage(accessToken = "access-a", refreshToken = "refresh-a")
        val client = FakeAuthSessionClient(AuthRefreshResult.HttpFailure(401))

        val retry = authenticator(storage, client).authenticate(null, unauthorizedResponse("access-a"))

        assertNull(retry)
        assertNull(storage.getSessionTokens())
    }

    @Test
    fun `401 without refresh clears legacy access session cleanly`() {
        val storage = MemorySessionTokenStorage(accessToken = "legacy-access", refreshToken = null)
        val client = FakeAuthSessionClient()

        val retry = authenticator(storage, client).authenticate(null, unauthorizedResponse("legacy-access"))

        assertNull(retry)
        assertNull(storage.getSessionTokens())
        assertEquals(emptyList<String>(), client.refreshTokens)
    }

    @Test
    fun `refresh endpoint 401 never invokes authenticator recursively`() {
        val storage = MemorySessionTokenStorage(accessToken = "access", refreshToken = "refresh")
        val client = FakeAuthSessionClient(success("unused-access", "unused-refresh"))
        val authenticator = authenticator(storage, client)
        val request =
            Request
                .Builder()
                .url("https://example.test/api/v1/auth/refresh")
                .header(AUTHORIZATION_HEADER, "Bearer access")
                .build()

        val retry = authenticator.authenticate(null, unauthorizedResponse(request))

        assertNull(retry)
        assertEquals(emptyList<String>(), client.refreshTokens)
        assertEquals("refresh", storage.getSessionTokens()?.refreshToken)
    }

    @Test
    fun `refresh 500 and 403 preserve tokens`() {
        listOf(500, 403).forEach { statusCode ->
            val storage = MemorySessionTokenStorage(accessToken = "access", refreshToken = "refresh")
            val client = FakeAuthSessionClient(AuthRefreshResult.HttpFailure(statusCode))

            val retry = authenticator(storage, client).authenticate(null, unauthorizedResponse("access"))

            assertNull(retry)
            assertEquals("access", storage.getSessionTokens()?.accessToken)
            assertEquals("refresh", storage.getSessionTokens()?.refreshToken)
        }
    }

    @Test
    fun `refresh network failure preserves tokens`() {
        val storage = MemorySessionTokenStorage(accessToken = "access", refreshToken = "refresh")
        val client = FakeAuthSessionClient(AuthRefreshResult.NetworkFailure)

        val retry = authenticator(storage, client).authenticate(null, unauthorizedResponse("access"))

        assertNull(retry)
        assertEquals("access", storage.getSessionTokens()?.accessToken)
        assertEquals("refresh", storage.getSessionTokens()?.refreshToken)
    }

    @Test
    fun `second 401 after retry does not refresh in a loop`() {
        val storage = MemorySessionTokenStorage(accessToken = "access-a", refreshToken = "refresh-a")
        val client = FakeAuthSessionClient(success("access-b", "refresh-b"))
        val authenticator = authenticator(storage, client)
        val firstResponse = unauthorizedResponse("access-a")
        val retryRequest = requireNotNull(authenticator.authenticate(null, firstResponse))

        val finalRetry =
            authenticator.authenticate(
                null,
                unauthorizedResponse(request = retryRequest, priorResponse = firstResponse),
            )

        assertNull(finalRetry)
        assertNull(storage.getSessionTokens())
        assertEquals(1, client.refreshTokens.size)
    }

    @Test
    fun `three simultaneous 401 rotate once and all reuse new access`() {
        val storage = MemorySessionTokenStorage(accessToken = "access-a", refreshToken = "refresh-a")
        val client = FakeAuthSessionClient(success("access-b", "refresh-b"), refreshDelayMillis = 80L)
        val authenticator = authenticator(storage, client)
        val executor = Executors.newFixedThreadPool(3)

        try {
            val retries =
                (1..3).map {
                    executor.submit<Request?> {
                        authenticator.authenticate(null, unauthorizedResponse("access-a"))
                    }
                }.map { it.get(3, TimeUnit.SECONDS) }

            assertEquals(1, client.refreshTokens.size)
            assertEquals(listOf("refresh-a"), client.refreshTokens)
            retries.forEach { request ->
                assertNotNull(request)
                assertEquals("Bearer access-b", request?.header(AUTHORIZATION_HEADER))
            }
            assertEquals("refresh-b", storage.getSessionTokens()?.refreshToken)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `next rotation uses latest refresh token never previous one`() {
        val storage = MemorySessionTokenStorage(accessToken = "access-a", refreshToken = "refresh-a")
        val client =
            FakeAuthSessionClient(
                success("access-b", "refresh-b"),
                success("access-c", "refresh-c"),
            )
        val authenticator = authenticator(storage, client)

        authenticator.authenticate(null, unauthorizedResponse("access-a"))
        authenticator.authenticate(null, unauthorizedResponse("access-b"))

        assertEquals(listOf("refresh-a", "refresh-b"), client.refreshTokens)
        assertEquals("access-c", storage.getSessionTokens()?.accessToken)
        assertEquals("refresh-c", storage.getSessionTokens()?.refreshToken)
    }

    private fun authenticator(
        storage: TokenStorage,
        client: AuthSessionClient,
    ): RefreshTokenAuthenticator = RefreshTokenAuthenticator(storage, client, SessionEventBus())

    private fun success(
        accessToken: String,
        refreshToken: String,
    ): AuthRefreshResult.Success =
        AuthRefreshResult.Success(
            TokenResponseDto(
                accessToken = accessToken,
                tokenType = "bearer",
                expiresIn = 7_200,
                role = "PARENT",
                refreshToken = refreshToken,
                refreshExpiresIn = 2_592_000,
            ),
        )
}

private class FakeAuthSessionClient(
    vararg refreshResults: AuthRefreshResult,
    private val refreshDelayMillis: Long = 0L,
) : AuthSessionClient {
    private val results = ArrayDeque(refreshResults.toList())
    val refreshTokens = mutableListOf<String>()

    override fun refresh(refreshToken: String): AuthRefreshResult {
        synchronized(this) {
            refreshTokens += refreshToken
        }
        if (refreshDelayMillis > 0L) Thread.sleep(refreshDelayMillis)
        return synchronized(this) { results.removeFirst() }
    }

    override fun logout(refreshToken: String) = Unit
}

private class MemorySessionTokenStorage(
    accessToken: String,
    refreshToken: String?,
) : TokenStorage {
    private var tokens: SessionTokens? = sessionTokens(accessToken, refreshToken)
    var sessionWriteCount: Int = 0
        private set

    @Synchronized
    override fun getSessionTokens(): SessionTokens? = tokens

    @Synchronized
    override fun saveSessionTokens(
        accessToken: String,
        refreshToken: String?,
        accessExpiresInSeconds: Int?,
        refreshExpiresInSeconds: Int?,
    ) {
        tokens = sessionTokens(accessToken, refreshToken)
        sessionWriteCount += 1
    }

    override fun getActiveChildId(): Long? = null

    override fun saveActiveChildId(childId: Long) = Unit

    override fun clearActiveChildId() = Unit

    override fun hasParentPin(): Boolean = false

    override fun saveParentPin(pin: String) = Unit

    override fun verifyParentPin(pin: String): Boolean = false

    @Synchronized
    override fun clear() {
        tokens = null
    }
}

private fun sessionTokens(
    accessToken: String,
    refreshToken: String?,
): SessionTokens =
    SessionTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessExpiresAtEpochSeconds = null,
        refreshExpiresAtEpochSeconds = null,
    )

private fun unauthorizedResponse(
    accessToken: String,
    priorResponse: Response? = null,
): Response =
    unauthorizedResponse(
        request =
            Request
                .Builder()
                .url("https://example.test/api/v1/family-tasks/today")
                .header(AUTHORIZATION_HEADER, "Bearer $accessToken")
                .build(),
        priorResponse = priorResponse,
    )

private fun unauthorizedResponse(
    request: Request,
    priorResponse: Response? = null,
): Response =
    Response
        .Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .apply { priorResponse?.let(::priorResponse) }
        .build()
