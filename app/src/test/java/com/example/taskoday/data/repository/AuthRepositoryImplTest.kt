package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
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
    fun `login replaces token and clears active child`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "old-token", activeChildId = 99L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage)

            repository.login(" parent@example.com ", "password123")

            assertEquals("new-token", repository.getAccessToken())
            assertNull(storage.getActiveChildId())
        }

    @Test
    fun `active child is read from storage until refresh is requested`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 77L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage)

            assertEquals(77L, repository.getActiveChildId())
            assertEquals(42L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(42L, storage.getActiveChildId())
        }

    @Test
    fun `refresh automatically selects the only accessible child`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token")
            val onlyChild = child(id = 18L, email = "only@example.com", displayName = "Only")
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(listOf(onlyChild)), storage)

            assertEquals(18L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(18L, storage.getActiveChildId())
        }

    @Test
    fun `refresh preserves stored active child when it is still accessible`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 99L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage)

            assertEquals(99L, repository.getActiveChildId(forceRefresh = true))
            assertEquals(99L, storage.getActiveChildId())
        }

    @Test
    fun `refresh clears stale active child when no child is accessible`() =
        runBlocking {
            val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 99L)
            val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(emptyList()), storage)

            assertNull(repository.getActiveChildId(forceRefresh = true))
            assertNull(storage.getActiveChildId())
        }

    @Test
    fun `clear session removes token and active child`() {
        val storage = MemoryTokenStorage(accessToken = "token", activeChildId = 42L)
        val repository = AuthRepositoryImpl(FakeAuthApi(), FakeChildrenApi(), storage)

        repository.clearSession()

        assertNull(repository.getAccessToken())
        assertNull(storage.getActiveChildId())
    }
}

private class MemoryTokenStorage(
    private var accessToken: String? = null,
    private var activeChildId: Long? = null,
) : TokenStorage {
    override fun getAccessToken(): String? = accessToken

    override fun saveAccessToken(token: String) {
        accessToken = token
    }

    override fun getActiveChildId(): Long? = activeChildId

    override fun saveActiveChildId(childId: Long) {
        activeChildId = childId
    }

    override fun clearActiveChildId() {
        activeChildId = null
    }

    override fun clear() {
        accessToken = null
        activeChildId = null
    }
}

private class FakeAuthApi : AuthApi {
    override suspend fun registerParent(payload: RegisterParentRequestDto): TokenResponseDto = tokenResponse()

    override suspend fun registerChild(payload: RegisterChildRequestDto): TokenResponseDto = tokenResponse()

    override suspend fun login(payload: LoginRequestDto): TokenResponseDto = tokenResponse()

    override suspend fun me(): MeResponseDto =
        MeResponseDto(
            id = 1L,
            email = "parent@example.com",
            role = "PARENT",
            isActive = true,
            familyIds = listOf(7L),
        )

    private fun tokenResponse(): TokenResponseDto =
        TokenResponseDto(
            accessToken = "new-token",
            tokenType = "bearer",
            expiresIn = 3600,
            role = "PARENT",
        )
}

private class FakeChildrenApi(
    private val children: List<ChildResponseDto> = defaultChildren(),
) : ChildrenApi {
    override suspend fun getChildren(): ApiEnvelopeDto<List<ChildResponseDto>> =
        ApiEnvelopeDto(
            success = true,
            data = children,
        )

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
