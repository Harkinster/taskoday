package com.example.taskoday.features.notifications

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class FamilyDailySummaryWorker
    constructor(
        appContext: Context,
        params: WorkerParameters,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    FamilyDailySummaryWorkerEntryPoint::class.java,
                )
            val preferences = entryPoint.familyNotificationPreferences()
            val settings = preferences.getSettings()
            val result = entryPoint.familyDailySummaryRunner().run(settings)
            if (result.shouldScheduleNext && preferences.getSettings().dailySummaryEnabled) {
                entryPoint.familyNotificationScheduler().scheduleNextDailySummary()
            }
            return Result.success()
        }
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FamilyDailySummaryWorkerEntryPoint {
    fun familyNotificationPreferences(): FamilyNotificationPreferences

    fun familyDailySummaryRunner(): FamilyDailySummaryRunner

    fun familyNotificationScheduler(): FamilyNotificationScheduler
}
