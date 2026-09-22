package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.data.remote.dto.CreateFamilyRequestDto
import com.example.taskoday.data.remote.dto.toFamilyInviteDto
import com.example.taskoday.data.remote.dto.toFamilyMemberDtos
import com.example.taskoday.data.remote.family.FamilyApi
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilySummary
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
        override suspend fun getActiveFamilyId(): Long? = authRepository.getActiveFamilyId()

        override fun setActiveFamilyId(familyId: Long) = authRepository.setActiveFamilyId(familyId)

        override suspend fun fetchFamilies(): Result<List<FamilySummary>> =
            runCatching { familyApi.getMyFamilies().data.map { it.toDomain() } }

        override suspend fun createFamily(name: String): Result<FamilySummary> =
            runCatching {
                val normalized = name.trim().takeIf { it.isNotBlank() } ?: error("Nom de famille requis.")
                familyApi.createFamily(CreateFamilyRequestDto(normalized)).data.toDomain().also {
                    authRepository.setActiveFamilyId(it.id)
                }
            }

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
                val before = authRepository.fetchMe().familyIds.toSet()
                familyApi.acceptParentInvite(normalizedCode)
                authRepository.fetchMe().also { me ->
                    val selected = me.familyIds.firstOrNull { it !in before }
                    if (selected != null) authRepository.setActiveFamilyId(selected)
                }
            }

        private suspend fun resolveFamilyId(): Long =
            authRepository.getActiveFamilyId()
                ?: error("Aucun foyer actif pour ce compte.")
    }
