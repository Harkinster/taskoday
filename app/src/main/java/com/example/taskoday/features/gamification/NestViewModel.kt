package com.example.taskoday.features.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.remote.dto.BestiaryDto
import com.example.taskoday.data.remote.dto.CrystalBalanceDto
import com.example.taskoday.data.remote.dto.DragonsDto
import com.example.taskoday.data.remote.dto.EggsDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.NestProgressDto
import com.example.taskoday.data.remote.dto.ScrollsDto
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.repository.toRemoteUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val userMessage: String? = null,
)

@HiltViewModel
class NestViewModel
    @Inject
    constructor(
        private val nestRepository: NestRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NestUiState())
        val uiState: StateFlow<NestUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            if (!nestRepository.hasRemoteSession()) {
                _uiState.update { it.copy(hasRemoteSession = false, isLoading = false) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(hasRemoteSession = true, isLoading = true) }
                nestRepository
                    .loadSnapshot()
                    .onSuccess { snapshot ->
                        _uiState.update {
                            it.copy(
                                hasRemoteSession = true,
                                isLoading = false,
                                progress = snapshot.progress,
                                crystals = snapshot.crystals,
                                inventory = snapshot.inventory,
                                bestiary = snapshot.bestiary,
                                eggs = snapshot.eggs,
                                dragons = snapshot.dragons,
                                scrolls = snapshot.scrolls,
                            )
                        }
                    }.onFailure { error ->
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
                        val message =
                            if (result.hatched) {
                                "${result.dragon?.title ?: "Le dragon"} a éclos."
                            } else {
                                "L'œuf évolue vers ${result.egg.state}."
                            }
                        _uiState.update { it.copy(isSubmitting = false, userMessage = message) }
                        refresh()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                userMessage = error.toRemoteUserMessage("Impossible de faire évoluer cet œuf."),
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
    }
