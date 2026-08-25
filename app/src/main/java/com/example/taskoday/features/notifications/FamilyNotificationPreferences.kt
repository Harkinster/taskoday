package com.example.taskoday.features.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface FamilyNotificationPreferences {
    fun getSettings(): FamilyNotificationSettings

    fun saveSettings(settings: FamilyNotificationSettings)
}

@Singleton
class SharedPreferencesFamilyNotificationPreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : FamilyNotificationPreferences {
        private val preferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        override fun getSettings(): FamilyNotificationSettings =
            normalizedFamilyNotificationSettings(
                enabled = preferences.getBoolean(KEY_DAILY_SUMMARY_ENABLED, false),
                hour = preferences.getInt(KEY_DAILY_SUMMARY_HOUR, DEFAULT_DAILY_SUMMARY_HOUR),
                minute = preferences.getInt(KEY_DAILY_SUMMARY_MINUTE, DEFAULT_DAILY_SUMMARY_MINUTE),
            )

        override fun saveSettings(settings: FamilyNotificationSettings) {
            val normalized =
                normalizedFamilyNotificationSettings(
                    enabled = settings.dailySummaryEnabled,
                    hour = settings.hour,
                    minute = settings.minute,
                )
            preferences
                .edit()
                .putBoolean(KEY_DAILY_SUMMARY_ENABLED, normalized.dailySummaryEnabled)
                .putInt(KEY_DAILY_SUMMARY_HOUR, normalized.hour)
                .putInt(KEY_DAILY_SUMMARY_MINUTE, normalized.minute)
                .apply()
        }

        private companion object {
            const val PREFS_NAME = "taskoday_family_notifications"
            const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
            const val KEY_DAILY_SUMMARY_HOUR = "daily_summary_hour"
            const val KEY_DAILY_SUMMARY_MINUTE = "daily_summary_minute"
        }
    }
