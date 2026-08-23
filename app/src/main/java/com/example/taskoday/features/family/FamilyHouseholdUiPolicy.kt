package com.example.taskoday.features.family

import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilyMemberRole
import java.util.Locale

data class FamilyHouseholdGroups(
    val parents: List<FamilyMember>,
    val children: List<FamilyMember>,
    val others: List<FamilyMember>,
)

fun groupFamilyHouseholdMembers(members: List<FamilyMember>): FamilyHouseholdGroups {
    val activeMembers = members.filter { member -> member.isActive }.distinctBy { member -> member.userId }
    return FamilyHouseholdGroups(
        parents =
            activeMembers
                .filter { member -> member.role == FamilyMemberRole.PARENT }
                .sortedBy { member -> member.displayName.lowercase(Locale.FRANCE) },
        children =
            activeMembers
                .filter { member -> member.role == FamilyMemberRole.CHILD }
                .sortedBy { member -> member.displayName.lowercase(Locale.FRANCE) },
        others =
            activeMembers
                .filter { member -> member.role == FamilyMemberRole.OTHER }
                .sortedBy { member -> member.displayName.lowercase(Locale.FRANCE) },
    )
}

fun normalizeFamilyInviteCodeInput(value: String): String =
    value.trim()

fun familyMemberSecondaryLabel(member: FamilyMember): String? =
    when (member.role) {
        FamilyMemberRole.PARENT -> member.email?.takeIf { email -> email.isNotBlank() }
        FamilyMemberRole.CHILD ->
            member.email
                ?.takeIf { email -> email.isNotBlank() && !email.contains("@children.taskoday.app", ignoreCase = true) }
        FamilyMemberRole.OTHER -> member.email?.takeIf { email -> email.isNotBlank() }
    }

fun familyInviteExpirationLabel(expiresAt: String?): String =
    expiresAt
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace("T", " à ")
        ?.removeSuffix("Z")
        ?.let { value -> "Expire le $value" }
        ?: "Expiration fournie par le serveur."

fun familyInviteUserMessage(
    httpCode: Int?,
    backendMessage: String?,
    fallback: String,
): String {
    cleanFamilyBackendMessage(backendMessage)?.let { cleanMessage ->
        val normalized = cleanMessage.lowercase(Locale.FRANCE)
        return when {
            "expired" in normalized || "expir" in normalized -> "Ce code a expiré."
            "already used" in normalized || "deja utilise" in normalized || "déjà utilisé" in normalized -> "Ce code a déjà été utilisé."
            "same family" in normalized || "already member of this family" in normalized -> "Ce compte est déjà membre de ce foyer."
            "another family" in normalized || "different family" in normalized -> "Ce compte est déjà membre d'un autre foyer."
            "not allowed" in normalized || "unauthorized" in normalized || "forbidden" in normalized -> "Ce compte n'est pas autorisé à rejoindre ce foyer."
            "invalid" in normalized || "introuvable" in normalized -> "Code d'invitation invalide."
            else -> cleanMessage
        }
    }

    return when (httpCode) {
        400, 404, 422 -> "Code d'invitation invalide ou expiré."
        401, 403 -> "Ce compte n'est pas autorisé à rejoindre ce foyer."
        409 -> "Ce compte est déjà rattaché à un foyer."
        else -> fallback
    }
}

private fun cleanFamilyBackendMessage(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("HTTP ")) return null
    return trimmed.takeIf { it.length <= 180 }
}
