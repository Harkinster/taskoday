package com.example.taskoday.features.parent

import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import java.lang.reflect.Proxy
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreePlanningTest {
    @Test fun `parent creates beyond all former commercial task caps`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = PlanningFake()
            val vm = ParentPlanningViewModel(repository, sync())
            repeat(2) {
                vm.createRoutine("Routine", null, DayPart.MATIN, setOf(1, 3), 1)
                vm.createMission("Mission", null, DayPart.MATIN, LocalDate.now(), 2)
                vm.createQuest("Quête", null, DayPart.MATIN, 3)
            }
            assertEquals(7, repository.routines)
            assertEquals(5, repository.missions)
            assertEquals(3, repository.quests)
            assertNull(vm.uiState.value.errorMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `free access preserves parent permission child ownership and validation`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val child = PlanningFake(parent = false)
            val denied = ParentPlanningViewModel(child, sync())
            denied.selectChild(1)
            denied.createQuest("Quête", null, DayPart.MATIN, 3)
            assertEquals(1, child.quests)
            assertNull(denied.uiState.value.selectedChildId)
            val parent = PlanningFake()
            val vm = ParentPlanningViewModel(parent, sync())
            vm.selectChild(999)
            assertEquals(1L, vm.uiState.value.selectedChildId)
            vm.createRoutine(" ", null, DayPart.MATIN, emptySet(), 1)
            assertEquals(5, parent.routines)
            assertNotNull(vm.uiState.value.errorMessage)
        } finally { Dispatchers.resetMain() }
    }

    private class PlanningFake(val parent: Boolean = true) : ParentPlanningRepository {
        var routines = 5; var missions = 3; var quests = 1
        override suspend fun isCurrentUserParent() = parent
        override suspend fun fetchChildren() = listOf(ParentChild(1, "QA Child", "qa@example.test"))
        override suspend fun getSelectedChildId() = 1L
        override fun setSelectedChildId(childId: Long) = Unit
        override suspend fun createRoutine(childId: Long, title: String, description: String?, dayPart: DayPart, selectedWeekdays: Set<Int>, points: Int) { routines++ }
        override suspend fun createMission(childId: Long, title: String, description: String?, dayPart: DayPart, scheduledDate: LocalDate, points: Int) { missions++ }
        override suspend fun createQuest(childId: Long, title: String, description: String?, dayPart: DayPart, points: Int) { quests++ }
    }
    private fun sync() = object : PlanningSyncRepository by unused() {
        override suspend fun syncDay(dayStartMillis: Long) = PlanningSyncResult(true)
    }
    @Suppress("UNCHECKED_CAST") private inline fun <reified T> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ -> error(method.name) } as T
}
