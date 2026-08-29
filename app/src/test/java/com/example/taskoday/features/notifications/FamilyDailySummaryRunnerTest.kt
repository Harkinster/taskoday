package com.example.taskoday.features.notifications

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyTaskCreateInput
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskOccurrencesRange
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.FamilyTasksToday
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FamilyDailySummaryRunnerTest {
    @Test
    fun `disabled settings do not fetch or publish`() =
        runBlocking {
            val authRepository = FakeAuthRepository()
            val publisher = FakePublisher()
            val runner = FamilyDailySummaryRunner(authRepository, FakeFamilyTasksRepository(), publisher)

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = false))

            assertEquals(FamilyDailySummaryRunResult.Disabled, result)
            assertFalse(authRepository.fetchMeCalled)
            assertEquals(0, publisher.contents.size)
        }

    @Test
    fun `missing token ends cleanly without notification`() =
        runBlocking {
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(accessToken = null),
                    FakeFamilyTasksRepository(),
                    FakePublisher(),
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryRunResult.NoSession, result)
        }

    @Test
    fun `child account does not receive parent summary`() =
        runBlocking {
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(user = user(role = "CHILD")),
                    FakeFamilyTasksRepository(),
                    FakePublisher(),
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryRunResult.NotParent, result)
        }

    @Test
    fun `empty today and overdue does not publish`() =
        runBlocking {
            val publisher = FakePublisher()
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(),
                    FakeFamilyTasksRepository(todayTasks = emptyList(), overdueTasks = emptyList()),
                    publisher,
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryRunResult.NothingToNotify, result)
            assertEquals(0, publisher.contents.size)
        }

    @Test
    fun `today and overdue success publishes real summary`() =
        runBlocking {
            val publisher = FakePublisher()
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(),
                    FakeFamilyTasksRepository(
                        todayTasks =
                            listOf(
                                task(status = FamilyTaskStatus.TODO),
                                task(status = FamilyTaskStatus.VALIDATED),
                            ),
                        overdueTasks = listOf(task(status = FamilyTaskStatus.TODO)),
                    ),
                    publisher,
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertTrue(result is FamilyDailySummaryRunResult.NotificationShown)
            assertEquals("1 tache aujourd'hui · 1 en retard", publisher.contents.single().text)
        }

    @Test
    fun `today failure does not publish partial notification`() =
        runBlocking {
            val publisher = FakePublisher()
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(),
                    FakeFamilyTasksRepository(todayFailure = IOException("offline"), overdueTasks = listOf(task())),
                    publisher,
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryFailureKind.Network, (result as FamilyDailySummaryRunResult.Failure).kind)
            assertEquals(0, publisher.contents.size)
        }

    @Test
    fun `overdue failure does not publish partial notification`() =
        runBlocking {
            val publisher = FakePublisher()
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(),
                    FakeFamilyTasksRepository(todayTasks = listOf(task()), overdueFailure = IOException("offline")),
                    publisher,
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryFailureKind.Network, (result as FamilyDailySummaryRunResult.Failure).kind)
            assertEquals(0, publisher.contents.size)
        }

    @Test
    fun `http 401 is classified as unauthorized without notification`() =
        runBlocking {
            val publisher = FakePublisher()
            val runner =
                FamilyDailySummaryRunner(
                    FakeAuthRepository(fetchMeFailure = httpException(401)),
                    FakeFamilyTasksRepository(todayTasks = listOf(task())),
                    publisher,
                )

            val result = runner.run(FamilyNotificationSettings(dailySummaryEnabled = true))

            assertEquals(FamilyDailySummaryFailureKind.Unauthorized, (result as FamilyDailySummaryRunResult.Failure).kind)
            assertEquals(0, publisher.contents.size)
        }

    private fun task(status: FamilyTaskStatus = FamilyTaskStatus.TODO): FamilyTaskTodayItem =
        FamilyTaskTodayItem(
            taskId = 1L,
            occurrenceId = 10L,
            title = "Tache",
            assignees = emptyList(),
            scheduledDate = "2026-08-26",
            dueDate = "2026-08-26",
            dueTime = null,
            hasDueTime = false,
            dueAt = null,
            status = status,
            validationRequired = false,
            gamificationEnabled = false,
            priority = FamilyTaskPriority.NORMAL,
        )

    private fun user(role: String = "PARENT"): AuthenticatedUser =
        AuthenticatedUser(
            id = 1L,
            email = "parent.test@example.com",
            role = role,
            isActive = true,
            familyIds = listOf(4L),
        )

    private fun httpException(code: Int): HttpException =
        HttpException(
            Response.error<Unit>(
                code,
                "error".toResponseBody("text/plain".toMediaType()),
            ),
        )
}

private class FakePublisher : FamilyDailySummaryNotificationPublisher {
    val contents = mutableListOf<FamilyDailySummaryNotificationContent>()

    override fun publish(content: FamilyDailySummaryNotificationContent): Boolean {
        contents += content
        return true
    }
}

private class FakeAuthRepository(
    private val accessToken: String? = "token",
    private val user: AuthenticatedUser =
        AuthenticatedUser(
            id = 1L,
            email = "parent.test@example.com",
            role = "PARENT",
            isActive = true,
            familyIds = listOf(4L),
        ),
    private val fetchMeFailure: Throwable? = null,
) : AuthRepository {
    var fetchMeCalled = false

    override suspend fun registerParent(
        email: String,
        password: String,
        familyName: String,
        birthDate: String,
        inviteCode: String?,
    ): AuthSession = error("Not used")

    override suspend fun registerChild(
        email: String,
        password: String,
        displayName: String,
        birthDate: String?,
    ): AuthSession = error("Not used")

    override suspend fun login(
        email: String,
        password: String,
    ): AuthSession = error("Not used")

    override suspend fun fetchMe(): AuthenticatedUser {
        fetchMeCalled = true
        fetchMeFailure?.let { throw it }
        return user
    }

    override fun getAccessToken(): String? = accessToken

    override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null

    override fun setActiveChildId(childId: Long) = Unit

    override fun hasParentPin(): Boolean = false

    override fun saveParentPin(pin: String) = Unit

    override fun verifyParentPin(pin: String): Boolean = false

    override fun logout() = Unit

    override fun clearSession() = Unit
}

private class FakeFamilyTasksRepository(
    private val todayTasks: List<FamilyTaskTodayItem> = emptyList(),
    private val overdueTasks: List<FamilyTaskTodayItem> = emptyList(),
    private val todayFailure: Throwable? = null,
    private val overdueFailure: Throwable? = null,
) : FamilyTasksRepository {
    override suspend fun fetchToday(): Result<FamilyTasksToday> =
        todayFailure?.let { Result.failure(it) }
            ?: Result.success(FamilyTasksToday(familyId = 4L, date = "2026-08-26", tasks = todayTasks))

    override suspend fun fetchOccurrences(
        startDate: String,
        endDate: String,
    ): Result<FamilyTaskOccurrencesRange> = error("Not used")

    override suspend fun fetchOverdueOccurrences(): Result<FamilyTaskOccurrencesRange> =
        overdueFailure?.let { Result.failure(it) }
            ?: Result.success(
                FamilyTaskOccurrencesRange(
                    familyId = 4L,
                    startDate = "",
                    endDate = "",
                    occurrences = overdueTasks,
                ),
            )

    override suspend fun fetchTasks(): Result<List<FamilyTaskDefinition>> = error("Not used")

    override suspend fun fetchTask(taskId: Long): Result<FamilyTaskDefinition> = error("Not used")

    override suspend fun fetchMembers(): Result<List<FamilyTaskMember>> = error("Not used")

    override suspend fun createTask(input: FamilyTaskCreateInput): Result<Unit> = error("Not used")

    override suspend fun updateTask(
        taskId: Long,
        input: FamilyTaskCreateInput,
    ): Result<Unit> = error("Not used")

    override suspend fun deleteTask(taskId: Long): Result<Unit> = error("Not used")

    override suspend fun completeOccurrence(occurrenceId: Long): Result<Unit> = error("Not used")

    override suspend fun validateOccurrence(occurrenceId: Long): Result<Unit> = error("Not used")

    override suspend fun reopenOccurrence(occurrenceId: Long): Result<Unit> = error("Not used")
}
