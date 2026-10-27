package com.example.taskoday.features.auth

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.TaskRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.data.demo.DemoModeStore
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Test fun `hung session restore times out without clearing credentials or blocking indefinitely`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stored = FakeAuthRepository(accessToken = "stored-access")
            val repository = object : AuthRepository by stored {
                override suspend fun fetchMe(): AuthenticatedUser {
                    delay(60_000L)
                    error("Restore should have timed out first")
                }
            }
            val vm = AuthViewModel(repository)
            testScheduler.advanceTimeBy(35_001L)
            testScheduler.runCurrent()
            assertFalse(vm.uiState.value.isCheckingSession)
            assertFalse(vm.uiState.value.isAuthenticated)
            assertTrue(vm.uiState.value.canRetrySession)
            assertFalse(stored.sessionCleared)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `restoring session invalidates unscoped projections before exposing authenticated state`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<String>()
            val tasks = Proxy.newProxyInstance(TaskRepository::class.java.classLoader, arrayOf(TaskRepository::class.java)) { _, method, _ ->
                check(method.name == "clearRemoteCache")
                calls += "tasks"
                Unit
            } as TaskRepository
            val quests = Proxy.newProxyInstance(QuestRepository::class.java.classLoader, arrayOf(QuestRepository::class.java)) { _, method, _ ->
                check(method.name == "clearRemoteCache")
                calls += "quests"
                Unit
            } as QuestRepository
            val vm = AuthViewModel(FakeAuthRepository(accessToken = "stored-access"), DemoModeStore(), tasks, quests)
            assertTrue(vm.uiState.value.isAuthenticated)
            assertTrue(calls == listOf("tasks", "quests"))
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `logout clears repository session and local auth state`() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        viewModel.continueInLocalMode()

        viewModel.logout()

        assertTrue(repository.sessionCleared)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.isLocalMode)
        assertFalse(viewModel.uiState.value.isCheckingSession)
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun `network failure during restore preserves session and offers retry`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val repository = FakeAuthRepository(accessToken = "stored-access", fetchMeFailure = IOException("offline"))
                val viewModel = AuthViewModel(repository)

                assertFalse(repository.sessionCleared)
                assertFalse(viewModel.uiState.value.isCheckingSession)
                assertTrue(viewModel.uiState.value.canRetrySession)
                assertFalse(viewModel.uiState.value.isAuthenticated)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `successful restored request authenticates without login state`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val repository = FakeAuthRepository(accessToken = "refreshed-access")
                val viewModel = AuthViewModel(repository)

                assertTrue(viewModel.uiState.value.isAuthenticated)
                assertFalse(viewModel.uiState.value.isCheckingSession)
                assertFalse(viewModel.uiState.value.canRetrySession)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `successful parent login authenticates parent role`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository(loginUserRole = "PARENT")
            val viewModel = AuthViewModel(repository)
            viewModel.login("parent@example.test", "unused-test-password")
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.currentUser?.role == "PARENT")
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `successful child login authenticates child role`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository(loginUserRole = "CHILD")
            val viewModel = AuthViewModel(repository)
            viewModel.login("child@example.test", "unused-test-password")
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.currentUser?.role == "CHILD")
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `login 401 is reported as invalid credentials`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository(loginFailure = httpError(401))
            val viewModel = AuthViewModel(repository)
            viewModel.login("person@example.test", "unused-test-password")
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.errorMessage == "Email ou mot de passe incorrect.")
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `login succeeds but profile 403 remains an access denial`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository(loginUserRole = "PARENT", fetchMeFailure = httpError(403))
            val viewModel = AuthViewModel(repository)
            viewModel.login("parent@example.test", "unused-test-password")
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.errorMessage == "Ce compte n’est pas autorisé à accéder à Taskoday.")
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun `invalid restored session is distinguished from rejected login`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository(accessToken = "stored-access", fetchMeFailure = httpError(401))
            val viewModel = AuthViewModel(repository)
            assertTrue(viewModel.uiState.value.errorMessage == "Session invalide. Reconnectez-vous.")
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `blank login remains a local validation without repository call`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeAuthRepository()
            val viewModel = AuthViewModel(repository)
            viewModel.login("", "")
            assertTrue(viewModel.uiState.value.errorMessage == "Email et mot de passe sont requis.")
            assertTrue(repository.loginCalls == 0)
            assertFalse(viewModel.uiState.value.isAuthenticated)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `offline and timeout login errors remain readable without authentication`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for ((failure, expected) in listOf(
                java.net.UnknownHostException() to "Serveur indisponible. Vérifiez votre connexion.",
                java.net.SocketTimeoutException() to "Connexion au serveur expirée.",
            )) {
                val viewModel = AuthViewModel(FakeAuthRepository(loginFailure = failure))
                viewModel.login("local@example.test", "not-a-real-credential")
                assertTrue(viewModel.uiState.value.errorMessage == expected)
                assertFalse(viewModel.uiState.value.isAuthenticated)
                assertFalse(viewModel.uiState.value.isLoading)
            }
        } finally { Dispatchers.resetMain() }
    }
}

private fun httpError(code: Int) = HttpException(
    Response.error<Any>(code, "".toResponseBody(null)),
)

private class FakeAuthRepository(
    private val accessToken: String? = null,
    private val fetchMeFailure: Throwable? = null,
    private val loginFailure: Throwable? = null,
    private val loginUserRole: String = "PARENT",
) : AuthRepository {
    var sessionCleared: Boolean = false
    var loginCalls: Int = 0

    override suspend fun registerParent(
        displayName: String,
        birthDate: String,
        email: String,
        password: String,
    ): AuthSession = error("Not used")

    override suspend fun registerChild(
        email: String,
        password: String,
        displayName: String,
        birthDate: String?,
    ): AuthSession = error("Not used")

    override suspend fun login(email: String, password: String): AuthSession {
        loginCalls++
        loginFailure?.let { throw it }
        return AuthSession(accessToken = "test-access", tokenType = "bearer", expiresInSeconds = 3600, role = loginUserRole)
    }

    override suspend fun fetchMe(): AuthenticatedUser {
        fetchMeFailure?.let { throw it }
        return AuthenticatedUser(
            id = 1L,
            email = "parent@example.test",
            role = loginUserRole,
            isActive = true,
            familyIds = listOf(7L),
        )
    }

    override fun getAccessToken(): String? = accessToken

    override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null

    override fun setActiveChildId(childId: Long) = Unit

    override fun hasParentPin(): Boolean = false

    override fun saveParentPin(pin: String) = Unit

    override fun verifyParentPin(pin: String): Boolean = false

    override fun logout() {
        sessionCleared = true
    }

    override fun clearSession() {
        sessionCleared = true
    }
}
