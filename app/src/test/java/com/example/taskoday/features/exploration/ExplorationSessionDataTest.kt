package com.example.taskoday.features.exploration

import com.example.taskoday.data.local.SeedData
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ExplorationSessionDataTest {
    @Test fun `real session excludes all seed tasks and objectives`() {
        val seeds = SeedData.tasks(projectIds = listOf(1L, 2L), nowMillis = 0L)
            .mapIndexed { index, task -> TaskForDay(Task(id = index + 1L, title = task.title, createdAt = 0L, updatedAt = 0L), false) }
        val quests = SeedData.quests(nowMillis = 0L)
            .mapIndexed { index, quest -> QuestForDay(Quest(id = index + 1L, title = quest.title, createdAt = 0L, updatedAt = 0L), false) }
        assertTrue(seeds.isNotEmpty())
        assertTrue(quests.isNotEmpty())
        assertTrue(remoteExplorationTasks(seeds).isEmpty())
        assertTrue(remoteExplorationQuests(quests).isEmpty())
        // Seeds remain untouched for offline/preview consumers.
        assertEquals(seeds.size, SeedData.tasks(listOf(1L, 2L), 0L).size)
    }

    @Test fun `real remote routines missions and objectives remain visible`() {
        val routine = TaskForDay(Task(id = RemotePlanningIdCodec.encodeTaskId(PlanningItemType.ROUTINE, 11L), title = "Remote routine", createdAt = 0L, updatedAt = 0L), false)
        val mission = routine.copy(task = routine.task.copy(id = RemotePlanningIdCodec.encodeTaskId(PlanningItemType.MISSION, 12L)))
        val local = routine.copy(task = routine.task.copy(id = 11L))
        assertEquals(listOf(routine, mission), remoteExplorationTasks(listOf(local, routine, mission)))
        val quest = QuestForDay(Quest(id = RemotePlanningIdCodec.encodeQuestId(13L), title = "Remote objective", createdAt = 0L, updatedAt = 0L), false)
        assertEquals(listOf(quest), remoteExplorationQuests(listOf(quest.copy(quest = quest.quest.copy(id = 13L)), quest)))
    }

    @Test fun `unknown negative identifiers cannot masquerade as remote entries`() {
        val unknown = TaskForDay(Task(id = -119L, title = "Unknown", createdAt = 0L, updatedAt = 0L), false)
        assertTrue(remoteExplorationTasks(listOf(unknown)).isEmpty())
    }
}
