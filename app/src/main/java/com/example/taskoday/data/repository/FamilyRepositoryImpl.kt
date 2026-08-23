package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.toFamilyInviteDto
import com.example.taskoday.data.remote.dto.toFamilyMemberDtos
import com.example.taskoday.data.remote.family.FamilyApi
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val familyApi: FamilyApi,
        private val gson: Gson,
    ) : FamilyRepository {
        override suspend fun fetchMembers(): Result<List<FamilyMember>> =
            runCatching {
                val familyId = resolveFamilyId()
                familyApi
                    .getFamilyMembers(familyId)
                    .data
                    .toFamilyMemberDtos(gson)
                    .map { member -> member.toDomain() }
                    .filter { member -> member.isActive }
                    .distinctBy { member -> member.userId }
            }

        override suspend fun createParentInvite(): Result<FamilyInvite> =
            runCatching {
                val familyId = resolveFamilyId()
                familyApi
                    .createParentInvite(familyId)
                    .data
                    .toFamilyInviteDto(gson)
                    .toDomain()
            }

        override suspend fun acceptParentInvite(code: String): Result<AuthenticatedUser> =
            runCatching {
                val normalizedCode = code.trim().takeIf { it.isNotBlank() } ?: error("Code d'invitation requis.")
                familyApi.acceptParentInvite(normalizedCode)
                authRepository.fetchMe()
            }

        private suspend fun resolveFamilyId(): Long =
            authRepository
                .fetchMe()
                .familyIds
                .firstOrNull()
                ?: error("Aucun foyer actif pour ce compte.")
    }
