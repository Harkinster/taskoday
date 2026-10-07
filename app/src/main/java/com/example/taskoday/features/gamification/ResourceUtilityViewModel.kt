package com.example.taskoday.features.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.remote.dto.*
import com.example.taskoday.data.repository.ResourceUtilityRepository
import com.example.taskoday.data.repository.toRemoteUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ResourceUtilityUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val isParent: Boolean = false,
    val familyId: Long? = null,
    val balance: ResourceBalanceDto? = null,
    val offers: List<WishOfferDto> = emptyList(),
    val requests: List<WishRequestDto> = emptyList(),
    val chests: ResourceChestCatalogDto? = null,
    val collection: CollectionDto? = null,
    val chestOpens: List<ChestOpenDto> = emptyList(),
    val chestReveal: ChestOpenDto? = null,
    val message: String? = null,
    val error: String? = null,
)

private data class ResourceUtilityLoadedState(
    val balance: ResourceBalanceDto,
    val offers: List<WishOfferDto>,
    val requests: List<WishRequestDto>,
    val chests: ResourceChestCatalogDto,
    val collection: CollectionDto,
    val chestOpens: List<ChestOpenDto>,
)

@HiltViewModel
class ResourceUtilityViewModel @Inject constructor(private val repository: ResourceUtilityRepository, private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _state = MutableStateFlow(ResourceUtilityUiState())
    val state: StateFlow<ResourceUtilityUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { loadState() }.onSuccess { _state.value = it }
            .onFailure { _state.update { old -> old.copy(loading = false, error = it.toRemoteUserMessage("Impossible de charger les ressources.")) } }
    }

    fun createOffer(title: String, costText: String) = mutate("Offre créée.") { family ->
        val cost = costText.toIntOrNull() ?: error("Indique un coût en Flammèches valide.")
        repository.createOffer(family, title.trim(), cost)
    }
    fun deactivateOffer(id: Long) = mutate("Offre désactivée.") { repository.deactivateOffer(it, id) }
    fun updateOffer(id: Long, title: String, costText: String) = mutate("Offre modifiée.") { family ->
        val cost = costText.toIntOrNull() ?: error("Indique un coût en Flammèches valide.")
        repository.updateOffer(family, id, title.trim(), cost)
    }
    fun requestWish(id: Long) = mutate("Demande envoyée.") { repository.request(it, id) }
    fun cancel(id: Long) = mutate("Demande annulée.") { repository.cancel(it, id) }
    fun approve(id: Long) = mutate("Souhait accepté.") { repository.approve(it, id) }
    fun reject(id: Long) = mutate("Demande refusée.") { repository.reject(it, id) }
    fun obtain(id: Long) = mutate("Souhait obtenu.") { repository.obtain(it, id) }
    fun openChest(type: String) = viewModelScope.launch {
        if (_state.value.submitting) return@launch
        val family = _state.value.familyId ?: return@launch
        val pendingType = savedStateHandle.get<String>(PENDING_CHEST_TYPE_KEY)
        if (pendingType != null && pendingType != type) {
            _state.update { it.copy(error = "Une ouverture attend encore sa confirmation. Réessaie ce même coffre.") }
            return@launch
        }
        val key = savedStateHandle.get<String>(PENDING_CHEST_KEY) ?: java.util.UUID.randomUUID().toString().also {
            savedStateHandle[PENDING_CHEST_KEY] = it
            savedStateHandle[PENDING_CHEST_TYPE_KEY] = type
        }
        _state.update { it.copy(submitting = true, message = null, error = null) }
        runCatching { repository.openChest(family, type, key) }
            .onSuccess { result ->
                savedStateHandle.remove<String>(PENDING_CHEST_KEY)
                savedStateHandle.remove<String>(PENDING_CHEST_TYPE_KEY)
                val loot = chestRevealText(result.drops)
                runCatching { loadState() }.onSuccess { loaded ->
                    _state.value = loaded.copy(chestReveal = result, message = "${result.drops.size} découverte(s) : $loot")
                }.onFailure { error -> _state.update { it.copy(submitting = false, error = error.toRemoteUserMessage("Coffre ouvert, mais les ressources n’ont pas pu être actualisées.")) } }
            }
            .onFailure { error -> _state.update { it.copy(submitting = false, error = error.toRemoteUserMessage("Impossible d’ouvrir ce coffre.")) } }
    }

    fun consumeMessage() = _state.update { it.copy(message = null, error = null) }

    fun dismissChestReveal() = _state.update { it.copy(chestReveal = null) }

    fun showChestReveal(opened: ChestOpenDto) = _state.update { it.copy(chestReveal = opened) }

    private fun mutate(success: String, call: suspend (Long) -> Any) = viewModelScope.launch {
        val family = _state.value.familyId ?: return@launch
        _state.update { it.copy(submitting = true, message = null, error = null) }
        runCatching { call(family) }
            .onSuccess {
                runCatching { loadState() }.onSuccess { loaded -> _state.value = loaded.copy(message = success) }
                    .onFailure { error -> _state.update { it.copy(submitting = false, error = error.toRemoteUserMessage("Action effectuée, actualisation impossible.")) } }
            }
            .onFailure { error -> _state.update { it.copy(submitting = false, error = error.toRemoteUserMessage("Action impossible pour le moment.")) } }
    }

    private suspend fun loadState(): ResourceUtilityUiState {
        val (familyId, parent) = repository.currentContext()
        val loaded = coroutineScope {
            val balance = async { repository.loadBalance(familyId) }
            val offers = async { repository.loadOffers(familyId) }
            val requests = async { repository.loadRequests(familyId) }
            val chests = async { repository.loadChests(familyId) }
            val collection = async { repository.loadCollection(familyId) }
            val chestOpens = async { repository.loadChestOpens(familyId) }
            ResourceUtilityLoadedState(balance.await(), offers.await(), requests.await(), chests.await(), collection.await(), chestOpens.await())
        }
        return ResourceUtilityUiState(
            loading = false,
            isParent = parent,
            familyId = familyId,
            balance = loaded.balance,
            offers = loaded.offers,
            requests = loaded.requests,
            chests = loaded.chests,
            collection = loaded.collection,
            chestOpens = loaded.chestOpens,
        )
    }

    private companion object {
        const val PENDING_CHEST_KEY = "resource_utility_pending_chest_idempotency"
        const val PENDING_CHEST_TYPE_KEY = "resource_utility_pending_chest_type"
    }
}
