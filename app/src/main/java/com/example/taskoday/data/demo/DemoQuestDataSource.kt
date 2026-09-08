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
        private val completedByDay = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

        fun observeActiveQuests(): Flow<List<Quest>> = flowOf(quests())

        fun observeQuestsForDay(dayStartMillis: Long): Flow<List<QuestForDay>> =
            completedByDay.map { completedByDate ->
                val completed = completedByDate[dayStartMillis] ?: defaultCompletedQuestIds()
                quests().map { quest ->
                    QuestForDay(quest = quest, isCompletedForDay = quest.id in completed)
                }
            }

        fun setCompleted(questId: Long, dayStartMillis: Long, completed: Boolean) {
            completedByDay.value = completedByDay.value.toMutableMap().apply {
                val current = (get(dayStartMillis) ?: defaultCompletedQuestIds()).toMutableSet()
                if (completed) current += questId else current -= questId
                put(dayStartMillis, current)
            }
        }

        fun clear() {
            completedByDay.value = emptyMap()
        }

        private fun quests(): List<Quest> {
            val now = System.currentTimeMillis()
            return listOf(
                Quest(
                    id = COMPLETED_QUEST_ID,
                    title = "Préparer le mini défi",
                    description = "Une étape d’aventure déjà accomplie aujourd’hui",
                    emoji = "✓",
                    pointsReward = 5,
                    dayPart = DayPart.APRES_MIDI,
                    createdAt = now,
                    updatedAt = now,
                ),
                Quest(
                    id = ACTIVE_QUEST_ID,
                    title = "Explorer le sentier violet",
                    description = "Une quête courte pour avancer pas à pas",
                    emoji = "✦",
                    pointsReward = 8,
                    dayPart = DayPart.SOIREE,
                    createdAt = now,
                    updatedAt = now,
                ),
                Quest(
                    id = UPCOMING_QUEST_ID,
                    title = "Rassembler les indices",
                    description = "Une nouvelle piste à découvrir bientôt",
                    emoji = "◇",
                    pointsReward = 6,
                    dayPart = DayPart.MATINEE,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        private fun defaultCompletedQuestIds(): Set<Long> = setOf(COMPLETED_QUEST_ID)

        private companion object {
            const val COMPLETED_QUEST_ID = 7101L
            const val ACTIVE_QUEST_ID = 7102L
            const val UPCOMING_QUEST_ID = 7103L
        }
    }
