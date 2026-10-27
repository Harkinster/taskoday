package com.example.taskoday.features.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.remote.dto.BestiaryDto
import com.example.taskoday.data.remote.dto.CrystalBalanceDto
import com.example.taskoday.data.remote.dto.DragonDto
import com.example.taskoday.data.remote.dto.DragonsDto
import com.example.taskoday.data.remote.dto.EggEvolutionDto
import com.example.taskoday.data.remote.dto.EggsDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.NestProgressDto
import com.example.taskoday.data.remote.dto.ScrollsDto
import com.example.taskoday.data.remote.dto.PersonalRewardSummaryDto
import com.example.taskoday.data.remote.dto.LineageDto
import com.example.taskoday.data.remote.dto.MyCompanionsDto
import com.example.taskoday.data.remote.dto.PersonalCompanionDto
import com.example.taskoday.data.repository.NestSnapshot
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.repository.toRemoteUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

data class NestUiState(
    val hasRemoteSession: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val progress: NestProgressDto? = null,
    val crystals: CrystalBalanceDto? = null,
    val inventory: InventoryDto? = null,
    val bestiary: BestiaryDto? = null,
    val eggs: EggsDto? = null,
    val dragons: DragonsDto? = null,
    val scrolls: ScrollsDto? = null,
    val personalRewards: PersonalRewardSummaryDto? = null,
    val accountUserId: Long? = null,
    val personalCompanions: MyCompanionsDto? = null,
    val personalCompanionsLoaded: Boolean = false,
    val personalCompanionsLoading: Boolean = false,
    val personalCompanionMessage: String? = null,
    val lineages: List<LineageDto> = emptyList(),
    val isLoadingLineages: Boolean = false,
    val starterCreated: Boolean = false,
    val userMessage: String? = null,
    val hatchingCelebration: NestHatchingCelebration? = null,
)

data class NestHatchingCelebration(
    val eventId: Long,
    val dragonId: Long? = null,
    val dragonKey: String? = null,
    val dragonTitle: String? = null,
    val dragonStage: String? = null,
) {
    val hasDragonDetails: Boolean
        get() = dragonId != null && !dragonTitle.isNullOrBlank()
}

@HiltViewModel
class NestViewModel
    @Inject
    constructor(
        private val nestRepository: NestRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NestUiState(hasRemoteSession = nestRepository.hasRemoteSession()))
        val uiState: StateFlow<NestUiState> = _uiState.asStateFlow()
        private var nextHatchingEventId = 0L
        private var refreshJob: Job? = null
        private val resumeRefresh = com.example.taskoday.core.util.InitialResumeRefresh()

        fun onScreenResumed() = resumeRefresh.onResume { refresh() }

        init {
            observeProgressChanges()
            refresh()
        }

        private fun observeProgressChanges() {
            viewModelScope.launch {
                nestRepository.progressChanges.collect {
                    refresh()
                }
            }
        }

        fun refresh() {
            refreshJob?.cancel()
            if (!nestRepository.hasRemoteSession()) {
                _uiState.value = NestUiState(isLoading = false)
                return
            }
            _uiState.value = NestUiState(hasRemoteSession = true)
            refreshJob = viewModelScope.launch {
                val userId = runCatching { nestRepository.currentUserId() }.getOrElse { error ->
                    ensureActive()
                    _uiState.update {
                        it.copy(isLoading = false, userMessage = error.toRemoteUserMessage("Impossible de vérifier la session."))
                    }
                    return@launch
                }
                ensureActive()
                _uiState.update { it.copy(accountUserId = userId, personalCompanionsLoading = true) }
                launch { refreshPersonalCompanions(userId) }
                launch {
                    nestRepository.loadPersonalRewards().onSuccess { summary ->
                        ensureActive()
                        _uiState.update { it.copy(personalRewards = summary) }
                    }
                }
                nestRepository
                    .loadSnapshot()
                    .onSuccess { snapshot ->
                        ensureActive()
                        _uiState.update {
                            it.withSnapshot(snapshot).copy(
                                hasRemoteSession = true,
                                isLoading = false,
                            )
                        }
                    }.onFailure { error ->
                        ensureActive()
                        _uiState.update {
                            it.copy(
                                hasRemoteSession = true,
                                isLoading = false,
                                userMessage = error.toRemoteUserMessage("Impossible de charger Le Nid."),
                            )
                        }
                    }
            }
        }

        fun loadLineages() {
            val state = _uiState.value
            if (state.lineages.isNotEmpty() || state.isLoadingLineages || !state.hasRemoteSession) return
            _uiState.update { it.copy(isLoadingLineages = true, userMessage = null) }
            viewModelScope.launch {
                nestRepository.loadLineages()
                    .onSuccess { lineages ->
                        _uiState.update { it.copy(lineages = lineages, isLoadingLineages = false) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoadingLineages = false, userMessage = error.toRemoteUserMessage("Impossible de charger les lignées.")) }
                    }
            }
        }

        fun createStarter(lineageId: String, displayName: String) {
            val expectedUserId = _uiState.value.accountUserId ?: return
            if (_uiState.value.isSubmitting || !_uiState.value.hasRemoteSession) return
            _uiState.update { it.copy(isSubmitting = true, userMessage = null) }
            viewModelScope.launch {
                nestRepository.createStarter(lineageId, displayName)
                    .onSuccess { starter ->
                        if (_uiState.value.accountUserId != expectedUserId) return@onSuccess
                        refreshPersonalCompanions(expectedUserId)
                        updatePersonalMutation(expectedUserId) {
                            it.copy(isSubmitting = false, starterCreated = true, userMessage = if (starter.reusedExisting) "Ton compagnon existant a été retrouvé." else "Ton dragon a rejoint le Nid.")
                        }
                    }
                    .onFailure { error ->
                        if (_uiState.value.accountUserId != expectedUserId) return@onFailure
                        if ((error as? retrofit2.HttpException)?.code() == 409) {
                            refreshPersonalCompanions(expectedUserId)
                        }
                        updatePersonalMutation(expectedUserId) { it.copy(isSubmitting = false, userMessage = error.toRemoteUserMessage("Impossible d'accueillir ce dragon.")) }
                    }
            }
        }

        fun activatePersonalCompanion(dragonId: Long) {
            val expectedUserId = _uiState.value.accountUserId ?: return
            if (_uiState.value.isSubmitting || !_uiState.value.hasRemoteSession) return
            _uiState.update { it.copy(isSubmitting = true, userMessage = null) }
            viewModelScope.launch {
                nestRepository.activatePersonalCompanion(dragonId)
                    .onSuccess { result ->
                        if (_uiState.value.accountUserId != expectedUserId) return@onSuccess
                        refreshPersonalCompanions(expectedUserId)
                        updatePersonalMutation(expectedUserId) { it.copy(isSubmitting = false, userMessage = "Compagnon actif mis à jour.") }
                    }
                    .onFailure { error ->
                        updatePersonalMutation(expectedUserId) { it.copy(isSubmitting = false, userMessage = error.toRemoteUserMessage("Impossible de changer de compagnon.")) }
                    }
            }
        }

        fun renamePersonalCompanion(dragonId: Long, displayName: String) {
            val expectedUserId = _uiState.value.accountUserId ?: return
            if (_uiState.value.isSubmitting || !_uiState.value.hasRemoteSession) return
            _uiState.update { it.copy(isSubmitting = true, userMessage = null) }
            viewModelScope.launch {
                nestRepository.renamePersonalCompanion(dragonId, displayName)
                    .onSuccess { result ->
                        if (_uiState.value.accountUserId != expectedUserId) return@onSuccess
                        refreshPersonalCompanions(expectedUserId)
                        updatePersonalMutation(expectedUserId) { it.copy(isSubmitting = false, userMessage = "Nom du compagnon mis à jour.") }
                    }
                    .onFailure { error ->
                        updatePersonalMutation(expectedUserId) { it.copy(isSubmitting = false, userMessage = error.toRemoteUserMessage("Vérifie le nom de ton dragon.")) }
                    }
            }
        }

        private fun updatePersonalMutation(expectedUserId: Long, transform: (NestUiState) -> NestUiState) {
            _uiState.update { if (it.accountUserId == expectedUserId && it.hasRemoteSession) transform(it) else it }
        }

        private suspend fun refreshPersonalCompanions(expectedUserId: Long) {
            if (_uiState.value.accountUserId != expectedUserId || !_uiState.value.hasRemoteSession) return
            _uiState.update {
                if (it.accountUserId != expectedUserId) it
                else it.copy(personalCompanionsLoading = true, personalCompanionMessage = null)
            }
            nestRepository.loadPersonalCompanions()
                .onSuccess { companions ->
                    if (companions.companions.any { it.ownerUserId != expectedUserId }) return@onSuccess
                    _uiState.update {
                        if (it.accountUserId != expectedUserId) it
                        else it.copy(personalCompanions = companions, personalCompanionsLoaded = true, personalCompanionsLoading = false, personalCompanionMessage = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        if (it.accountUserId != expectedUserId) it
                        else it.copy(personalCompanionsLoaded = false, personalCompanionsLoading = false, personalCompanionMessage = error.toRemoteUserMessage("Impossible de charger tes compagnons."))
                    }
                }
        }

        fun evolveEgg(eggId: Long) {
            val currentState = _uiState.value
            if (currentState.isSubmitting) return

            val egg = currentState.eggs?.eggs.orEmpty().firstOrNull { it.id == eggId }
            val actionState = eggEvolutionActionState(egg, currentState.inventory)
            if (!actionState.enabled) {
                _uiState.update { it.copy(userMessage = actionState.requirementsLabel) }
                return
            }

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                nestRepository
                    .evolveEgg(eggId)
                    .onSuccess { result ->
                        if (result.hatched) {
                            val snapshotResult = nestRepository.loadSnapshot()
                            val celebration =
                                hatchingCelebrationFromEvolution(
                                    eventId = nextHatchingEventId++,
                                    result = result,
                                    refreshedDragons = snapshotResult.getOrNull()?.dragons,
                                )
                            _uiState.update { state ->
                                val refreshedState =
                                    snapshotResult
                                        .getOrNull()
                                        ?.let { snapshot -> state.withSnapshot(snapshot) }
                                        ?: state
                                refreshedState.copy(
                                    isSubmitting = false,
                                    isLoading = false,
                                    inventory = snapshotResult.getOrNull()?.inventory ?: result.inventory,
                                    hatchingCelebration = celebration,
                                    userMessage = null,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    userMessage = "L'œuf évolue vers ${result.egg.state}.",
                                    hatchingCelebration = null,
                                )
                            }
                            refresh()
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = error.toRemoteUserMessage("Impossible de faire évoluer cet œuf."),
                                hatchingCelebration = null,
                            )
                        }
                    }
            }
        }

        fun activateDragon(dragonId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                nestRepository
                    .activateDragon(dragonId)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = "${result.activeCompanion.title} est maintenant le compagnon actif.",
                            )
                        }
                        refresh()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = error.toRemoteUserMessage("Impossible de changer de compagnon."),
                            )
                        }
                    }
            }
        }

        fun evolveDragon(dragonId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                nestRepository
                    .evolveDragon(dragonId)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = "${result.dragon.title} atteint le stade ${result.dragon.stage}.",
                            )
                        }
                        refresh()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = error.toRemoteUserMessage("Impossible de faire évoluer ce dragon."),
                            )
                        }
                    }
            }
        }

        fun consumeMessage() {
            _uiState.update { it.copy(userMessage = null) }
        }

        fun consumeStarterCreated() {
            _uiState.update { it.copy(starterCreated = false) }
        }

        fun consumeHatchingCelebration() {
            _uiState.update(::consumeHatchingCelebrationState)
        }
    }

private fun NestUiState.withSnapshot(snapshot: NestSnapshot): NestUiState =
    copy(
        progress = snapshot.progress,
        crystals = snapshot.crystals,
        inventory = snapshot.inventory,
        bestiary = snapshot.bestiary,
        eggs = snapshot.eggs,
        dragons = snapshot.dragons,
        scrolls = snapshot.scrolls,
    )

internal fun hatchingCelebrationFromEvolution(
    eventId: Long,
    result: EggEvolutionDto?,
    refreshedDragons: DragonsDto?,
): NestHatchingCelebration? {
    if (result?.hatched != true) return null
    val dragon = findHatchedDragon(returnedDragon = result.dragon, refreshedDragons = refreshedDragons)
    return NestHatchingCelebration(
        eventId = eventId,
        dragonId = dragon?.id,
        dragonKey = dragon?.dragonKey,
        dragonTitle = dragon?.title,
        dragonStage = dragon?.stage,
    )
}

internal fun consumeHatchingCelebrationState(state: NestUiState): NestUiState =
    state.copy(hatchingCelebration = null)

private fun findHatchedDragon(
    returnedDragon: DragonDto?,
    refreshedDragons: DragonsDto?,
): DragonDto? {
    if (refreshedDragons == null) return returnedDragon
    if (returnedDragon == null) return null
    return refreshedDragons.dragons.firstOrNull { dragon -> dragon.id == returnedDragon.id }
        ?: refreshedDragons.dragons.firstOrNull { dragon -> dragon.dragonKey == returnedDragon.dragonKey }
}
