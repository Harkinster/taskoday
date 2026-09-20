package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilyMemberRole
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.Locale

data class FamilyMemberDto(
    @SerializedName(value = "user_id", alternate = ["id"])
    val userId: Long,
    @SerializedName(value = "display_name", alternate = ["name"])
    val displayName: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName(value = "is_active", alternate = ["isActive", "active"])
    val isActive: Boolean? = null,
)

data class FamilyInviteDto(
    @SerializedName(value = "code", alternate = ["invite_code", "invitation_code"])
    val code: String? = null,
    @SerializedName(value = "expires_at", alternate = ["expiresAt", "expiration", "expires_on"])
    val expiresAt: String? = null,
)

fun JsonElement.toFamilyMemberDtos(gson: Gson): List<FamilyMemberDto> =
    when {
        isJsonArray -> {
            val listType = object : TypeToken<List<FamilyMemberDto>>() {}.type
            gson.fromJson(this, listType)
        }
        isJsonObject -> {
            val listType = object : TypeToken<List<FamilyMemberDto>>() {}.type
            val nestedMembers =
                asJsonObject.get("members")
                    ?: asJsonObject.get("items")
            if (nestedMembers?.isJsonArray == true) {
                gson.fromJson(nestedMembers, listType)
            } else {
                emptyList()
            }
        }
        else -> emptyList()
    }

fun JsonElement.toFamilyInviteDto(gson: Gson): FamilyInviteDto =
    when {
        isJsonObject -> {
            val candidate =
                asJsonObject.get("invite")
                    ?: asJsonObject.get("invitation")
                    ?: asJsonObject.get("parent_invite")
                    ?: this
            gson.fromJson(candidate, FamilyInviteDto::class.java)
        }
        else -> FamilyInviteDto()
    }

fun FamilyMemberDto.toDomain(): FamilyMember =
    FamilyMember(
        userId = userId,
        displayName = humanizeMemberLabel(displayName, email, userId),
        email = email?.trim()?.takeIf { it.isNotBlank() },
        role = role.toFamilyMemberRole(),
        isActive = isActive ?: true,
    )

fun humanizeMemberLabel(displayName: String?, email: String?, userId: Long): String {
    val raw = (displayName ?: email)
        ?.trim()
        ?.substringBefore("@")
        ?.takeIf { it.isNotBlank() }
        ?: return "Membre #$userId"
    if (!raw.matches(Regex("^[\\p{L}\\d]+[._-][\\p{L}\\d]+$"))) return raw
    return raw.split(Regex("[._-]+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { char -> char.titlecase(Locale.FRANCE) } }
}

fun FamilyInviteDto.toDomain(): FamilyInvite =
    FamilyInvite(
        code = code?.trim()?.takeIf { it.isNotBlank() } ?: error("Code d'invitation introuvable."),
        expiresAt = expiresAt?.trim()?.takeIf { it.isNotBlank() },
    )

private fun String?.toFamilyMemberRole(): FamilyMemberRole =
    when (this?.trim()?.replace("-", "_")?.replace(" ", "_")?.uppercase(Locale.US)) {
        "PARENT", "ADULT", "OWNER" -> FamilyMemberRole.PARENT
        "CHILD", "ENFANT" -> FamilyMemberRole.CHILD
        else -> FamilyMemberRole.OTHER
    }
