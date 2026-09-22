package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.AuthRefreshResult
import com.example.taskoday.data.remote.auth.AuthSessionClient
import com.example.taskoday.data.remote.auth.SessionTokens
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ChildCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildProfileResponseDto
import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.ChildUpdateRequestDto
import com.example.taskoday.data.remote.dto.ChildUpdateResponseDto
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.MeResponseDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.RegisterParentRequestDto
import com.example.taskoday.data.remote.dto.RoutineItemDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryImplTest {
    @Test
    fun `register parent sends simple identity without family`() =
        runBlocking {
            val authApi = FakeAuthApi()
            val repository = AuthRepositoryImpl(authApi, FakeChildrenApi(), MemoryTokenStorage(), FakeAuthSessionClient())

            repository.registerParent(
                displayName = " Test ",
                birthDate = "1990-01-01",
                email = "parent@example.test",
                password = "password123",
            )

            assertEquals("Test", authApi.lastRegisterParentPayload?.displayName)
            assertEquals("1990-01-01", authApi.lastRegisterParentPayload?.birthDate)
        }

    @Test
    fun `login replaces token and clears active child`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "old-token", activeChildId = 99L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, FakeAuthSessionClient())

            repository.login(" parent@example.com ", "password123")

            assertEquals("new-token", repository.getAccessToken())
            assertEquals("new-refresh", storage.getSessionTokens()?.refreshToken)
            assertNull(storage.getActiveChildId())
        }

    @Test
    fun `register child stores access and refresh together`() =
        runBlocking {
            val storage = MemoryTokenStorage()
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, FakeAuthSessionClient())

            repository.registerChild("child@example.com", "password123", "Child")

            assertEquals("new-token", storage.getSessionTokens()?.accessToken)
            assertEquals("new-refresh", storage.getSessionTokens()?.refreshToken)
            assertEquals(1, storage.sessionWriteCount)
        }

    @Test
    fun `active child is read from storage until refresh is requested`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 77L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, FakeAuthSessionClient())

            assertEquals(77L, repository.getActiveChildId())
            assertEquals(42L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(42L, storage.getActiveChildId())
        }

    @Test
    fun `active family is persisted validated and not chosen arbitrarily`() =
        runBlocking {
            val stored = MemoryTokenStorage(accessToken = "token", activeFamilyId = 9L)
            val multiple = AuthRepositoryImpl(FakeAuthApi(listOf(4L, 9L)), FakeChildrenApi(), stored, FakeAuthSessionClient())
            assertEquals(9L, multiple.getActiveFamilyId())

            val withoutChoice = AuthRepositoryImpl(FakeAuthApi(listOf(4L, 9L)), FakeChildrenApi(), MemoryTokenStorage(), FakeAuthSessionClient())
            assertNull(withoutChoice.getActiveFamilyId())

            val singleStorage = MemoryTokenStorage()
            val single = AuthRepositoryImpl(FakeAuthApi(listOf(4L)), FakeChildrenApi(), singleStorage, FakeAuthSessionClient())
            assertEquals(4L, single.getActiveFamilyId())
            assertEquals(4L, singleStorage.getActiveFamilyId())
        }

    @Test
    fun `refresh automatically selects the only accessible child`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token")
            val onlyChild = child(id = 18L, email = "only@example.com", displayName = "Only")
            val repository =
                AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(listOf(onlyChild)), storage, FakeAuthSessionClient())

            assertEquals(18L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(18L, storage.getActiveChildId())
        }

    @Test
    fun `refresh preserves stored active child when it is still accessible`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 99L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, FakeAuthSessionClient())

            assertEquals(99L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(99L, storage.getActiveChildId())
        }

    @Test
    fun `refresh clears stale active child when no child is accessible`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 99L)
            val repository =
                AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(emptyList()), storage, FakeAuthSessionClient())

            assertNull(repository.getActiveChildId(forceRefresh = true))
            assertNull(storage.getActiveChildId())
        }

    @Test
    fun `clear session removes token and active child`() {
        val storage = MemoryTokenStorage(accessToken = "token", refreshToken = "refresh", activeChildId = 42L)
        val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, FakeAuthSessionClient())

        repository.clearSession()

        assertNull(repository.getAccessToken())
        assertNull(storage.getSessionTokens())
        assertNull(storage.getActiveChildId())
    }

    @Test
    fun `old access-only session remains readable`() {
        val storage = MemoryTokenStorage(accessToken = "legacy-token")

        assertEquals("legacy-token", storage.getSessionTokens()?.accessToken)
        assertNull(storage.getSessionTokens()?.refreshToken)
    }

    @Test
    fun `logout revokes current refresh then clears local session`() {
        val storage = MemoryTokenStorage(accessToken = "access", refreshToken = "current-refresh")
        val sessionClient = FakeAuthSessionClient()
        val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, sessionClient)

        repository.logout()

        assertEquals(listOf("current-refresh"), sessionClient.logoutTokens)
        assertNull(storage.getSessionTokens())
    }

    @Test
    fun `logout without refresh clears locally without backend call`() {
        val storage = MemoryTokenStorage(accessToken = "legacy-token")
        val sessionClient = FakeAuthSessionClient()
        val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, sessionClient)

        repository.logout()

        assertEquals(emptyList<String>(), sessionClient.logoutTokens)
        assertNull(storage.getSessionTokens())
    }

    @Test
    fun `logout clears locally when backend call throws`() {
        val storage = MemoryTokenStorage(accessToken = "access", refreshToken = "refresh")
        val sessionClient = FakeAuthSessionClient(throwOnLogout = true)
        val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage, sessionClient)

        repository.logout()

        assertNull(storage.getSessionTokens())
    }
}

private class MemoryTokenStorage(
    private var accessToken: String? = null,
    private var refreshToken: String? = null,
    private var activeChildId: Long? = null,
    private var activeFamilyId: Long? = null,
    private var parentPin: String? = null,
) : TokenStorage {
    var sessionWriteCount: Int = 0
        private set

    override fun getSessionTokens(): SessionTokens? =
        accessToken?.let {
            SessionTokens(
                accessToken = it,
                refreshToken = refreshToken,
                accessExpiresAtEpochSeconds = null,
                refreshExpiresAtEpochSeconds = null,
            )
        }

    override fun saveSessionTokens(
        accessToken: String,
        refreshToken: String?,
        accessExpiresInSeconds: Int?,
        refreshExpiresInSeconds: Int?,
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        sessionWriteCount += 1
    }

    override fun getActiveChildId(): Long? = activeChildId

    override fun saveActiveChildId(childId: Long) {
        activeChildId = childId
    }

    override fun clearActiveChildId() {
        activeChildId = null
    }

    override fun getActiveFamilyId(): Long? = activeFamilyId

    override fun saveActiveFamilyId(familyId: Long) {
        activeFamilyId = familyId
    }

    override fun clearActiveFamilyId() {
        activeFamilyId = null
    }

    override fun hasParentPin(): Boolean = !parentPin.isNullOrBlank()

    override fun saveParentPin(pin: String) {
        parentPin = pin
    }

    override fun verifyParentPin(pin: String): Boolean = parentPin == pin

    override fun clear() {
        accessToken = null
        refreshToken = null
        activeChildId = null
        activeFamilyId = null
    }
}

private class FakeAuthApi(
    private val familyIds: List<Long> = listOf(7L),
) : AuthApi {
    var lastRegisterParentPayload: RegisterParentRequestDto? = null

    override suspend fun registerParent(payload: RegisterParentRequestDto): TokenResponseDto {
        lastRegisterParentPayload = payload
        return tokenResponse()
    }

    override suspend fun registerChild(payload: RegisterChildRequestDto): TokenResponseDto = tokenResponse()

    override suspend fun login(payload: LoginRequestDto): TokenResponseDto = tokenResponse()

    override suspend fun me(): MeResponseDto =
        MeResponseDto(
            id = 1L,
            email = "parent@example.com",
            role = "PARENT",
            isActive = true,
            familyIds = familyIds,
        )

    private fun tokenResponse(): TokenResponseDto =
        TokenResponseDto(
            accessToken = "new-token",
            tokenType = "bearer",
            expiresIn = 3600,
            role = "PARENT",
            refreshToken = "new-refresh",
            refreshExpiresIn = 2_592_000,
        )
}

private class FakeAuthSessionClient(
    private val throwOnLogout: Boolean = false,
) : AuthSessionClient {
    val logoutTokens = mutableListOf<String>()

    override fun refresh(refreshToken: String): AuthRefreshResult = error("Not used")

    override fun logout(refreshToken: String) {
        if (throwOnLogout) error("offline")
        logoutTokens += refreshToken
    }
}

private class FakeChildrenApi(
    private val children: List<ChildResponseDto> = defaultChildren(),
) : ChildrenApi {
    override suspend fun getChildren(familyId: Long?): ApiEnvelopeDto<List<ChildResponseDto>> =
        ApiEnvelopeDto(
            success = true,
            data = children,
        )

    override suspend fun createChild(payload: ChildCreateRequestDto): ApiEnvelopeDto<ChildResponseDto> = error("Not used")

    override suspend fun getChild(childId: Long): ApiEnvelopeDto<ChildResponseDto> = error("Not used")

    override suspend fun getProfile(childId: Long): ApiEnvelopeDto<ChildProfileResponseDto> = error("Not used")

    override suspend fun getRoutines(childId: Long): ApiEnvelopeDto<List<RoutineItemDto>> = error("Not used")

    override suspend fun updateChild(
        childId: Long,
        payload: ChildUpdateRequestDto,
    ): ApiEnvelopeDto<ChildUpdateResponseDto> = error("Not used")
}

private fun defaultChildren(): List<ChildResponseDto> =
    listOf(
        child(id = 42L, email = "child@example.com", displayName = "Child"),
        child(id = 99L, email = "second-child@example.com", displayName = "Second Child"),
    )

private fun child(
    id: Long,
    email: String,
    displayName: String,
): ChildResponseDto =
    ChildResponseDto(
        id = id,
        email = email,
        displayName = displayName,
    )
