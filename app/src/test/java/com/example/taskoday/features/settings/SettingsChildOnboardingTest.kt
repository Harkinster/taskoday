package com.example.taskoday.features.settings

import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildOnboardingRepository
import com.example.taskoday.domain.repository.ChildTemporarySession
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.PairingRepository
import com.example.taskoday.domain.repository.ProfileRepository
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.POST

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsChildOnboardingTest {
    @Test fun `parent session and family are preserved through child creation and refresh`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")

            assertEquals(listOf("register", "generate", "attach:7", "refresh", "discard"), fixture.events.take(5))
            assertEquals("child-access", fixture.generatedWith)
            assertEquals("parent-access", fixture.parentToken)
            assertEquals(7L, fixture.familyId)
            assertEquals(0, fixture.activeChildUpdates)
            assertTrue(vm.uiState.value.isParentUser)
            assertEquals(7L, vm.uiState.value.selectedFamilyId)
            assertEquals("QA New", vm.uiState.value.pairedChildren.single().displayName)
            assertNotNull(vm.uiState.value.childManagementSuccessMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `registration failure leaves parent untouched and does not pair`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(failAt = "register")
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertEquals(listOf("register"), fixture.events)
            assertEquals("parent-access", fixture.parentToken)
            assertTrue(vm.uiState.value.isParentUser)
            assertNotNull(vm.uiState.value.childManagementErrorMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `502 with created child recovers through isolated login and pairs once`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.registerFailures += httpError(502)
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)

            assertEquals(listOf("register", "login", "generate", "attach:7", "refresh", "discard"), fixture.events.take(6))
            assertEquals("recovered-access", fixture.generatedWith)
            assertEquals("parent-access", fixture.parentToken)
            assertEquals(7L, fixture.familyId)
            assertTrue(vm.uiState.value.isParentUser)
            assertEquals(7L, vm.uiState.value.selectedFamilyId)
            assertEquals(0, fixture.activeChildUpdates)
            assertNotNull(vm.uiState.value.childManagementSuccessMessage)
            assertFalse(vm.uiState.value.toString().contains(TEST_PASSWORD))
            assertFalse(vm.javaClass.declaredFields.any { field ->
                field.isAccessible = true
                (field.get(vm) as? String)?.contains(TEST_PASSWORD) == true
            })
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `timeout with created child recovers through login`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.registerFailures += SocketTimeoutException("timeout")
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)
            assertEquals(listOf("register", "login", "generate", "attach:7", "refresh", "discard"), fixture.events.take(6))
            assertEquals("recovered-access", fixture.generatedWith)
            assertEquals("parent-access", fixture.parentToken)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `502 without created child keeps error and permits a manual retry`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.registerFailures += httpError(502)
            fixture.loginFailures += httpError(401)
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)
            assertEquals(listOf("register", "login"), fixture.events)
            assertTrue(vm.uiState.value.childManagementErrorMessage.orEmpty().contains("Inscription incertaine"))
            assertEquals("parent-access", fixture.parentToken)

            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)
            assertEquals(2, fixture.events.count { it == "register" })
            assertEquals(1, fixture.events.count { it == "login" })
            assertEquals(1, fixture.events.count { it.startsWith("attach:") })
            assertNotNull(vm.uiState.value.childManagementSuccessMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `fresh email conflict and validation failure never attempt child login`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for (status in listOf(409, 422)) {
                val fixture = Fixture()
                fixture.registerFailures += httpError(status)
                val vm = fixture.viewModel()
                vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)
                assertEquals(listOf("register"), fixture.events)
                assertNotNull(vm.uiState.value.childManagementErrorMessage)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `conflict after ambiguous failure gets one recovery login and pairs`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.registerFailures += listOf(httpError(502), httpError(409))
            fixture.loginFailures += httpError(401)
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)
            vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD)

            assertEquals(listOf("register", "login", "register", "login", "generate", "attach:7", "refresh", "discard"), fixture.events.take(8))
            assertEquals("recovered-access", fixture.generatedWith)
            assertEquals("parent-access", fixture.parentToken)
            assertEquals(7L, fixture.familyId)
            assertNotNull(vm.uiState.value.childManagementSuccessMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `failed recovery after ambiguous conflict stops automatic login attempts`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.registerFailures += listOf(httpError(502), httpError(409), httpError(409))
            fixture.loginFailures += listOf(httpError(401), httpError(401))
            val vm = fixture.viewModel()
            repeat(3) { vm.createChild("QA New", "new@example.test", "2016-01-01", TEST_PASSWORD) }

            assertEquals(3, fixture.events.count { it == "register" })
            assertEquals(2, fixture.events.count { it == "login" })
            assertFalse(fixture.events.any { it.startsWith("attach:") })
            assertTrue(vm.uiState.value.childManagementErrorMessage.orEmpty().contains("déjà utilisée"))
            assertEquals("parent-access", fixture.parentToken)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `second family is never attached when first family is active`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(familyId = 7L)
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertTrue(fixture.events.contains("attach:7"))
            assertFalse(fixture.events.contains("attach:8"))
            assertEquals(7L, vm.uiState.value.selectedFamilyId)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `pairing generation failure can retry without registering twice`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(failAt = "generate")
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertEquals(listOf("register", "generate"), fixture.events)
            fixture.failAt = null
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertEquals(1, fixture.events.count { it == "register" })
            assertEquals("parent-access", fixture.parentToken)
            assertNotNull(vm.uiState.value.childManagementSuccessMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `attach failure can retry with same code and explicit family`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(failAt = "attach")
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertEquals(listOf("register", "generate", "attach:7"), fixture.events)
            assertTrue(vm.uiState.value.pairedChildren.isEmpty())
            fixture.failAt = null
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertEquals(1, fixture.events.count { it == "register" })
            assertEquals(1, fixture.events.count { it == "generate" })
            assertEquals(2, fixture.events.count { it == "attach:7" })
            assertEquals("parent-access", fixture.parentToken)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `child and absent family cannot start creation`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val child = Fixture(role = "CHILD")
            child.viewModel().createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertTrue(child.events.isEmpty())
            val noFamily = Fixture(familyId = null)
            val vm = noFamily.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertTrue(noFamily.events.isEmpty())
            assertNotNull(vm.uiState.value.childManagementErrorMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `existing free plan child limit prevents account creation`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(existingChildren = 1)
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertTrue(fixture.events.isEmpty())
            assertTrue(vm.uiState.value.childManagementErrorMessage.orEmpty().contains("Limite"))
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `switching from empty family reloads children before allowing creation`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture(familyId = 8L, existingChildren = 2)
            val vm = fixture.viewModel()
            assertTrue(vm.uiState.value.pairedChildren.isEmpty())
            vm.selectFamily(7L)
            assertEquals(2, vm.uiState.value.pairedChildren.size)
            assertTrue(vm.uiState.value.isChildrenListReady)
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertTrue(fixture.events.isEmpty())
            assertNotNull(vm.uiState.value.childManagementErrorMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `parent family switching during registration prevents attach`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture()
            fixture.onGenerate = { fixture.familyId = 8L }
            val vm = fixture.viewModel()
            vm.createChild("QA New", "new@example.test", "2016-01-01", "ValidPass26!")
            assertFalse(fixture.events.any { it.startsWith("attach:") })
            assertEquals("parent-access", fixture.parentToken)
            assertNotNull(vm.uiState.value.childManagementErrorMessage)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `input validation rejects invalid email date and password`() {
        assertNotNull(validateChildAccountInput("Child", "wrong", "2016-01-01", "ValidPass26!"))
        assertNotNull(validateChildAccountInput("Child", "child@example.test", "2099-01-01", "ValidPass26!"))
        assertNotNull(validateChildAccountInput("Child", "child@example.test", "2016-01-01", "short"))
        assertNull(validateChildAccountInput("Child", "child@example.test", "2016-01-01", "ValidPass26!"))
    }

    @Test fun `legacy children creation endpoint is absent`() {
        val posts = ChildrenApi::class.java.methods.mapNotNull { it.getAnnotation(POST::class.java)?.value }
        assertFalse(posts.contains("children"))
    }

    private class Fixture(var failAt: String? = null, val role: String = "PARENT", var familyId: Long? = 7L, val existingChildren: Int = 0) {
        val events = mutableListOf<String>()
        val registerFailures = mutableListOf<Throwable>()
        val loginFailures = mutableListOf<Throwable>()
        var parentToken = "parent-access"
        var generatedWith: String? = null
        var onGenerate: () -> Unit = {}
        var activeChildUpdates = 0
        private var attached = false

        fun viewModel(): SettingsViewModel {
            val auth = object : AuthRepository by unused() {
                override suspend fun fetchMe() = AuthenticatedUser(100L, "parent@example.test", role, true, if (familyId == null) emptyList() else listOf(7L, 8L), "Parent")
                override fun getAccessToken() = parentToken
                override suspend fun getActiveFamilyId(forceRefresh: Boolean) = familyId
                override fun setActiveFamilyId(familyId: Long) { this@Fixture.familyId = familyId }
                override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null
                override fun setActiveChildId(childId: Long) { activeChildUpdates++ }
                override fun hasParentPin() = false
            }
            val children = object : ChildrenRepository by unused() {
                override suspend fun fetchChildren(): List<ParentChild> {
                    if (attached) events += "refresh"
                    return if (attached) {
                        listOf(ParentChild(30L, "QA New", "new@example.test"))
                    } else {
                        if (familyId == 7L) (1..existingChildren).map { ParentChild(it.toLong(), "Existing $it", "existing$it@example.test") } else emptyList()
                    }
                }
            }
            val onboarding = object : ChildOnboardingRepository {
                override suspend fun register(displayName: String, birthDate: String, email: String, password: String): ChildTemporarySession {
                    events += "register"
                    if (registerFailures.isNotEmpty()) throw registerFailures.removeAt(0)
                    if (failAt == "register") error("register failed")
                    return ChildTemporarySession("child-access", "child-refresh")
                }
                override suspend fun login(email: String, password: String): ChildTemporarySession {
                    events += "login"
                    assertEquals(TEST_PASSWORD, password)
                    if (loginFailures.isNotEmpty()) throw loginFailures.removeAt(0)
                    return ChildTemporarySession("recovered-access", "recovered-refresh")
                }
                override suspend fun generateCode(childAccessToken: String): String {
                    events += "generate"
                    generatedWith = childAccessToken
                    onGenerate()
                    if (failAt == "generate") error("generate failed")
                    return "ABC123"
                }
                override suspend fun discard(childRefreshToken: String?) { events += "discard" }
            }
            val pairing = object : PairingRepository by unused() {
                override suspend fun attachChild(code: String, familyId: Long?): Result<Unit> {
                    events += "attach:$familyId"
                    assertEquals("parent-access", parentToken)
                    if (failAt == "attach") return Result.failure(IllegalStateException("attach failed"))
                    assertEquals("ABC123", code)
                    attached = true
                    return Result.success(Unit)
                }
            }
            val profile = object : ProfileRepository by unused() {
                override suspend fun fetchActiveChildDashboard() = error("No active child")
            }
            return SettingsViewModel(auth, profile, children, onboarding, pairing)
        }
    }

}

private const val TEST_PASSWORD = "ValidPass26!"

private fun httpError(status: Int): HttpException =
    HttpException(Response.error<Any>(status, "error".toResponseBody("text/plain".toMediaType())))

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected call: ${method.name}")
} as T
