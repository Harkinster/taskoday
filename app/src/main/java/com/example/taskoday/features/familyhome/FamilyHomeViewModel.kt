package com.example.taskoday.features.familyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyHomeViewModel
    @Inject
    constructor(
        private val familyTasksRepository: FamilyTasksRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FamilyHomeUiState())
        val uiState: StateFlow<FamilyHomeUiState> = _uiState.asStateFlow()

        private var activeWeekWindow = familyTaskWeekWindowContaining(LocalDate.now())
        private var selectedWeekDate = LocalDate.now()
        private var weekOccurrences: List<FamilyTaskTodayItem> = emptyList()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                when (_uiState.value.mode) {
                    FamilyHomeMode.TODAY -> loadToday(showLoading = true)
                    FamilyHomeMode.WEEK ->
                        loadWeek(
                            showLoading = true,
                            weekStartDate = activeWeekWindow.startDate,
                            preferredSelectedDate = selectedWeekDate,
                        )
                }
            }
        }

        fun showToday() {
            if (_uiState.value.mode == FamilyHomeMode.TODAY) return
            viewModelScope.launch {
                _uiState.update { it.copy(mode = FamilyHomeMode.TODAY) }
                loadToday(showLoading = true)
            }
        }

        fun showWeek() {
            if (_uiState.value.mode == FamilyHomeMode.WEEK) return
            val today = resolvedTodayDate()
            activeWeekWindow = familyTaskWeekWindowContaining(today)
            selectedWeekDate = today
            viewModelScope.launch {
                _uiState.update { it.copy(mode = FamilyHomeMode.WEEK) }
                loadWeek(
                    showLoading = true,
                    weekStartDate = activeWeekWindow.startDate,
                    preferredSelectedDate = selectedWeekDate,
                )
            }
        }

        fun previousWeek() {
            navigateWeek(offset = -1L)
        }

        fun nextWeek() {
            navigateWeek(offset = 1L)
        }

        fun showCurrentWeek() {
            val today = resolvedTodayDate()
            activeWeekWindow = familyTaskWeekWindowContaining(today)
            selectedWeekDate = today
            viewModelScope.launch {
                loadWeek(
                    showLoading = true,
                    weekStartDate = activeWeekWindow.startDate,
                    preferredSelectedDate = selectedWeekDate,
                )
            }
        }

        fun selectWeekDate(value: String) {
            val date = parseFamilyTaskDateInput(value) ?: return
            val window = familyTaskWeekWindowContaining(activeWeekWindow.startDate)
            if (date !in window.startDate..window.endDate) return
            selectedWeekDate = date
            applyWeekSelection()
        }

        fun clearMessages() {
            _uiState.update { it.copy(errorMessage = null, secondaryErrorMessage = null, userMessage = null) }
        }

        fun runQuickAction(task: FamilyTaskTodayItem) {
            val action = quickActionFor(task) ?: return
            if (!canRunQuickAction(task)) return

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        actingOccurrenceId = task.occurrenceId,
                        errorMessage = null,
                        userMessage = null,
                    )
                }
                val result =
                    when (action) {
                        FamilyTaskQuickAction.COMPLETE -> familyTasksRepository.completeOccurrence(task.occurrenceId)
                        FamilyTaskQuickAction.VALIDATE -> familyTasksRepository.validateOccurrence(task.occurrenceId)
                        FamilyTaskQuickAction.REOPEN -> familyTasksRepository.reopenOccurrence(task.occurrenceId)
                    }
                result
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                actingOccurrenceId = null,
                                userMessage = action.successMessage(),
                            )
                        }
                        when (_uiState.value.mode) {
                            FamilyHomeMode.TODAY -> loadToday(showLoading = false)
                            FamilyHomeMode.WEEK ->
                                loadWeek(
                                    showLoading = false,
                                    weekStartDate = activeWeekWindow.startDate,
                                    preferredSelectedDate = selectedWeekDate,
                                )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                actingOccurrenceId = null,
                                errorMessage = throwable.toRemoteUserMessage("Action impossible pour le moment."),
                            )
                        }
                    }
            }
        }

        private fun navigateWeek(offset: Long) {
            if (_uiState.value.mode != FamilyHomeMode.WEEK) return
            val nextStartDate = activeWeekWindow.startDate.plusWeeks(offset)
            viewModelScope.launch {
                loadWeek(
                    showLoading = true,
                    weekStartDate = nextStartDate,
                    preferredSelectedDate = null,
                )
            }
        }

        private suspend fun loadToday(showLoading: Boolean) {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            familyTasksRepository
                .fetchToday()
                .onSuccess { today ->
                    val todayReference = parseFamilyTaskDateInput(today.date.orEmpty()) ?: resolvedTodayDate()
                    val todayDate = today.date ?: todayReference.toString()
                    val upcomingWindow = familyTaskUpcomingWindow(todayReference)
                    val secondaryErrors = mutableListOf<String>()
                    val overdueTasks =
                        familyTasksRepository
                            .fetchOverdueOccurrences()
                            .onFailure {
                                secondaryErrors += "Les tâches en retard n'ont pas pu être chargées."
                            }
                            .getOrNull()
                            ?.occurrences
                            .orEmpty()
                    val upcomingTasks =
                        familyTasksRepository
                            .fetchOccurrences(
                                startDate = upcomingWindow.startDate.toString(),
                                endDate = upcomingWindow.endDate.toString(),
                            ).onFailure {
                                secondaryErrors += "Les tâches à venir n'ont pas pu être chargées."
                            }
                            .getOrNull()
                            ?.occurrences
                            .orEmpty()
                    val sections = buildFamilyTaskSections(today.tasks)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mode = FamilyHomeMode.TODAY,
                            familyId = today.familyId,
                            todayDate = todayDate,
                            dateLabel = formatFamilyHomeDateLabel(todayDate, fallback = todayReference),
                            sections = sections,
                            totalTasks = today.tasks.size,
                            completedTasks = today.tasks.count { task -> task.status.countsAsDone },
                            pendingValidationTasks = familyTaskPendingValidationCount(today.tasks),
                            overdueTasks = buildFamilyTaskOverduePreview(overdueTasks),
                            overdueTotalTasks = overdueTasks.size,
                            upcomingSections =
                                buildFamilyTaskUpcomingSections(
                                    tasks = upcomingTasks,
                                    today = todayReference,
                                ),
                            upcomingTotalTasks = upcomingTasks.size,
                            upcomingStartDate = upcomingWindow.startDate.toString(),
                            upcomingEndDate = upcomingWindow.endDate.toString(),
                            hasMoreUpcomingTasks =
                                hasMoreFamilyTaskUpcomingPreview(
                                    tasks = upcomingTasks,
                                    today = todayReference,
                                ),
                            isWeekEmpty = false,
                            errorMessage = null,
                            secondaryErrorMessage = secondaryErrors.joinToString(" ").takeIf { message -> message.isNotBlank() },
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mode = FamilyHomeMode.TODAY,
                            todayDate = null,
                            dateLabel = formatFamilyHomeDateLabel(null),
                            sections = emptyList(),
                            totalTasks = 0,
                            completedTasks = 0,
                            pendingValidationTasks = 0,
                            overdueTasks = emptyList(),
                            overdueTotalTasks = 0,
                            upcomingSections = emptyList(),
                            upcomingTotalTasks = 0,
                            upcomingStartDate = null,
                            upcomingEndDate = null,
                            hasMoreUpcomingTasks = false,
                            isWeekEmpty = false,
                            errorMessage = throwable.toRemoteUserMessage("Impossible de charger Ma maison."),
                            secondaryErrorMessage = null,
                        )
                    }
                }
        }

        private suspend fun loadWeek(
            showLoading: Boolean,
            weekStartDate: LocalDate,
            preferredSelectedDate: LocalDate?,
        ) {
            val requestedWindow = familyTaskWeekWindowContaining(weekStartDate)
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, mode = FamilyHomeMode.WEEK, errorMessage = null) }
            }
            familyTasksRepository
                .fetchOccurrences(
                    startDate = requestedWindow.startDate.toString(),
                    endDate = requestedWindow.endDate.toString(),
                ).onSuccess { range ->
                    activeWeekWindow =
                        familyTaskWeekWindowContaining(
                            parseFamilyTaskDateInput(range.startDate) ?: requestedWindow.startDate,
                        )
                    selectedWeekDate =
                        resolveFamilyTaskSelectedWeekDate(
                            preferredDate = preferredSelectedDate,
                            weekStartDate = activeWeekWindow.startDate,
                            today = resolvedTodayDate(),
                        )
                    weekOccurrences = range.occurrences
                    applyWeekSelection(
                        familyId = range.familyId,
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onFailure { throwable ->
                    activeWeekWindow = requestedWindow
                    weekOccurrences = emptyList()
                    selectedWeekDate =
                        resolveFamilyTaskSelectedWeekDate(
                            preferredDate = preferredSelectedDate,
                            weekStartDate = activeWeekWindow.startDate,
                            today = resolvedTodayDate(),
                        )
                    applyWeekSelection(
                        familyId = _uiState.value.familyId,
                        isLoading = false,
                        errorMessage = throwable.toRemoteUserMessage("Impossible de charger la semaine."),
                    )
                }
        }

        private fun applyWeekSelection(
            familyId: Long? = _uiState.value.familyId,
            isLoading: Boolean = _uiState.value.isLoading,
            errorMessage: String? = _uiState.value.errorMessage,
        ) {
            val today = resolvedTodayDate()
            val selectedTasks = familyTasksForDate(tasks = weekOccurrences, date = selectedWeekDate)
            val sections = buildFamilyTaskSections(selectedTasks)
            _uiState.update {
                it.copy(
                    isLoading = isLoading,
                    mode = FamilyHomeMode.WEEK,
                    familyId = familyId,
                    dateLabel = formatFamilyHomeDateLabel(today.toString()),
                    sections = sections,
                    totalTasks = selectedTasks.size,
                    completedTasks = selectedTasks.count { task -> task.status.countsAsDone },
                    pendingValidationTasks = familyTaskPendingValidationCount(selectedTasks),
                    overdueTasks = emptyList(),
                    overdueTotalTasks = 0,
                    upcomingSections = emptyList(),
                    upcomingTotalTasks = 0,
                    upcomingStartDate = null,
                    upcomingEndDate = null,
                    hasMoreUpcomingTasks = false,
                    weekRangeLabel =
                        familyTaskWeekRangeLabel(
                            startDate = activeWeekWindow.startDate,
                            endDate = activeWeekWindow.endDate,
                        ),
                    selectedWeekDateLabel = formatFamilyHomeSelectedDayLabel(selectedWeekDate),
                    selectedWeekDate = selectedWeekDate.toString(),
                    weekDays =
                        buildFamilyTaskWeekDaySummaries(
                            days = activeWeekWindow.days,
                            tasks = weekOccurrences,
                            selectedDate = selectedWeekDate,
                            today = today,
                        ),
                    isCurrentWeek = today in activeWeekWindow.startDate..activeWeekWindow.endDate,
                    isWeekEmpty = familyTaskWeekIsEmpty(weekOccurrences),
                    errorMessage = errorMessage,
                    secondaryErrorMessage = null,
                )
            }
        }

        private fun resolvedTodayDate(): LocalDate =
            parseFamilyTaskDateInput(_uiState.value.todayDate.orEmpty()) ?: LocalDate.now()
    }

private fun FamilyTaskQuickAction.successMessage(): String =
    when (this) {
        FamilyTaskQuickAction.COMPLETE -> "Tâche terminée."
        FamilyTaskQuickAction.VALIDATE -> "Tâche validée."
        FamilyTaskQuickAction.REOPEN -> "Tâche rouverte."
    }
