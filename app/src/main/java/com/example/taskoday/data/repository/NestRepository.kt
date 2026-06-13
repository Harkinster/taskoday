package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ActiveCompanionDto
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
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NestSnapshot(
    val progress: NestProgressDto,
    val crystals: CrystalBalanceDto,
    val inventory: InventoryDto,
    val bestiary: BestiaryDto,
    val eggs: EggsDto,
    val dragons: DragonsDto,
    val scrolls: ScrollsDto,
)

@Singleton
class NestRepository
    @Inject
    constructor(
        private val nestApi: NestApi,
        private val authRepository: AuthRepository,
    ) {
        private val _progressChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val progressChanges = _progressChanges.asSharedFlow()

        fun hasRemoteSession(): Boolean = !authRepository.getAccessToken().isNullOrBlank()

        fun notifyProgressChanged() {
            _progressChanges.tryEmit(Unit)
        }

        suspend fun loadSnapshot(): Result<NestSnapshot> =
            runCatching {
                val childId = activeChildId()
                NestSnapshot(
                    progress = nestApi.getProgress(childId).data,
                    crystals = nestApi.getCrystals(childId).data,
                    inventory = nestApi.getInventory(childId).data,
                    bestiary = nestApi.getBestiary(childId).data,
                    eggs = nestApi.getEggs(childId).data,
                    dragons = nestApi.getDragons(childId).data,
                    scrolls = nestApi.getScrolls(childId).data,
                )
            }

        suspend fun getChestCatalog(): Result<ChestCatalogDto> =
            runCatching { nestApi.getChestCatalog(activeChildId()).data }

        suspend fun getProgress(): Result<NestProgressDto> =
            runCatching { nestApi.getProgress(activeChildId()).data }

        suspend fun openCatalogChest(catalogId: String): Result<OpenCatalogChestDto> =
            runCatching { nestApi.openCatalogChest(activeChildId(), catalogId).data }

        suspend fun evolveEgg(eggId: Long): Result<EggEvolutionDto> =
            runCatching { nestApi.evolveEgg(activeChildId(), eggId).data }

        suspend fun activateDragon(dragonId: Long): Result<ActiveCompanionDto> =
            runCatching { nestApi.activateDragon(activeChildId(), dragonId).data }

        suspend fun evolveDragon(dragonId: Long): Result<DragonEvolutionDto> =
            runCatching { nestApi.evolveDragon(activeChildId(), dragonId).data }

        private suspend fun activeChildId(): Long =
            authRepository.getActiveChildId(forceRefresh = true)
                ?: throw IllegalStateException("Aucun enfant actif disponible.")
    }
