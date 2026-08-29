package com.example.taskoday.features.notifications

import com.example.taskoday.data.remote.auth.SessionEventBus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class FamilyDailySummaryRunner
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val familyTasksRepository: FamilyTasksRepository,
        private val publisher: FamilyDailySummaryNotificationPublisher,
        private val sessionEventBus: SessionEventBus = SessionEventBus(),
    ) {
        suspend fun run(settings: FamilyNotificationSettings): FamilyDailySummaryRunResult =
            sessionEventBus.suppressUnauthorizedEvents {
                runWithoutSessionNavigation(settings)
            }

        private suspend fun runWithoutSessionNavigation(
            settings: FamilyNotificationSettings,
        ): FamilyDailySummaryRunResult {
            if (!settings.dailySummaryEnabled) return FamilyDailySummaryRunResult.Disabled
            if (authRepository.getAccessToken().isNullOrBlank()) return FamilyDailySummaryRunResult.NoSession

            val me =
                runCatching { authRepository.fetchMe() }
                    .getOrElse { throwable -> return throwable.toDailySummaryFailure() }
            if (!me.role.equals("PARENT", ignoreCase = true)) return FamilyDailySummaryRunResult.NotParent
            if (me.familyIds.isEmpty()) return FamilyDailySummaryRunResult.NoFamily

            val today =
                familyTasksRepository
                    .fetchToday()
                    .getOrElse { throwable -> return throwable.toDailySummaryFailure() }
            val overdue =
                familyTasksRepository
                    .fetchOverdueOccurrences()
                    .getOrElse { throwable -> return throwable.toDailySummaryFailure() }
            val content =
                buildFamilyDailySummaryNotificationContent(
                    todayTasks = today.tasks,
                    overdueTasks = overdue.occurrences,
                ) ?: return FamilyDailySummaryRunResult.NothingToNotify

            return if (publisher.publish(content)) {
                FamilyDailySummaryRunResult.NotificationShown(content)
            } else {
                FamilyDailySummaryRunResult.NotificationSuppressed
            }
        }
    }

sealed class FamilyDailySummaryRunResult {
    data object Disabled : FamilyDailySummaryRunResult()

    data object NoSession : FamilyDailySummaryRunResult()

    data object NotParent : FamilyDailySummaryRunResult()

    data object NoFamily : FamilyDailySummaryRunResult()

    data object NothingToNotify : FamilyDailySummaryRunResult()

    data object NotificationSuppressed : FamilyDailySummaryRunResult()

    data class NotificationShown(
        val content: FamilyDailySummaryNotificationContent,
    ) : FamilyDailySummaryRunResult()

    data class Failure(
        val kind: FamilyDailySummaryFailureKind,
    ) : FamilyDailySummaryRunResult()

    val shouldScheduleNext: Boolean
        get() = this !is Disabled
}

enum class FamilyDailySummaryFailureKind {
    Unauthorized,
    Network,
    Api,
    Unknown,
}

private fun Throwable.toDailySummaryFailure(): FamilyDailySummaryRunResult.Failure =
    FamilyDailySummaryRunResult.Failure(
        kind =
            when (this) {
                is HttpException -> if (code() == 401) FamilyDailySummaryFailureKind.Unauthorized else FamilyDailySummaryFailureKind.Api
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException,
                is IOException,
                -> FamilyDailySummaryFailureKind.Network

                else -> FamilyDailySummaryFailureKind.Unknown
            },
    )
