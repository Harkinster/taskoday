package com.example.taskoday.features.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.PointsRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.QuestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class QuestsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val questRepository: QuestRepository,
        private val questsRepository: QuestsRepository,
        private val pointsRepository: PointsRepository,
    ) : ViewModel() {
        private val selectedDay = MutableStateFlow(DateTimeUtils.startOfDayMillis())

        private val _uiState =
            MutableStateFlow(
                QuestsUiState(
                    selectedDayStartMillis = selectedDay.value,
                    dateLabel = DateTimeUtils.formatDayLabel(selectedDay.value),
                    isLoading = true,
                ),
            )
        val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()

        init {
            observeData()
            syncRemoteQuests()
            resolveCanCreateQuest()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeData() {
            viewModelScope.launch {
                selectedDay
                    .flatMapLatest { dayStart ->
                        combine(
                            questRepository.observeQuestsForDay(dayStart),
                            pointsRepository.observeBalance(),
                        ) { quests, points ->
                            Triple(dayStart, quests, points)
                        }
                    }.collect { (dayStart, quests, points) ->
                        _uiState.value =
                            QuestsUiState(
                                selectedDayStartMillis = dayStart,
                                dateLabel = DateTimeUtils.formatDayLabel(dayStart),
                                pointsBalance = points,
                                quests = quests,
                                canCreateQuest = _uiState.value.canCreateQuest,
                                canManageQuests = _uiState.value.canManageQuests,
                                isSubmittingQuest = _uiState.value.isSubmittingQuest,
                                isLoading = false,
                                errorMessage = _uiState.value.errorMessage,
                            )
                    }
            }
        }

        private fun resolveCanCreateQuest() {
            viewModelScope.launch {
                val canManage =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                _uiState.update {
                    it.copy(
                        canCreateQuest = canManage,
                        canManageQuests = canManage,
                    )
                }
            }
        }

        private fun syncRemoteQuests() {
            viewModelScope.launch {
                val result = questsRepository.syncQuests()
                if (result.errorMessage != null) {
                    _uiState.update { it.copy(errorMessage = result.errorMessage) }
                }
            }
        }

        fun goToPreviousDay() {
            selectDay(DateTimeUtils.plusDays(selectedDay.value, -1L))
        }

        fun goToNextDay() {
            selectDay(DateTimeUtils.plusDays(selectedDay.value, 1L))
        }

        fun goToToday() {
            selectDay(DateTimeUtils.startOfDayMillis())
        }

        private fun selectDay(dayStartMillis: Long) {
            val normalized = DateTimeUtils.startOfDayMillis(dayStartMillis)
            selectedDay.value = normalized
            _uiState.update {
                it.copy(
                    selectedDayStartMillis = normalized,
                    dateLabel = DateTimeUtils.formatDayLabel(normalized),
                    isLoading = true,
                )
            }
            syncRemoteQuests()
        }

        fun setQuestCompleted(item: QuestForDay, checked: Boolean) {
            val dayStart = selectedDay.value
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(item.quest.id)
                if (checked && remoteRef?.itemType == PlanningItemType.QUEST) {
                    val remoteResult = questsRepository.completeQuest(item.quest.id)
                    if (remoteResult.isFailure) {
                        val error = remoteResult.exceptionOrNull()
                        _uiState.update {
                            it.copy(
                                errorMessage = error?.toMessage() ?: "Erreur backend.",
                            )
                        }
                        if (error.isForbidden()) return@launch
                    }
                }

                questRepository.setQuestCompletedForDay(item.quest.id, dayStart, checked)
                if (checked) {
                    pointsRepository.grantForQuest(
                        questId = item.quest.id,
                        dayStartMillis = dayStart,
                        points = item.quest.pointsReward,
                        reason = "Quête: ${item.quest.title}",
                    )
                } else {
                    pointsRepository.revokeForQuest(item.quest.id, dayStart)
                }
            }
        }

        fun createQuest(
            title: String,
            description: String?,
            dayPart: DayPart,
            pointsReward: Int,
        ) {
            if (!_uiState.value.canCreateQuest) {
                _uiState.update { it.copy(errorMessage = "Action non autorisee.") }
                return
            }
            if (title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre de la quete est requis.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmittingQuest = true, errorMessage = null) }
                val now = System.currentTimeMillis()
                val draft =
                    Quest(
                        title = title.trim(),
                        description = description?.trim().takeUnless { it.isNullOrBlank() },
                        dayPart = dayPart,
                        pointsReward = pointsReward.coerceIn(1, 20),
                        createdAt = now,
                        updatedAt = now,
                    )

                val result = questsRepository.createQuest(draft)
                if (result.isSuccess) {
                    questRepository.upsertQuest(result.getOrThrow())
                    _uiState.update { it.copy(isSubmittingQuest = false) }
                    return@launch
                }

                val error = result.exceptionOrNull()
                if (error.isForbidden()) {
                    _uiState.update { it.copy(isSubmittingQuest = false, errorMessage = "Action non autorisee.") }
                    return@launch
                }

                questRepository.upsertQuest(draft)
                _uiState.update {
                    it.copy(
                        isSubmittingQuest = false,
                        errorMessage = "Serveur indisponible, quete enregistree en local.",
                    )
                }
            }
        }

        fun updateQuest(
            questForDay: QuestForDay,
            title: String,
            description: String?,
            dayPart: DayPart,
            pointsReward: Int,
        ) {
            if (!_uiState.value.canManageQuests) {
                _uiState.update { it.copy(errorMessage = "Action non autorisee.") }
                return
            }
            if (title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre de la quete est requis.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmittingQuest = true, errorMessage = null) }
                val updatedQuest =
                    questForDay.quest.copy(
                        title = title.trim(),
                        description = description?.trim().takeUnless { it.isNullOrBlank() },
                        dayPart = dayPart,
                        pointsReward = pointsReward.coerceIn(1, 20),
                        updatedAt = System.currentTimeMillis(),
                    )

                if (RemotePlanningIdCodec.decodeQuestId(updatedQuest.id) == null) {
                    questRepository.upsertQuest(updatedQuest)
                    _uiState.update { it.copy(isSubmittingQuest = false) }
                    return@launch
                }

                val remoteResult = questsRepository.updateQuest(updatedQuest)
                if (remoteResult.isSuccess) {
                    questRepository.upsertQuest(remoteResult.getOrThrow())
                    _uiState.update { it.copy(isSubmittingQuest = false) }
                    return@launch
                }

                val error = remoteResult.exceptionOrNull()
                if (error.isForbidden()) {
                    _uiState.update { it.copy(isSubmittingQuest = false, errorMessage = "Action non autorisee.") }
                    return@launch
                }

                questRepository.upsertQuest(updatedQuest)
                _uiState.update {
                    it.copy(
                        isSubmittingQuest = false,
                        errorMessage = "Serveur indisponible, modification locale appliquee.",
                    )
                }
            }
        }

        fun deleteQuest(questForDay: QuestForDay) {
            if (!_uiState.value.canManageQuests) {
                _uiState.update { it.copy(errorMessage = "Action non autorisee.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmittingQuest = true, errorMessage = null) }
                if (RemotePlanningIdCodec.decodeQuestId(questForDay.quest.id) == null) {
                    questRepository.deleteQuest(questForDay.quest.id)
                    _uiState.update { it.copy(isSubmittingQuest = false) }
                    return@launch
                }
                val remoteResult = questsRepository.deleteQuest(questForDay.quest.id)
                if (remoteResult.isFailure) {
                    val error = remoteResult.exceptionOrNull()
                    if (error.isForbidden()) {
                        _uiState.update { it.copy(isSubmittingQuest = false, errorMessage = "Action non autorisee.") }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingQuest = false,
                            errorMessage = "Serveur indisponible, suppression locale appliquee.",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSubmittingQuest = false) }
                }
                questRepository.deleteQuest(questForDay.quest.id)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requete expiree."
        is HttpException ->
            when (code()) {
                403 -> "Action non autorisee."
                else -> "Erreur API (${code()})."
            }

        is IOException -> "Erreur reseau."
        else -> message ?: "Erreur inconnue."
    }

private fun Throwable?.isForbidden(): Boolean = this is HttpException && this.code() == 403
