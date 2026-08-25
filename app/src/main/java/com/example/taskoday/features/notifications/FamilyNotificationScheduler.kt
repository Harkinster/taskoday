package com.example.taskoday.features.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface FamilyNotificationScheduler {
    fun scheduleDailySummary(settings: FamilyNotificationSettings? = null)

    fun scheduleNextDailySummary(settings: FamilyNotificationSettings? = null)

    fun cancelDailySummary()

    fun rescheduleDailySummary(settings: FamilyNotificationSettings? = null)
}

@Singleton
class WorkManagerFamilyNotificationScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: FamilyNotificationPreferences,
    ) : FamilyNotificationScheduler {
        private val workManager: WorkManager
            get() = WorkManager.getInstance(context)

        override fun scheduleDailySummary(settings: FamilyNotificationSettings?) {
            enqueueDailySummary(settings = settings ?: preferences.getSettings(), policy = ExistingWorkPolicy.REPLACE)
        }

        override fun scheduleNextDailySummary(settings: FamilyNotificationSettings?) {
            enqueueDailySummary(settings = settings ?: preferences.getSettings(), policy = ExistingWorkPolicy.APPEND_OR_REPLACE)
        }

        override fun cancelDailySummary() {
            workManager.cancelUniqueWork(UNIQUE_DAILY_SUMMARY_WORK_NAME)
        }

        override fun rescheduleDailySummary(settings: FamilyNotificationSettings?) {
            scheduleDailySummary(settings)
        }

        private fun enqueueDailySummary(
            settings: FamilyNotificationSettings,
            policy: ExistingWorkPolicy,
        ) {
            if (!settings.dailySummaryEnabled) {
                cancelDailySummary()
                return
            }

            val delayMillis =
                FamilyNotificationSchedulePolicy.delayUntilNextDailySummaryMillis(
                    now = ZonedDateTime.now(),
                    hour = settings.hour,
                    minute = settings.minute,
                )
            val request =
                OneTimeWorkRequestBuilder<FamilyDailySummaryWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()

            workManager.enqueueUniqueWork(UNIQUE_DAILY_SUMMARY_WORK_NAME, policy, request)
        }
    }

@Singleton
class FamilyNotificationSettingsController
    @Inject
    constructor(
        private val preferences: FamilyNotificationPreferences,
        private val scheduler: FamilyNotificationScheduler,
    ) {
        fun loadSettings(): FamilyNotificationSettings = preferences.getSettings()

        fun enableDailySummary(): FamilyNotificationSettings {
            val updated = preferences.getSettings().copy(dailySummaryEnabled = true)
            preferences.saveSettings(updated)
            scheduler.scheduleDailySummary(updated)
            return updated
        }

        fun disableDailySummary(): FamilyNotificationSettings {
            val updated = preferences.getSettings().copy(dailySummaryEnabled = false)
            preferences.saveSettings(updated)
            scheduler.cancelDailySummary()
            return updated
        }

        fun updateDailySummaryTime(
            hour: Int,
            minute: Int,
        ): FamilyNotificationSettings {
            val updated =
                normalizedFamilyNotificationSettings(
                    enabled = preferences.getSettings().dailySummaryEnabled,
                    hour = hour,
                    minute = minute,
                )
            preferences.saveSettings(updated)
            if (updated.dailySummaryEnabled) {
                scheduler.rescheduleDailySummary(updated)
            }
            return updated
        }
    }

const val UNIQUE_DAILY_SUMMARY_WORK_NAME: String = "taskoday_family_daily_summary"
