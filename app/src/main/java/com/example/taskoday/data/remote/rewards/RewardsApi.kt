package com.example.taskoday.data.remote.rewards

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.ExternalRewardDto
import com.example.taskoday.data.remote.dto.RewardCreateRequestDto
import com.example.taskoday.data.remote.dto.RewardRequestCreateDto
import com.example.taskoday.data.remote.dto.RewardRequestDecisionDto
import com.example.taskoday.data.remote.dto.RewardRequestDto
import com.example.taskoday.data.remote.dto.RewardRequestsDataDto
import com.example.taskoday.data.remote.dto.RewardsDataDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RewardsApi {
    @GET("children/{childId}/wishes")
    suspend fun getRewards(
        @Path("childId") childId: Long,
    ): ApiEnvelopeDto<RewardsDataDto>

    @POST("children/{childId}/wishes")
    suspend fun createReward(
        @Path("childId") childId: Long,
        @Body payload: RewardCreateRequestDto,
    ): ApiEnvelopeDto<ExternalRewardDto>

    @POST("wishes/{rewardId}/requests")
    suspend fun requestReward(
        @Path("rewardId") rewardId: Long,
        @Body payload: RewardRequestCreateDto,
    ): ApiEnvelopeDto<RewardRequestDto>

    @GET("children/{childId}/wish-requests")
    suspend fun getRewardRequests(
        @Path("childId") childId: Long,
    ): ApiEnvelopeDto<RewardRequestsDataDto>

    @PATCH("reward-requests/{requestId}")
    suspend fun decideRewardRequest(
        @Path("requestId") requestId: Long,
        @Body payload: RewardRequestDecisionDto,
    ): ApiEnvelopeDto<RewardRequestDto>

    @POST("scrolls/{couponId}/use")
    suspend fun useCoupon(
        @Path("couponId") couponId: Long,
    ): ApiEnvelopeDto<RewardRequestDto>
}
