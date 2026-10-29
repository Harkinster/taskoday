package com.example.taskoday.features.notifications

import android.Manifest
import android.app.*
import android.content.*
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

fun notificationPermissionGranted(context: Context): Boolean =
    (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

fun accountNotificationText(kind: NoticeKind, count: Int): String = when (kind) {
    NoticeKind.PERSONAL_DUE -> if(count==1) "Une de tes tâches arrive à échéance." else "Des tâches personnelles arrivent à échéance."
    NoticeKind.VALIDATION_NEEDED -> if(count==1) "Une mission attend ta validation." else "Des missions attendent ta validation."
    NoticeKind.MISSION_VALIDATED -> if(count==1) "Une de tes missions a été validée." else "Des missions ont été validées."
    NoticeKind.MISSION_RETURNED -> if(count==1) "Une de tes missions est à reprendre." else "Des missions sont à reprendre."
    NoticeKind.IMPORTANT_OVERDUE -> if(count==1) "Une tâche importante est en retard." else "Des tâches importantes sont en retard."
    NoticeKind.DAILY_SUMMARY -> "Ton résumé familial est prêt dans Taskoday."
}

@Singleton
class AndroidAccountNotificationPublisher @Inject constructor(@ApplicationContext private val context: Context) : AccountNotificationPublisher {
    override fun publish(scope: NotificationScope, kind: NoticeKind, count: Int): Boolean {
        if (!notificationPermissionGranted(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT>=26) {
            manager.createNotificationChannel(NotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID,"Organisation familiale",NotificationManager.IMPORTANCE_DEFAULT))
            // A user can block this channel while keeping the app-wide permission.
            // notify() silently drops that message: leave it pending instead of consuming its key.
            if (manager.getNotificationChannel(FAMILY_ORGANIZATION_CHANNEL_ID)?.importance==NotificationManager.IMPORTANCE_NONE) return false
        }
        val intent = PendingIntent.getActivity(context,4300+kind.ordinal,Intent(context,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = NotificationCompat.Builder(context,FAMILY_ORGANIZATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Taskoday").setContentText("Une notification est disponible dans l’application.").build()
        val notification = NotificationCompat.Builder(context,FAMILY_ORGANIZATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Taskoday").setContentText(accountNotificationText(kind,count)).setContentIntent(intent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public).setSilent(true).setOnlyAlertOnce(true)
            .setAutoCancel(true).build()
        return runCatching { manager.notify(ACCOUNT_NOTIFICATION_TAG,4300+kind.ordinal,notification);true }.getOrDefault(false)
    }
}
