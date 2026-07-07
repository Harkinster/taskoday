package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.RewardDao
import com.example.taskoday.data.remote.dto.ExternalRewardDto
import com.example.taskoday.data.remote.dto.RewardCouponDto
import com.example.taskoday.data.remote.dto.RewardCreateRequestDto
import com.example.taskoday.data.remote.dto.RewardRequestCreateDto
import com.example.taskoday.data.remote.dto.RewardRequestDecisionDto
import com.example.taskoday.data.remote.dto.RewardRequestDto
import com.example.taskoday.data.remote.dto.RewardUpdateRequestDto
import com.example.taskoday.data.remote.rewards.RewardsApi
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardCoupon
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus
import com.example.taskoday.domain.model.RewardShopSnapshot
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.RewardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RewardRepositoryImpl
    @Inject
    constructor(
        private val rewardDao: RewardDao,
        private val rewardsApi: RewardsApi,
        private val authRepository: AuthRepository,
    ) : RewardRepository {
        override fun observeActiveRewards(): Flow<List<Reward>> =
            rewardDao.observeActive().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getReward(rewardId: Long): Reward? = rewardDao.getById(rewardId)?.toDomain()

        override suspend fun upsertReward(reward: Reward): Long = rewardDao.upsert(reward.toEntity())

        override suspend fun fetchRemoteShopSnapshot(
            childId: Long?,
            includeInactiveRewards: Boolean,
        ): Result<RewardShopSnapshot> =
            runCatching {
                val targetChildId = childId ?: activeChildId()
                val rewardsData = rewardsApi.getRewards(targetChildId, includeInactive = includeInactiveRewards).data
                val requestsData = rewardsApi.getRewardRequests(targetChildId).data
                RewardShopSnapshot(
                    childId = targetChildId,
                    scalesBalance = rewardsData.scalesBalance,
                    rewards = rewardsData.rewards.map { it.toDomain() },
                    requests = requestsData.requests.map { it.toDomain() },
                )
            }

        override suspend fun createRemoteReward(
            title: String,
            description: String?,
            costScales: Int,
            isActive: Boolean,
        ): Result<Reward> =
            runCatching {
                val childId = activeChildId()
                rewardsApi
                    .createReward(
                        childId = childId,
                        payload =
                            RewardCreateRequestDto(
                                title = title.trim(),
                                description = description?.trim()?.takeIf { it.isNotBlank() },
                                costScales = costScales.coerceAtLeast(0),
                                isActive = isActive,
                            ),
                    ).data
                    .toDomain()
            }

        override suspend fun updateRemoteReward(
            rewardId: Long,
            title: String,
            description: String?,
            costScales: Int,
            isActive: Boolean,
        ): Result<Reward> =
            runCatching {
                rewardsApi
                    .updateReward(
                        rewardId = rewardId,
                        payload =
                            RewardUpdateRequestDto(
                                title = title.trim(),
                                description = description?.trim()?.takeIf { it.isNotBlank() },
                                costScales = costScales.coerceAtLeast(0),
                                isActive = isActive,
                            ),
                    ).data
                    .toDomain()
            }

        override suspend fun deactivateRemoteReward(rewardId: Long): Result<Reward> =
            runCatching {
                rewardsApi
                    .updateReward(
                        rewardId = rewardId,
                        payload = RewardUpdateRequestDto(isActive = false),
                    ).data
                    .toDomain()
            }

        override suspend fun requestRemoteReward(
            childId: Long,
            rewardId: Long,
            note: String?,
        ): Result<RewardRedemptionRequest> =
            runCatching {
                require(activeChildId() == childId) { "Aucun enfant sélectionné." }
                rewardsApi
                    .requestReward(
                        rewardId = rewardId,
                        payload = RewardRequestCreateDto(note = note?.trim()?.takeIf { it.isNotBlank() }),
                    ).data
                    .toDomain()
            }

        override suspend fun decideRemoteRequest(
            requestId: Long,
            status: RewardRequestStatus,
        ): Result<RewardRedemptionRequest> =
            runCatching {
                rewardsApi
                    .decideRewardRequest(
                        requestId = requestId,
                        payload = RewardRequestDecisionDto(status = status.apiValue),
                    ).data
                    .toDomain()
            }

        override suspend fun useRemoteCoupon(couponId: Long): Result<RewardRedemptionRequest> =
            runCatching { rewardsApi.useCoupon(couponId).data.toDomain() }

        private suspend fun activeChildId(): Long =
            authRepository.getActiveChildId(forceRefresh = true)
                ?: throw IllegalStateException("Aucun enfant actif disponible.")
    }

private fun ExternalRewardDto.toDomain(): Reward =
    Reward(
        id = id,
        title = title,
        description = description,
        cost = costScales,
        emoji = emoji?.takeIf { it.length <= 2 } ?: "\uD83C\uDF81",
        isActive = isActive,
        childId = childId,
        createdAt = 0L,
        updatedAt = 0L,
    )

private fun RewardRequestDto.toDomain(): RewardRedemptionRequest =
    RewardRedemptionRequest(
        id = id,
        childId = childId,
        rewardId = rewardId,
        rewardTitle = rewardTitle,
        costScales = costScales,
        status = RewardRequestStatus.fromApi(status),
        requestedAt = requestedAt,
        decidedAt = decidedAt,
        expiresAt = expiresAt,
        note = note,
        parentNote = parentNote,
        coupon = coupon?.toDomain(),
    )

private fun RewardCouponDto.toDomain(): RewardCoupon =
    RewardCoupon(
        id = id,
        requestId = requestId,
        childId = childId,
        rewardId = rewardId,
        code = code,
        status = status,
        createdAt = createdAt,
        usedAt = usedAt,
    )
