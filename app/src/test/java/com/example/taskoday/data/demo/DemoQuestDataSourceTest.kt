package com.example.taskoday.data.demo

import com.example.taskoday.core.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoQuestDataSourceTest {
    @Test
    fun `demo quests expose completed and active states`() = runTest {
        val source = DemoQuestDataSource()
        val quests = source.observeQuestsForDay(DateTimeUtils.startOfDayMillis()).first()

        assertEquals(3, quests.size)
        assertTrue(quests.any { it.isCompletedForDay })
        assertTrue(quests.any { !it.isCompletedForDay })
    }

    @Test
    fun `demo quest completion updates the observable flow`() = runTest {
        val source = DemoQuestDataSource()
        val day = DateTimeUtils.startOfDayMillis()
        val target = source.observeQuestsForDay(day).first().first { !it.isCompletedForDay }

        source.setCompleted(target.quest.id, day, completed = true)
        assertTrue(source.observeQuestsForDay(day).first().first { it.quest.id == target.quest.id }.isCompletedForDay)

        source.setCompleted(target.quest.id, day, completed = false)
        assertFalse(source.observeQuestsForDay(day).first().first { it.quest.id == target.quest.id }.isCompletedForDay)
    }
}
