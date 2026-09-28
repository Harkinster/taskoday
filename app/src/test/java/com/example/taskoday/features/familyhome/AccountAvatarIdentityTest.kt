package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.AuthenticatedUser
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountAvatarIdentityTest {
    @Test fun `parent avatar comes from connected display name`() {
        assertEquals("AL", accountAvatarInitials(user("Ada Lovelace")))
    }

    @Test fun `child avatar follows connected child not previous parent`() {
        assertEquals("NC", accountAvatarInitials(user("Nino Child", "CHILD")))
    }

    @Test fun `missing account has neutral avatar instead of sample initials`() {
        assertEquals("", accountAvatarInitials(null))
    }

    @Test fun `blank display name falls back to account email`() {
        assertEquals("A", accountAvatarInitials(user(" ")))
    }

    @Test fun `whitespace and long name keep avatar to two initials`() {
        assertEquals("AL", accountAvatarInitials(user("  Ada\tLovelace Byron  ")))
    }

    private fun user(name: String, role: String = "PARENT") =
        AuthenticatedUser(1L, "ada@example.com", role, true, listOf(7L), name)
}
