package com.example.taskoday.data.demo

import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.QuestForDay
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class DemoQuestDataSource
    @Inject
    constructor() {
        private val completedByDay = MutableStateFlow<Set<Long>>(emptySet())

        fun observeActiveQuests(): Flow<List<Quest>> = flowOf(listOf(quest()))

        fun observeQuestsForDay(dayStartMillis: Long): Flow<List<QuestForDay>> =
            completedByDay.map { completed ->
                listOf(QuestForDay(quest = quest(), isCompletedForDay = QUEST_ID in completed))
            }

        fun setCompleted(completed: Boolean) {
            completedByDay.value = if (completed) setOf(QUEST_ID) else emptySet()
        }

        fun clear() {
            completedByDay.value = emptySet()
        }

        private fun quest(): Quest {
            val now = System.currentTimeMillis()
            return Quest(
                id = QUEST_ID,
                title = "Préparer le mini défi",
                description = "Une étape d’aventure simple pour aujourd’hui",
                emoji = "✦",
                pointsReward = 5,
                dayPart = DayPart.APRES_MIDI,
                createdAt = now,
                updatedAt = now,
            )
        }

        private companion object {
            const val QUEST_ID = 7101L
        }
    }
