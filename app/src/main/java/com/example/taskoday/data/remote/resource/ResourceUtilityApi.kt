package com.example.taskoday.data.remote.resource

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ResourceUtilityApi {
    @GET("families/{familyId}/resources/me") suspend fun balance(@Path("familyId") familyId: Long): ApiEnvelopeDto<ResourceBalanceDto>
    @GET("families/{familyId}/wishes/offers") suspend fun offers(@Path("familyId") familyId: Long): ApiEnvelopeDto<WishOfferListDto>
    @POST("families/{familyId}/wishes/offers") suspend fun createOffer(@Path("familyId") familyId: Long, @Body body: WishOfferCreateDto): ApiEnvelopeDto<WishOfferDto>
    @PATCH("families/{familyId}/wishes/offers/{offerId}") suspend fun updateOffer(@Path("familyId") familyId: Long, @Path("offerId") offerId: Long, @Body body: WishOfferUpdateDto): ApiEnvelopeDto<WishOfferDto>
    @GET("families/{familyId}/wishes/requests") suspend fun requests(@Path("familyId") familyId: Long): ApiEnvelopeDto<WishRequestListDto>
    @POST("families/{familyId}/wishes/requests") suspend fun requestWish(@Path("familyId") familyId: Long, @Body body: WishRequestCreateDto): ApiEnvelopeDto<WishRequestDto>
    @POST("families/{familyId}/wishes/requests/{requestId}/cancel") suspend fun cancel(@Path("familyId") familyId: Long, @Path("requestId") requestId: Long): ApiEnvelopeDto<WishRequestDto>
    @POST("families/{familyId}/wishes/requests/{requestId}/approve") suspend fun approve(@Path("familyId") familyId: Long, @Path("requestId") requestId: Long): ApiEnvelopeDto<WishRequestDto>
    @POST("families/{familyId}/wishes/requests/{requestId}/reject") suspend fun reject(@Path("familyId") familyId: Long, @Path("requestId") requestId: Long): ApiEnvelopeDto<WishRequestDto>
    @POST("families/{familyId}/wishes/obtain") suspend fun obtain(@Path("familyId") familyId: Long, @Body body: WishObtainDto): ApiEnvelopeDto<WishRequestDto>
    @GET("families/{familyId}/chests/catalog") suspend fun chestCatalog(@Path("familyId") familyId: Long): ApiEnvelopeDto<ResourceChestCatalogDto>
    @POST("families/{familyId}/chests/open") suspend fun openChest(@Path("familyId") familyId: Long, @Body body: ChestOpenRequestDto): ApiEnvelopeDto<ChestOpenDto>
    @GET("families/{familyId}/chests/opens") suspend fun chestOpens(@Path("familyId") familyId: Long): ApiEnvelopeDto<ChestOpenListDto>
    @GET("families/{familyId}/collection") suspend fun collection(@Path("familyId") familyId: Long): ApiEnvelopeDto<CollectionDto>
}
