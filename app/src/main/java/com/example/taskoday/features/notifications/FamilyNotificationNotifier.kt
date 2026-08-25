package com.example.taskoday.features.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.taskoday.MainActivity
import com.example.taskoday.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface FamilyDailySummaryNotificationPublisher {
    fun publish(content: FamilyDailySummaryNotificationContent): Boolean
}

@Singleton
class AndroidFamilyNotificationNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FamilyDailySummaryNotificationPublisher {
        override fun publish(content: FamilyDailySummaryNotificationContent): Boolean {
            if (!canPostNotifications()) return false

            createChannelIfNeeded()

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    DAILY_SUMMARY_PENDING_INTENT_ID,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat
                    .Builder(context, FAMILY_ORGANIZATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(content.title)
                    .setContentText(content.text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

            return runCatching {
                NotificationManagerCompat
                    .from(context)
                    .notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
                true
            }.getOrDefault(false)
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun createChannelIfNeeded() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val channel =
                NotificationChannel(
                    FAMILY_ORGANIZATION_CHANNEL_ID,
                    "Organisation familiale",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Rappels et resumes des taches du foyer"
                }
            context
                .getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

const val FAMILY_ORGANIZATION_CHANNEL_ID: String = "family_organization"
private const val DAILY_SUMMARY_NOTIFICATION_ID: Int = 4101
private const val DAILY_SUMMARY_PENDING_INTENT_ID: Int = 4102
