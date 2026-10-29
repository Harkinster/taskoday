package com.example.taskoday.features.notifications

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface FamilyNotificationPreferences {
    fun getSettings(): FamilyNotificationSettings
    fun saveSettings(settings: FamilyNotificationSettings)
}

@Singleton
class SharedPreferencesFamilyNotificationPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val gate: SharedPreferencesNotificationSessionGate,
    private val gson: Gson,
) : FamilyNotificationPreferences {
    // Retain old global preferences without assigning them to an unverified owner.
    private val prefs = context.getSharedPreferences("taskoday_account_notifications",Context.MODE_PRIVATE)
    override fun getSettings(): FamilyNotificationSettings {
        val owner = gate.owner() ?: return FamilyNotificationSettings()
        return runCatching { gson.fromJson(prefs.getString("user_$owner",null),FamilyNotificationSettings::class.java) }
            .getOrNull()?.let { normalized(it) } ?: FamilyNotificationSettings()
    }
    override fun saveSettings(settings: FamilyNotificationSettings) {
        val owner = gate.owner() ?: return
        prefs.edit().putString("user_$owner",gson.toJson(normalized(settings))).commit()
    }
    private fun normalized(s: FamilyNotificationSettings) = s.copy(hour=s.hour.coerceIn(0,23),minute=s.minute.coerceIn(0,59),
        quietStartMinute=s.quietStartMinute.coerceIn(0,1439),quietEndMinute=s.quietEndMinute.coerceIn(0,1439),enabledSinceMs=s.enabledSinceMs.coerceAtLeast(0))
}

@Singleton
class SharedPreferencesAccountNoticeLedger @Inject constructor(@ApplicationContext context: Context, private val gson: Gson) : AccountNoticeLedger {
    private val prefs = context.getSharedPreferences("taskoday_notification_ledger",Context.MODE_PRIVATE)
    private fun key(scope: NotificationScope) = "${scope.userId}_${scope.familyId}"
    override fun read(scope: NotificationScope): NotificationLedger =
        runCatching { gson.fromJson(prefs.getString(key(scope),null),NotificationLedger::class.java) }.getOrNull() ?: NotificationLedger()
    override fun write(scope: NotificationScope, ledger: NotificationLedger) {
        prefs.edit().putString(key(scope),gson.toJson(ledger)).commit()
    }
}
