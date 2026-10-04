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
    fun `leaving selected family selects remaining family without replacing account`() = runBlocking {
        val auth = FakeAuthRepository(familyIds = listOf(7L, 8L))
        val api = FakeFamilyApi()
        api.families = listOf(FamilySummaryDto(7L, "Famille 7"), FamilySummaryDto(8L, "Famille 8"))
        api.onLeave = { auth.familyIds = listOf(7L); api.families = listOf(FamilySummaryDto(7L, "Famille 7")) }
        val repository = FamilyRepositoryImpl(auth, api, Gson())

        assertEquals(7L, repository.leaveFamily(8L).getOrThrow())
        assertEquals(8L, api.lastLeftFamilyId)
        assertEquals(7L, auth.activeFamilyId)
        assertEquals("token", auth.getAccessToken())
        assertEquals(listOf(7L), auth.fetchMe().familyIds)
    }

    @Test
    fun `archiving selected family selects another accessible family`() = runBlocking {
        val auth = FakeAuthRepository(familyIds = listOf(7L, 9L))
        val api = FakeFamilyApi()
        api.families = listOf(FamilySummaryDto(7L, "Famille 7"), FamilySummaryDto(9L, "Famille 9"))
        api.onArchive = { auth.familyIds = listOf(7L); api.families = listOf(FamilySummaryDto(7L, "Famille 7")) }

        assertEquals(7L, FamilyRepositoryImpl(auth, api, Gson()).archiveFamily(9L).getOrThrow())
        assertEquals(9L, api.lastArchivedFamilyId)
        assertEquals(7L, auth.activeFamilyId)
        assertEquals("token", auth.getAccessToken())
    }

    @Test
    fun `archiving only family clears active family and keeps account`() = runBlocking {
        val auth = FakeAuthRepository(familyIds = listOf(9L))
        val api = FakeFamilyApi()
        api.onArchive = { auth.familyIds = emptyList(); api.families = emptyList() }

        assertEquals(null, FamilyRepositoryImpl(auth, api, Gson()).archiveFamily(9L).getOrThrow())
        assertEquals(null, auth.activeFamilyId)
        assertEquals("token", auth.getAccessToken())
    }

    @Test
    fun `remove member sends explicit family and user identifiers`() = runBlocking {
        val auth = FakeAuthRepository(familyIds = listOf(7L, 8L))
        val api = FakeFamilyApi()
        val repository = FamilyRepositoryImpl(auth, api, Gson())

        repository.removeMember(8L, 29L).getOrThrow()

        assertEquals(8L to 29L, api.lastRemoval)
        assertEquals(8L, auth.activeFamilyId)
        assertEquals("token", auth.getAccessToken())
    }

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
    var lastLeftFamilyId: Long? = null
    var lastArchivedFamilyId: Long? = null
    var lastRemoval: Pair<Long, Long>? = null
    var families = listOf(FamilySummaryDto(4L, "Famille Test"))
    var onLeave: (() -> Unit)? = null
    var onArchive: (() -> Unit)? = null

    override suspend fun getMyFamilies(): ApiEnvelopeDto<List<FamilySummaryDto>> =
        ApiEnvelopeDto(success = true, data = families)

    override suspend fun leaveFamily(familyId: Long): ApiEnvelopeDto<JsonElement> {
        lastLeftFamilyId = familyId
        onLeave?.invoke()
        return envelope("{}")
    }

    override suspend fun archiveFamily(familyId: Long): ApiEnvelopeDto<JsonElement> {
        lastArchivedFamilyId = familyId
        onArchive?.invoke()
        return envelope("{}")
    }

    override suspend fun removeMember(familyId: Long, userId: Long): ApiEnvelopeDto<JsonElement> {
        lastRemoval = familyId to userId
        return envelope("{}")
    }

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
    var familyIds: List<Long>,
) : AuthRepository {
    var fetchMeCalls: Int = 0
    var activeFamilyId: Long? = familyIds.lastOrNull()

    override suspend fun getActiveFamilyId(forceRefresh: Boolean): Long? =
        activeFamilyId?.takeIf { it in familyIds } ?: familyIds.singleOrNull()

    override fun setActiveFamilyId(familyId: Long) { activeFamilyId = familyId }

    override fun clearActiveFamilyId() { activeFamilyId = null }

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
