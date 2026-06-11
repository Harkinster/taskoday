package com.example.taskoday.features.settings

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.ChildProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsProfileIdentityTest {
    @Test
    fun `parent identity comes from connected account instead of active child`() {
        val identity =
            resolveProfileIdentity(
                me = authenticatedUser(email = "parent-one@example.com", role = "PARENT"),
                childProfile = childProfile(displayName = "Test Child", email = "child@example.com"),
            )

        assertEquals("parent-one@example.com", identity.name)
        assertEquals("parent-one@example.com", identity.email)
        assertEquals("Parent • parent-one@example.com", identity.subtitle)
    }

    @Test
    fun `child identity uses display name and connected account email`() {
        val identity =
            resolveProfileIdentity(
                me = authenticatedUser(email = "lea@example.com", role = "CHILD"),
                childProfile = childProfile(displayName = "Léa", email = "stale@example.com"),
            )

        assertEquals("Léa", identity.name)
        assertEquals("lea@example.com", identity.email)
        assertEquals("Enfant • lea@example.com", identity.subtitle)
    }

    @Test
    fun `missing child display name falls back to connected email`() {
        val identity =
            resolveProfileIdentity(
                me = authenticatedUser(email = "child-two@example.com", role = "CHILD"),
                childProfile = childProfile(displayName = " ", email = "stale@example.com"),
            )

        assertEquals("child-two@example.com", identity.name)
        assertEquals("Enfant • child-two@example.com", identity.subtitle)
    }
}

private fun authenticatedUser(
    email: String,
    role: String,
): AuthenticatedUser =
    AuthenticatedUser(
        id = 1L,
        email = email,
        role = role,
        isActive = true,
        familyIds = emptyList(),
    )

private fun childProfile(
    displayName: String,
    email: String,
): ChildProfile =
    ChildProfile(
        id = 10L,
        familyId = 20L,
        userId = 30L,
        email = email,
        displayName = displayName,
        birthDate = null,
        role = "CHILD",
    )
