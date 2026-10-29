package com.example.taskoday.features.notifications

import javax.inject.Inject
import javax.inject.Singleton

interface FamilyNotificationScheduler {
    fun scheduleDailySummary(settings: FamilyNotificationSettings? = null)
    fun scheduleNextDailySummary(settings: FamilyNotificationSettings? = null)
    fun cancelDailySummary()
    fun rescheduleDailySummary(settings: FamilyNotificationSettings? = null)
}

@Singleton
class WorkManagerFamilyNotificationScheduler @Inject constructor(private val runtime: NotificationRuntime) : FamilyNotificationScheduler {
    override fun scheduleDailySummary(settings: FamilyNotificationSettings?) { if(settings==null)runtime.reschedule() else runtime.reschedule(settings) }
    override fun scheduleNextDailySummary(settings: FamilyNotificationSettings?) = scheduleDailySummary(settings)
    override fun cancelDailySummary() = runtime.cancelDelivery()
    override fun rescheduleDailySummary(settings: FamilyNotificationSettings?) = scheduleDailySummary(settings)
}

@Singleton
class FamilyNotificationSettingsController @Inject constructor(private val preferences: FamilyNotificationPreferences,private val scheduler: FamilyNotificationScheduler) {
    fun loadSettings() = preferences.getSettings()
    fun enableDailySummary(): FamilyNotificationSettings {
        val updated = preferences.getSettings().copy(dailySummaryEnabled=true,enabled=true)
        preferences.saveSettings(updated);scheduler.scheduleDailySummary(updated);return updated
    }
    fun disableDailySummary(): FamilyNotificationSettings {
        val updated = preferences.getSettings().copy(dailySummaryEnabled=false)
        preferences.saveSettings(updated)
        if(updated.enabled)scheduler.rescheduleDailySummary(updated) else scheduler.cancelDailySummary()
        return updated
    }
    fun updateDailySummaryTime(hour: Int,minute: Int): FamilyNotificationSettings {
        val updated = preferences.getSettings().copy(hour=hour.coerceIn(0,23),minute=minute.coerceIn(0,59))
        preferences.saveSettings(updated)
        if(updated.enabled || updated.dailySummaryEnabled)scheduler.rescheduleDailySummary(updated)
        return updated
    }
}
const val UNIQUE_DAILY_SUMMARY_WORK_NAME = "taskoday_family_daily_summary"
