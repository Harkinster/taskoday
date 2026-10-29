package com.example.taskoday.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class FamilyNotificationsUiState(
    val owner: Long? = null,
    val generation: String = "",
    val parent: Boolean = false,
    val hasFamily: Boolean = false,
    val settings: FamilyNotificationSettings = FamilyNotificationSettings(),
    val isBusy: Boolean = true,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class FamilyNotificationsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val preferences: FamilyNotificationPreferences,
    private val runtime: NotificationRuntime,
) : ViewModel() {
    private val state = MutableStateFlow(FamilyNotificationsUiState())
    val uiState: StateFlow<FamilyNotificationsUiState> = state.asStateFlow()
    init { refresh() }
    fun refresh() {
        val token = auth.getAccessToken()
        state.value = FamilyNotificationsUiState()
        viewModelScope.launch {
            try {
                check(!token.isNullOrBlank())
                val me = auth.fetchMe()
                val family = auth.getActiveFamilyId()?.takeIf { it in me.familyIds }
                check(auth.getAccessToken()==token)
                runtime.bind(me.id,me.role.equals("PARENT",true),family)
                state.value = FamilyNotificationsUiState(me.id,runtime.gate.generation(),me.role.equals("PARENT",true),family!=null,preferences.getSettings(),false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.value=FamilyNotificationsUiState(isBusy=false,errorMessage="Impossible de vérifier ce compte. Reconnecte-toi ou réessaie avec une connexion.") }
        }
    }
    private fun valid(generation: String = state.value.generation): Boolean =
        !state.value.isBusy && state.value.owner!=null && runtime.gate.owner()==state.value.owner && runtime.gate.generation()==generation
    private fun update(generation: String = state.value.generation, change: (FamilyNotificationSettings)->FamilyNotificationSettings) {
        if (!valid(generation)) return
        val next = change(preferences.getSettings())
        if (next.quietEnabled && next.quietStartMinute==next.quietEndMinute) {
            state.update { it.copy(errorMessage="Choisis des heures de début et de fin différentes.",successMessage=null) };return
        }
        if (runtime.save(requireNotNull(state.value.owner),next)) state.update { it.copy(settings=preferences.getSettings(),errorMessage=null,successMessage="Préférences enregistrées pour ce compte.") }
    }
    fun setEnabled(enabled: Boolean, generation: String = state.value.generation) = update(generation) { it.copy(enabled=enabled) }
    fun category(kind: NoticeKind, enabled: Boolean) = update { when(kind) {
        NoticeKind.PERSONAL_DUE -> it.copy(personalDue=enabled)
        NoticeKind.IMPORTANT_OVERDUE -> it.copy(importantOverdue=enabled)
        NoticeKind.MISSION_VALIDATED,NoticeKind.MISSION_RETURNED -> it.copy(missionDecision=enabled)
        NoticeKind.VALIDATION_NEEDED -> if(state.value.parent) it.copy(validationNeeded=enabled) else it
        NoticeKind.DAILY_SUMMARY -> if(state.value.parent) it.copy(dailySummaryEnabled=enabled) else it
    } }
    fun setQuiet(enabled: Boolean) = update { it.copy(quietEnabled=enabled) }
    fun quietTime(start: Boolean,hour: Int,minute: Int) = update {
        val value=hour.coerceIn(0,23)*60+minute.coerceIn(0,59)
        if(start)it.copy(quietStartMinute=value) else it.copy(quietEndMinute=value)
    }
    fun summaryTime(hour: Int,minute: Int) = update { it.copy(hour=hour.coerceIn(0,23),minute=minute.coerceIn(0,59)) }
    fun permissionDenied(generation: String = state.value.generation) {
        if (!valid(generation)) return
        setEnabled(false,generation)
        state.update { it.copy(errorMessage="Autorise les notifications dans Android pour les activer ici.",successMessage=null) }
    }
    fun checkNow() {
        if(!valid() || !state.value.settings.enabled) return
        runtime.checkNow();state.update { it.copy(successMessage="Vérification planifiée dès qu’une connexion est disponible.",errorMessage=null) }
    }
}
