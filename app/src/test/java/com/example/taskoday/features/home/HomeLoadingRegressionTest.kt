package com.example.taskoday.features.home

import androidx.lifecycle.ViewModelStore
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.repository.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeLoadingRegressionTest {
    @Test fun `immediate local flows do not leave discovery loading after sync`() = scenario {
        assertFalse(it.uiState.value.isLoading)
        assertFalse(it.uiState.value.hasRemoteSession)
        assertFalse(it.uiState.value.usingRemoteData)
    }

    @Test fun `changing local day finishes loading even with immediate cached flows`() = scenario {
        it.goToNextDay()
        assertFalse(it.uiState.value.isLoading)
        it.goToPreviousDay()
        assertFalse(it.uiState.value.isLoading)
    }

    private fun scenario(check: (HomeViewModel) -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val tasks = MutableStateFlow(emptyList<TaskForDay>())
            val quests = MutableStateFlow(emptyList<QuestForDay>())
            val auth = stub<AuthRepository> { name -> when (name) {
                "getAccessToken" -> null
                "hasParentPin" -> false
                else -> error("Unexpected auth call: $name")
            } }
            val taskRepository = stub<TaskRepository> { name -> when (name) {
                "observeTasksForDay" -> tasks
                "cleanupOldTaskChecks" -> Unit
                else -> error("Unexpected task call: $name")
            } }
            val vm = HomeViewModel(
                taskRepository,
                stub<QuestRepository> { quests },
                stub<PointsRepository> { MutableStateFlow(0) },
                stub<PlanningSyncRepository> { PlanningSyncResult(usedRemoteData = false) },
                NestRepository(stub<NestApi> { error("No network permitted") }, auth),
                auth,
                stub<ChildrenRepository> { error("No children API permitted") },
                stub<RewardRepository> { error("No rewards API permitted") },
                stub<RoutinesRepository> { error("No routines API permitted") },
            )
            store.put("home", vm)
            runCurrent()
            check(vm)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> stub(crossinline answer: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            answer(method.name)
        } as T
}
