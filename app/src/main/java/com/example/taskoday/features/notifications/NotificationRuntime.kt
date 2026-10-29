package com.example.taskoday.features.notifications

import android.content.Context
import android.app.NotificationManager
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val ACCOUNT_NOTIFICATION_WORK = "taskoday_account_notification_poll"
const val ACCOUNT_NOTIFICATION_NOW = "taskoday_account_notification_now"
const val ACCOUNT_NOTIFICATION_TAG = "taskoday-account-notices"

@Singleton
class NotificationRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    val gate: SharedPreferencesNotificationSessionGate,
    private val preferences: FamilyNotificationPreferences,
    private val ledger: AccountNoticeLedger,
) {
    private val work get() = WorkManager.getInstance(context)
    fun bind(user: Long, parent: Boolean, family: Long?) {
        if (gate.bind(user,parent,family)) { cancelDelivery();reschedule() }
    }
    fun invalidate() { gate.invalidate();cancelDelivery() }
    fun invalidateIfCurrent(scope: NotificationScope) {
        val cleared = gate.withCurrent(scope) { gate.invalidate();true } ?: false
        if (cleared) cancelDelivery()
    }
    fun familyChanged(family: Long?) {
        if (gate.familyChanged(family)) { cancelDelivery();reschedule() }
    }
    fun save(user: Long, settings: FamilyNotificationSettings): Boolean {
        val saved = gate.withOwner(user) {
            val old = preferences.getSettings()
            val updated = settings.copy(enabledSinceMs=if(settings.enabled && !old.enabled) System.currentTimeMillis() else old.enabledSinceMs)
            preferences.saveSettings(updated)
            if (!updated.enabled) gate.current()?.let { scope -> ledger.write(scope,ledger.read(scope).copy(pending=emptyList())) }
            true
        } ?: false
        if (saved) { cancelVisible();reschedule() }
        return saved
    }
    fun reschedule(settings: FamilyNotificationSettings = preferences.getSettings()) {
        work.cancelUniqueWork(UNIQUE_DAILY_SUMMARY_WORK_NAME)
        val scope = gate.current()
        if (!settings.enabled || scope == null) { cancelDelivery();return }
        val request = PeriodicWorkRequestBuilder<AccountNotificationWorker>(15,TimeUnit.MINUTES)
            .setInputData(scope.data()).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
        work.enqueueUniquePeriodicWork(ACCOUNT_NOTIFICATION_WORK,ExistingPeriodicWorkPolicy.UPDATE,request)
    }
    fun checkNow() {
        val scope = gate.current() ?: return
        if (!preferences.getSettings().enabled) return
        val request = OneTimeWorkRequestBuilder<AccountNotificationWorker>().setInputData(scope.data())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        work.enqueueUniqueWork(ACCOUNT_NOTIFICATION_NOW,ExistingWorkPolicy.REPLACE,request)
    }
    fun cancelDelivery() {
        work.cancelUniqueWork(UNIQUE_DAILY_SUMMARY_WORK_NAME)
        work.cancelUniqueWork(ACCOUNT_NOTIFICATION_WORK);work.cancelUniqueWork(ACCOUNT_NOTIFICATION_NOW);cancelVisible()
    }
    private fun cancelVisible() {
        val manager = context.getSystemService(NotificationManager::class.java)
        NoticeKind.entries.forEach { manager.cancel(ACCOUNT_NOTIFICATION_TAG,4300+it.ordinal) }
        manager.cancel(4101)
    }
}

private fun NotificationScope.data() = workDataOf("user" to userId,"family" to familyId,"parent" to parent,"generation" to generation)

class AccountNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(applicationContext,AccountNotificationEntryPoint::class.java)
        val scope = NotificationScope(inputData.getLong("user",0),inputData.getLong("family",0),inputData.getBoolean("parent",false),inputData.getString("generation").orEmpty())
        return when (entry.accountNotificationRunner().run(scope)) {
            NotificationRunResult.RETRY -> Result.retry()
            NotificationRunResult.NO_SESSION, NotificationRunResult.STALE_SESSION -> { entry.notificationRuntime().invalidateIfCurrent(scope);Result.success() }
            else -> Result.success()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AccountNotificationEntryPoint {
    fun accountNotificationRunner(): AccountNotificationRunner
    fun notificationRuntime(): NotificationRuntime
}
