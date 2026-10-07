package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.resource.ResourceUtilityApi
import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceUtilityRepository @Inject constructor(
    private val api: ResourceUtilityApi,
    private val auth: AuthRepository,
) {
    suspend fun currentContext(): Pair<Long, Boolean> {
        val user = auth.fetchMe()
        val familyId = auth.getActiveFamilyId() ?: user.familyIds.singleOrNull()
            ?: error("Aucune famille active.")
        check(familyId in user.familyIds) { "La famille active ne correspond pas au compte connecté." }
        return familyId to user.role.equals("PARENT", ignoreCase = true)
    }
    suspend fun loadBalance(familyId: Long) = api.balance(familyId).data.also { check(it.familyId == familyId) }
    suspend fun loadOffers(familyId: Long) = api.offers(familyId).data.items
    suspend fun loadRequests(familyId: Long) = api.requests(familyId).data.items
    suspend fun loadChests(familyId: Long) = api.chestCatalog(familyId).data.also { check(it.familyId == familyId) }
    suspend fun loadCollection(familyId: Long) = api.collection(familyId).data
    suspend fun loadChestOpens(familyId: Long) = api.chestOpens(familyId).data.items
    suspend fun createOffer(familyId: Long, title: String, cost: Int) = api.createOffer(familyId, WishOfferCreateDto(title, null, cost)).data
    suspend fun updateOffer(familyId: Long, offerId: Long, title: String, cost: Int) = api.updateOffer(familyId, offerId, WishOfferUpdateDto(title = title, flameCost = cost)).data
    suspend fun deactivateOffer(familyId: Long, offerId: Long) = api.updateOffer(familyId, offerId, WishOfferUpdateDto(active = false)).data
    suspend fun request(familyId: Long, offerId: Long) = api.requestWish(familyId, WishRequestCreateDto(offerId)).data
    suspend fun cancel(familyId: Long, requestId: Long) = api.cancel(familyId, requestId).data
    suspend fun approve(familyId: Long, requestId: Long) = api.approve(familyId, requestId).data
    suspend fun reject(familyId: Long, requestId: Long) = api.reject(familyId, requestId).data
    suspend fun obtain(familyId: Long, offerId: Long) = api.obtain(familyId, WishObtainDto(offerId, UUID.randomUUID().toString())).data
    suspend fun openChest(familyId: Long, type: String, idempotencyKey: String) = api.openChest(familyId, ChestOpenRequestDto(type, idempotencyKey)).data
}
