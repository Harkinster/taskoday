package com.example.taskoday.features.settings

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildOnboardingRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.PairingRepository
import com.example.taskoday.domain.repository.ProfileRepository
import java.lang.reflect.Proxy
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAccountTest {
    @Test fun `valid email updates displayed identity and preserves parent selection`() = runTest {
        withFixture {
            val vm = viewModel()
            vm.updateEmail("  NEW@example.test  ")
            assertEquals("new@example.test", email)
            assertEquals("new@example.test", vm.uiState.value.profileEmail)
            assertTrue(vm.uiState.value.profileSubtitle.contains("new@example.test"))
            assertEquals("Test Papa", vm.uiState.value.profileName)
            assertEquals(25L, userId)
            assertEquals("PARENT", role)
            assertEquals(listOf(7L, 8L), families)
            assertEquals(8L, familyId)
            assertEquals(29L, childId)
            assertEquals("parent-access", accessToken)
            assertEquals(1, emailCalls)
            assertEquals("Adresse email modifiée.", vm.uiState.value.emailSuccessMessage)
        }
    }

    @Test fun `invalid email is rejected locally without API call`() = runTest {
        withFixture {
            val vm = viewModel()
            vm.updateEmail("bad address")
            assertEquals(0, emailCalls)
            assertEquals("parent@example.test", vm.uiState.value.profileEmail)
            assertTrue(vm.uiState.value.emailErrorMessage.orEmpty().contains("valide"))
        }
    }

    @Test fun `email conflict network server and expired session have safe messages`() = runTest {
        for ((error, expected) in listOf(
            httpError(409) to "déjà utilisée",
            httpError(422) to "invalide",
            UnknownHostException("offline") to "Réseau",
            httpError(502) to "serveur",
            httpError(401) to "Session expirée",
        )) {
            withFixture {
                emailFailure = error
                val vm = viewModel()
                vm.updateEmail("new@example.test")
                assertTrue(vm.uiState.value.emailErrorMessage.orEmpty().contains(expected))
                assertEquals("parent@example.test", vm.uiState.value.profileEmail)
                assertEquals(8L, familyId)
                assertEquals("parent-access", accessToken)
            }
        }
    }

    @Test fun `password change keeps parent token and family and stores no password in state`() = runTest {
        withFixture {
            val vm = viewModel()
            vm.changePassword("old-test-pass", "new-test-pass", "new-test-pass")
            assertEquals(1, passwordCalls)
            assertEquals("Mot de passe modifié.", vm.uiState.value.passwordSuccessMessage)
            assertEquals("parent-access", accessToken)
            assertEquals(8L, familyId)
            assertEquals(29L, childId)
            assertFalse(vm.uiState.value.toString().contains("old-test-pass"))
            assertFalse(vm.uiState.value.toString().contains("new-test-pass"))
            assertFalse(vm.javaClass.declaredFields.any { field ->
                field.isAccessible = true
                (field.get(vm) as? String)?.contains("new-test-pass") == true
            })
        }
    }

    @Test fun `incorrect current password gets a clear error`() = runTest {
        withFixture {
            passwordFailure = httpError(400)
            val vm = viewModel()
            vm.changePassword("wrong-test-pass", "new-test-pass", "new-test-pass")
            assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains("actuel incorrect"))
            assertNull(vm.uiState.value.passwordSuccessMessage)
            assertEquals("parent-access", accessToken)
        }
    }

    @Test fun `password validation rejects missing current short new and mismatch`() = runTest {
        withFixture {
            val vm = viewModel()
            vm.changePassword("", "new-test-pass", "new-test-pass")
            assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains("actuel"))
            vm.changePassword("short", "new-test-pass", "new-test-pass")
            assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains("actuel"))
            vm.changePassword("old-test-pass", "short", "short")
            assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains("8 et 128"))
            vm.changePassword("old-test-pass", "new-test-pass", "different")
            assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains("correspondent pas"))
            assertEquals(0, passwordCalls)
        }
    }

    @Test fun `password network server and expired session errors are safe`() = runTest {
        for ((error, expected) in listOf(
            UnknownHostException("offline") to "Réseau",
            httpError(502) to "serveur",
            httpError(401) to "Session expirée",
            httpError(422) to "nouveau mot de passe",
        )) {
            withFixture {
                passwordFailure = error
                val vm = viewModel()
                vm.changePassword("old-test-pass", "new-test-pass", "new-test-pass")
                assertTrue(vm.uiState.value.passwordErrorMessage.orEmpty().contains(expected))
                assertEquals("parent-access", accessToken)
            }
        }
    }

    @Test fun `authenticated child can change only own account identifiers`() = runTest {
        withFixture {
            role = "CHILD"
            userId = 29L
            families = listOf(8L)
            email = "child@example.test"
            accessToken = "child-access"
            val vm = viewModel()
            vm.updateEmail("child-new@example.test")
            vm.changePassword("old-test-pass", "new-test-pass", "new-test-pass")
            assertEquals("child-new@example.test", vm.uiState.value.profileEmail)
            assertEquals(1, emailCalls)
            assertEquals(1, passwordCalls)
            assertFalse(vm.uiState.value.isParentUser)
            assertEquals(listOf(8L), vm.uiState.value.familyIds)
            assertEquals(8L, familyId)
            assertEquals("child-access", accessToken)
        }
    }

    private suspend fun withFixture(block: suspend Fixture.() -> Unit) {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try { Fixture().block() } finally { Dispatchers.resetMain() }
    }

    private class Fixture {
        var userId = 25L
        var role = "PARENT"
        var email = "parent@example.test"
        var families = listOf(7L, 8L)
        var familyId = 8L
        var childId = 29L
        var accessToken = "parent-access"
        var emailCalls = 0
        var passwordCalls = 0
        var emailFailure: Throwable? = null
        var passwordFailure: Throwable? = null

        fun viewModel(): SettingsViewModel {
            val auth = object : AuthRepository by unused() {
                override suspend fun fetchMe() = AuthenticatedUser(userId, email, role, true, families, if (role == "PARENT") "Test Papa" else "Test Enfant B")
                override fun getAccessToken() = accessToken
                override suspend fun getActiveFamilyId(forceRefresh: Boolean) = familyId
                override suspend fun getActiveChildId(forceRefresh: Boolean) = childId
                override fun hasParentPin() = false
                override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
                    passwordCalls++
                    return passwordFailure?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)
                }
            }
            val profile = object : ProfileRepository by unused() {
                override suspend fun updateMyEmail(email: String): Result<Unit> {
                    emailCalls++
                    emailFailure?.let { return Result.failure(it) }
                    this@Fixture.email = email
                    return Result.success(Unit)
                }
                override suspend fun fetchActiveChildDashboard() = error("No dashboard")
            }
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren() = emptyList<com.example.taskoday.domain.model.ParentChild>()
            }
            return SettingsViewModel(auth, profile, children, unused<ChildOnboardingRepository>(), unused<PairingRepository>())
        }
    }
}

private fun httpError(status: Int): HttpException =
    HttpException(Response.error<Any>(status, "error".toResponseBody("text/plain".toMediaType())))

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected call: ${method.name}")
} as T
