package com.example.taskoday.features.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors

/** Compatibility for an old persisted WorkSpec; new work uses the account-scoped worker. */
class FamilyDailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext,AccountNotificationEntryPoint::class.java)
        val scope = entry.notificationRuntime().gate.current() ?: return Result.success()
        return if(entry.accountNotificationRunner().run(scope)==NotificationRunResult.RETRY)Result.retry() else Result.success()
    }
}
