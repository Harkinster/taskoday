package com.example.taskoday.features.family

import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilyMemberRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyHouseholdUiPolicyTest {
    @Test
    fun `members are separated by role and inactive members are hidden`() {
        val groups =
            groupFamilyHouseholdMembers(
                listOf(
                    member(2L, "Enfant Test", FamilyMemberRole.CHILD),
                    member(1L, "Parent Test", FamilyMemberRole.PARENT),
                    member(3L, "Parent Invite", FamilyMemberRole.PARENT, isActive = false),
                ),
            )

        assertEquals(listOf("Parent Test"), groups.parents.map { member -> member.displayName })
        assertEquals(listOf("Enfant Test"), groups.children.map { member -> member.displayName })
    }

    @Test
    fun `invite code normalization trims only outside spaces`() {
        assertEquals("AbC-123", normalizeFamilyInviteCodeInput("  AbC-123  "))
    }

    @Test
    fun `child technical email is hidden but parent email remains visible`() {
        assertEquals(
            "parent@example.test",
            familyMemberSecondaryLabel(member(1L, "Parent", FamilyMemberRole.PARENT, email = "parent@example.test")),
        )
        assertNull(
            familyMemberSecondaryLabel(
                member(2L, "Child", FamilyMemberRole.CHILD, email = "child-abc@children.taskoday.app"),
            ),
        )
    }

    @Test
    fun `invite errors are user friendly`() {
        assertEquals("Ce code a expiré.", familyInviteUserMessage(400, "Invite expired", "fallback"))
        assertEquals("Ce code a déjà été utilisé.", familyInviteUserMessage(409, "already used", "fallback"))
        assertEquals("Ce compte est déjà membre d'un autre foyer.", familyInviteUserMessage(409, "another family", "fallback"))
        assertEquals("Code d'invitation invalide ou expiré.", familyInviteUserMessage(404, null, "fallback"))
        assertEquals("fallback", familyInviteUserMessage(null, "{\"detail\":\"raw\"}", "fallback"))
    }

    private fun member(
        id: Long,
        name: String,
        role: FamilyMemberRole,
        email: String? = null,
        isActive: Boolean = true,
    ): FamilyMember =
        FamilyMember(
            userId = id,
            displayName = name,
            email = email,
            role = role,
            isActive = isActive,
        )
}
