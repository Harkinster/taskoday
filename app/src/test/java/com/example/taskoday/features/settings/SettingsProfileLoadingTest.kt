package com.example.taskoday.features.settings

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.ProfileRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsProfileLoadingTest {
    @Test fun `pending identity cannot save default profile`() = scenario {
        val vm = viewModel()
        runCurrent()
        assertTrue(vm.uiState.value.isProfileLoading)
        assertFalse(vm.uiState.value.canEditProfile)
        assertEquals("…", vm.uiState.value.profileInitials)
        vm.updateProfile("Profil", "2000-01-01")
        runCurrent()
        assertEquals(0, saves)
        identity.completeExceptionally(IllegalStateException("offline"))
        runCurrent()
    }

    @Test fun `loaded parent and child identities unlock only their own form`() = scenario {
        for (role in listOf("PARENT", "CHILD")) {
            identity = CompletableDeferred()
            val vm = viewModel()
            identity.complete(AuthenticatedUser(7, "qa@example.test", role, true, listOf(9), "Loaded $role"))
            runCurrent()
            assertFalse(vm.uiState.value.isProfileLoading)
            assertTrue(vm.uiState.value.canEditProfile)
            assertEquals(if (role == "PARENT") "Loaded PARENT" else "qa@example.test", vm.uiState.value.profileName)
            assertEquals(role == "PARENT", vm.uiState.value.isParentUser)
        }
    }

    @Test fun `failed identity stays read only after loading ends`() = scenario {
        val vm = viewModel()
        identity.completeExceptionally(IllegalStateException("offline"))
        runCurrent()
        assertFalse(vm.uiState.value.isProfileLoading)
        assertFalse(vm.uiState.value.canEditProfile)
        vm.updateProfile("Profil", "2000-01-01")
        runCurrent()
        assertEquals(0, saves)
        assertNotNull(vm.uiState.value.profileErrorMessage)
    }

    @Test fun `local profile cannot save a fabricated server identity`() = scenario {
        hasSession = false
        val vm = viewModel()
        assertFalse(vm.uiState.value.isProfileLoading)
        assertFalse(vm.uiState.value.canEditProfile)
        assertEquals("Profil local", vm.uiState.value.profileName)
        vm.updateProfile("Profil local", "2000-01-01")
        runCurrent()
        assertEquals(0, saves)
    }

    private fun scenario(block: suspend Fixture.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { Fixture { runCurrent() }.block() } finally { Dispatchers.resetMain() }
    }

    private class Fixture(val runCurrent: () -> Unit) {
        var identity = CompletableDeferred<AuthenticatedUser>()
        var hasSession = true
        var saves = 0
        fun viewModel(): SettingsViewModel {
            val auth = object : AuthRepository by unused() {
                override fun getAccessToken() = if (hasSession) "local-test-session" else null
                override suspend fun fetchMe() = identity.await()
                override suspend fun getActiveFamilyId(forceRefresh: Boolean) = 9L
                override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null
                override fun hasParentPin() = false
            }
            val profile = object : ProfileRepository by unused() {
                override suspend fun fetchActiveChildDashboard() = error("No child dashboard")
                override suspend fun updateMyProfile(displayName: String, birthDate: String): Result<Unit> {
                    saves++
                    return Result.success(Unit)
                }
            }
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = emptyList<ParentChild>()
            }
            return SettingsViewModel(auth, profile, children, unused(), unused())
        }
    }
}

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
    T::class.java.classLoader, arrayOf(T::class.java),
) { _, method, _ -> error("Unexpected call: ${method.name}") } as T
