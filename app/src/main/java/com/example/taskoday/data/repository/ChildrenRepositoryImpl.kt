package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.ChildCreateRequestDto
import com.example.taskoday.data.remote.dto.ChildResponseDto
import com.example.taskoday.data.remote.dto.ChildUpdateRequestDto
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildrenRepositoryImpl
    @Inject
    constructor(
        private val childrenApi: ChildrenApi,
        private val authRepository: AuthRepository,
    ) : ChildrenRepository {
        override suspend fun fetchChildren(): List<ParentChild> =
            childrenApi.getChildren(authRepository.getActiveFamilyId()).data.map { child ->
                child.toDomain()
            }

        override suspend fun createChild(
            displayName: String,
            email: String?,
            birthDate: String?,
        ): ParentChild =
            childrenApi
                .createChild(
                    ChildCreateRequestDto(
                        displayName = displayName.trim(),
                        email = email?.trim()?.takeIf { it.isNotBlank() },
                        birthDate = birthDate?.trim()?.takeIf { it.isNotBlank() },
                        familyId = authRepository.getActiveFamilyId(),
                    ),
                ).data
                .toDomain()

        override suspend fun updateChildDisplayName(
            childId: Long,
            displayName: String,
        ) {
            childrenApi.updateChild(
                childId = childId,
                payload = ChildUpdateRequestDto(displayName = displayName.trim()),
            )
        }
    }

private fun ChildResponseDto.toDomain(): ParentChild =
    ParentChild(
        id = id,
        displayName = displayName,
        email = email,
    )
