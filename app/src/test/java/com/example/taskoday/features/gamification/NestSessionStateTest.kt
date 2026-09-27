package com.example.taskoday.features.gamification

import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.domain.repository.AuthRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NestSessionStateTest {
    @Test fun `real session never starts in sample mode and missing child is a safe error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val auth = object : AuthRepository by unused() {
                override fun getAccessToken() = "stored-access"
                override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null
            }
            val vm = NestViewModel(NestRepository(unused<NestApi>(), auth))
            assertTrue(vm.uiState.value.hasRemoteSession)
            testScheduler.runCurrent()
            assertTrue(vm.uiState.value.hasRemoteSession)
            assertFalse(vm.uiState.value.isLoading)
            assertNotNull(vm.uiState.value.userMessage)
            assertNull(vm.uiState.value.progress)
            assertNull(vm.uiState.value.dragons)
        } finally { Dispatchers.resetMain() }
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected call: ${method.name}")
    } as T
}
