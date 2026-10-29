package com.example.taskoday

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.example.taskoday.features.notifications.*
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Explicit QA bridge: inert during the normal suite; never packaged in the application APK. */
class AccountNotificationsProbeTest {
    @Test fun local_qa_probe() = runBlocking {
        val args=InstrumentationRegistry.getArguments()
        val operation=args.getString("notificationProbe") ?: return@runBlocking
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName=="com.harkinster.taskoday.familyqa")
        val entry=EntryPointAccessors.fromApplication(context,AccountNotificationEntryPoint::class.java)
        val runtime=entry.notificationRuntime()
        val scope=runtime.gate.current()
        val prefs=SharedPreferencesFamilyNotificationPreferences(context,runtime.gate,Gson())
        val ledger=SharedPreferencesAccountNoticeLedger(context,Gson())
        var result="INSPECTED"
        if(operation=="cleanup") {
            runtime.invalidate()
            listOf("taskoday_notification_session","taskoday_account_notifications","taskoday_notification_ledger").forEach {context.deleteSharedPreferences(it)}
            return@runBlocking
        }
        if(operation in setOf("enable","quiet","unquiet","disable")) {
            check(scope!=null)
            val minute=java.time.ZonedDateTime.now().let {it.hour*60+it.minute}
            assertTrue(runtime.save(scope.userId,prefs.getSettings().copy(enabled=operation!="disable",
                quietEnabled=operation=="quiet",quietStartMinute=(minute+1430)%1440,quietEndMinute=(minute+30)%1440)))
        }
        if(operation=="now")runtime.checkNow()
        if(operation=="scan") {
            check(scope!=null)
            result=entry.accountNotificationRunner().run(scope).name
        }
        val work=WorkManager.getInstance(context)
        val jobs=listOf(ACCOUNT_NOTIFICATION_WORK,ACCOUNT_NOTIFICATION_NOW).associateWith {name ->
            work.getWorkInfosForUniqueWork(name).get(15,TimeUnit.SECONDS).map {mapOf("id" to it.id.toString(),"state" to it.state.name,"attempts" to it.runAttemptCount)}
        }
        val active=context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter {it.tag==ACCOUNT_NOTIFICATION_TAG}.map {mapOf("id" to it.id,"postTime" to it.postTime,"text" to it.notification.extras.getCharSequence("android.text").toString(),"visibility" to it.notification.visibility)}
        val state=scope?.let {ledger.read(it)}
        File(context.filesDir,"qa-notification-proof.json").writeText(Gson().toJson(mapOf(
            "operation" to operation,"result" to result,"owner" to scope?.userId,"family" to scope?.familyId,"parent" to scope?.parent,
            "enabled" to prefs.getSettings().enabled,"quiet" to prefs.getSettings().quietEnabled,
            "delivered" to state?.delivered?.keys,"pending" to state?.pending?.map {it.kind.name},
            "cursor" to state?.cursorMs,"work" to jobs,"notifications" to active)))
        assertTrue(active.all {it["visibility"]==android.app.Notification.VISIBILITY_PRIVATE})
    }
}
