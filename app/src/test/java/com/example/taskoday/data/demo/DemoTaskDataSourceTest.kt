package com.example.taskoday.data.demo

import com.example.taskoday.core.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTaskDataSourceTest {
    @Test
    fun `demo day contains the visual states needed by the home screen`() = runTest {
        val source = DemoTaskDataSource()

        val items = source.observeTasksForDay(DateTimeUtils.startOfDayMillis()).first()

        assertEquals(4, items.size)
        assertTrue(items.any { it.isCompleted })
        assertTrue(items.any { item -> item.task.dueDate!! < System.currentTimeMillis() && !item.isCompleted })
        assertTrue(items.any { item -> item.task.isDaily })
        assertTrue(items.any { item -> !item.task.isDaily })
    }

    @Test
    fun `demo completion updates the same observable flow`() = runTest {
        val source = DemoTaskDataSource()
        val day = DateTimeUtils.startOfDayMillis()
        val target = source.observeTasksForDay(day).first().first { !it.isCompleted }

        source.setChecked(target.task.id, day, checked = true)

        assertTrue(source.observeTasksForDay(day).first().first { it.task.id == target.task.id }.isCompleted)
        source.setChecked(target.task.id, day, checked = false)
        assertFalse(source.observeTasksForDay(day).first().first { it.task.id == target.task.id }.isCompleted)
    }
}
