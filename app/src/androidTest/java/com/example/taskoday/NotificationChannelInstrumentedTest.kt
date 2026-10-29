package com.example.taskoday

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskoday.features.notifications.*
import org.junit.Assert.*
import org.junit.Test

/** Isolated QA app only. The QA harness restores the system channel switch afterwards. */
class NotificationChannelInstrumentedTest {
    @Test fun disabled_channel_is_not_reported_as_delivered() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".familyqa"))
        check(notificationPermissionGranted(context)) {"QA fixture requires the app-wide notification permission"}
        val manager=context.getSystemService(NotificationManager::class.java)
        val previous=manager.getNotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID)
        try {
            manager.deleteNotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID)
            manager.createNotificationChannel(NotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID,"Organisation familiale",NotificationManager.IMPORTANCE_NONE))
            assertEquals(NotificationManager.IMPORTANCE_NONE,manager.getNotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID).importance)
            assertFalse(AndroidAccountNotificationPublisher(context).publish(NotificationScope(901,91,true,"channel-qa"),NoticeKind.PERSONAL_DUE,1))
            assertFalse(manager.activeNotifications.any {it.tag==ACCOUNT_NOTIFICATION_TAG})
        } finally {
            manager.cancel(ACCOUNT_NOTIFICATION_TAG,4300)
            manager.deleteNotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID)
            if(previous!=null)manager.createNotificationChannel(previous)
        }
    }
}
