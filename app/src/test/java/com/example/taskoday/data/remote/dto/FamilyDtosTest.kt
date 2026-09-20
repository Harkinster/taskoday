package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyMemberRole
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyDtosTest {
    private val gson = Gson()

    @Test
    fun `members payload maps active parents and children`() {
        val payload =
            JsonParser.parseString(
                """
                {
                  "members": [
                    {"user_id": 10, "display_name": "Parent Test", "role": "PARENT", "email": "parent@example.test", "is_active": true},
                    {"user_id": 11, "display_name": "Second Parent", "role": "PARENT", "email": "second@example.test", "is_active": true},
                    {"user_id": 20, "display_name": "Enfant Test", "role": "CHILD", "email": "child@example.test", "is_active": false}
                  ]
                }
                """.trimIndent(),
            )

        val members = payload.toFamilyMemberDtos(gson).map { dto -> dto.toDomain() }

        assertEquals(listOf(10L, 11L, 20L), members.map { member -> member.userId })
        assertEquals(FamilyMemberRole.PARENT, members[0].role)
        assertEquals(FamilyMemberRole.PARENT, members[1].role)
        assertEquals(FamilyMemberRole.CHILD, members[2].role)
        assertTrue(members[0].isActive)
        assertTrue(members[1].isActive)
        assertFalse(members[2].isActive)
    }

    @Test
    fun `parent invite payload maps code and expiration`() {
        val payload =
            JsonParser.parseString(
                """
                {
                  "invite": {
                    "invite_code": "AbC-123",
                    "expires_at": "2026-08-25T20:00:00Z"
                  }
                }
                """.trimIndent(),
            )

        val invite = payload.toFamilyInviteDto(gson).toDomain()

        assertEquals("AbC-123", invite.code)
        assertEquals("2026-08-25T20:00:00Z", invite.expiresAt)
    }

    @Test
    fun `login-like member labels become readable names`() {
        val member = FamilyMemberDto(userId = 10L, displayName = "laurens.matthieu", email = null).toDomain()

        assertEquals("Laurens Matthieu", member.displayName)
    }

    @Test
    fun `email-like labels use a readable fallback`() {
        assertEquals("Laurens Matthieu", humanizeMemberLabel("laurens.matthieu@yahoo.fr", null, 10L))
    }
}
