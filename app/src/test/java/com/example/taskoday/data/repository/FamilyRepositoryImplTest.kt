package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.CreateFamilyRequestDto
import com.example.taskoday.data.remote.dto.FamilySummaryDto
import com.example.taskoday.data.remote.family.FamilyApi
import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyRepositoryImplTest {
    @Test
    fun `fetch members uses active family and filters inactive members`() =
        runBlocking {
            val api = FakeFamilyApi()
            val repository = FamilyRepositoryImpl(FakeAuthRepository(familyIds = listOf(4L)), api, Gson())

            val members = repository.fetchMembers().getOrThrow()

            assertEquals(4L, api.lastMembersFamilyId)
            assertEquals(listOf("Parent Test", "Enfant Test"), members.map { member -> member.displayName })
        }

    @Test
    fun `create parent invite maps code and expiration`() =
        runBlocking {
            val repository = FamilyRepositoryImpl(FakeAuthRepository(familyIds = listOf(4L)), FakeFamilyApi(), Gson())

            val invite = repository.createParentInvite().getOrThrow()

            assertEquals("AbC-123", invite.code)
            assertEquals("2026-08-25T20:00:00Z", invite.expiresAt)
        }

    @Test
    fun `accept invite preserves code case and refreshes me`() =
        runBlocking {
            val authRepository = FakeAuthRepository(familyIds = listOf(9L))
            val api = FakeFamilyApi()
            val repository = FamilyRepositoryImpl(authRepository, api, Gson())

            val user = repository.acceptParentInvite("  AbC-123  ").getOrThrow()

            assertEquals("AbC-123", api.acceptedCode)
            assertTrue(authRepository.fetchMeCalls >= 1)
            assertEquals(listOf(9L), user.familyIds)
        }
}

private class FakeFamilyApi : FamilyApi {
    var lastMembersFamilyId: Long? = null
    var acceptedCode: String? = null

    override suspend fun getMyFamilies(): ApiEnvelopeDto<List<FamilySummaryDto>> =
        ApiEnvelopeDto(success = true, data = listOf(FamilySummaryDto(4L, "Famille Test")))

    override suspend fun createFamily(payload: CreateFamilyRequestDto): ApiEnvelopeDto<FamilySummaryDto> =
        ApiEnvelopeDto(success = true, data = FamilySummaryDto(5L, payload.name))

    override suspend fun getFamilyMembers(familyId: Long): ApiEnvelopeDto<JsonElement> {
        lastMembersFamilyId = familyId
        return envelope(
            """
            {
              "members": [
                {"user_id": 1, "display_name": "Parent Test", "role": "PARENT", "email": "parent@example.test", "is_active": true},
                {"user_id": 2, "display_name": "Enfant Test", "role": "CHILD", "email": "child@example.test", "is_active": true},
                {"user_id": 3, "display_name": "Inactive", "role": "PARENT", "email": "inactive@example.test", "is_active": false}
              ]
            }
            """.trimIndent(),
        )
    }

    override suspend fun createParentInvite(familyId: Long): ApiEnvelopeDto<JsonElement> =
        envelope(
            """
            {
              "invite": {
                "code": "AbC-123",
                "expires_at": "2026-08-25T20:00:00Z"
              }
            }
            """.trimIndent(),
        )

    override suspend fun acceptParentInvite(code: String): ApiEnvelopeDto<JsonElement> {
        acceptedCode = code
        return envelope("""{"accepted": true}""")
    }

    private fun envelope(json: String): ApiEnvelopeDto<JsonElement> =
        ApiEnvelopeDto(success = true, data = JsonParser.parseString(json))
}

private class FakeAuthRepository(
    private var familyIds: List<Long>,
) : AuthRepository {
    var fetchMeCalls: Int = 0

    override suspend fun registerParent(
        displayName: String,
        birthDate: String,
        email: String,
        password: String,
    ): AuthSession = error("Not used")

    override suspend fun registerChild(
        email: String,
        password: String,
        displayName: String,
        birthDate: String?,
    ): AuthSession = error("Not used")

    override suspend fun login(email: String, password: String): AuthSession = error("Not used")

    override suspend fun fetchMe(): AuthenticatedUser {
        fetchMeCalls += 1
        return AuthenticatedUser(
            id = 1L,
            email = "parent@example.test",
            role = "PARENT",
            isActive = true,
            familyIds = familyIds,
        )
    }

    override fun getAccessToken(): String? = "token"

    override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null

    override fun setActiveChildId(childId: Long) = Unit

    override fun hasParentPin(): Boolean = false

    override fun saveParentPin(pin: String) = Unit

    override fun verifyParentPin(pin: String): Boolean = false

    override fun logout() = Unit

    override fun clearSession() = Unit
}
