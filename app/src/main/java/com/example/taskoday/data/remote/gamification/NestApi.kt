package com.example.taskoday.data.remote.gamification

import com.example.taskoday.data.remote.dto.ActiveCompanionDto
import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.BestiaryDto
import com.example.taskoday.data.remote.dto.ChestCatalogDto
import com.example.taskoday.data.remote.dto.CrystalBalanceDto
import com.example.taskoday.data.remote.dto.DragonsDto
import com.example.taskoday.data.remote.dto.DragonEvolutionDto
import com.example.taskoday.data.remote.dto.EggEvolutionDto
import com.example.taskoday.data.remote.dto.EggsDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.NestProgressDto
import com.example.taskoday.data.remote.dto.OpenCatalogChestDto
import com.example.taskoday.data.remote.dto.ScrollsDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NestApi {
    @GET("children/{childId}/progress")
    suspend fun getProgress(@Path("childId") childId: Long): ApiEnvelopeDto<NestProgressDto>

    @GET("children/{childId}/crystals")
    suspend fun getCrystals(@Path("childId") childId: Long): ApiEnvelopeDto<CrystalBalanceDto>

    @GET("children/{childId}/inventory")
    suspend fun getInventory(@Path("childId") childId: Long): ApiEnvelopeDto<InventoryDto>

    @GET("children/{childId}/chests/catalog")
    suspend fun getChestCatalog(@Path("childId") childId: Long): ApiEnvelopeDto<ChestCatalogDto>

    @POST("children/{childId}/chests/catalog/{catalogId}/open")
    suspend fun openCatalogChest(
        @Path("childId") childId: Long,
        @Path("catalogId") catalogId: String,
    ): ApiEnvelopeDto<OpenCatalogChestDto>

    @GET("children/{childId}/bestiary")
    suspend fun getBestiary(@Path("childId") childId: Long): ApiEnvelopeDto<BestiaryDto>

    @GET("children/{childId}/eggs")
    suspend fun getEggs(@Path("childId") childId: Long): ApiEnvelopeDto<EggsDto>

    @POST("children/{childId}/eggs/{eggId}/evolve")
    suspend fun evolveEgg(
        @Path("childId") childId: Long,
        @Path("eggId") eggId: Long,
    ): ApiEnvelopeDto<EggEvolutionDto>

    @GET("children/{childId}/dragons")
    suspend fun getDragons(@Path("childId") childId: Long): ApiEnvelopeDto<DragonsDto>

    @POST("children/{childId}/dragons/{dragonId}/activate")
    suspend fun activateDragon(
        @Path("childId") childId: Long,
        @Path("dragonId") dragonId: Long,
    ): ApiEnvelopeDto<ActiveCompanionDto>

    @POST("children/{childId}/dragons/{dragonId}/evolve")
    suspend fun evolveDragon(
        @Path("childId") childId: Long,
        @Path("dragonId") dragonId: Long,
    ): ApiEnvelopeDto<DragonEvolutionDto>

    @GET("children/{childId}/scrolls")
    suspend fun getScrolls(@Path("childId") childId: Long): ApiEnvelopeDto<ScrollsDto>
}
