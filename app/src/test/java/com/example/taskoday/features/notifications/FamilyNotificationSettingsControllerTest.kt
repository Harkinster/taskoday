package com.example.taskoday.features.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyNotificationSettingsControllerTest {
    @Test
    fun `default settings are disabled at eight`() {
        val preferences = FakeFamilyNotificationPreferences()

        val settings = preferences.getSettings()

        assertFalse(settings.dailySummaryEnabled)
        assertEquals(8, settings.hour)
        assertEquals(0, settings.minute)
    }

    @Test
    fun `activation saves preferences and schedules work`() {
        val preferences = FakeFamilyNotificationPreferences()
        val scheduler = FakeFamilyNotificationScheduler()
        val controller = FamilyNotificationSettingsController(preferences, scheduler)

        val settings = controller.enableDailySummary()

        assertTrue(settings.dailySummaryEnabled)
        assertTrue(preferences.savedSettings.last().dailySummaryEnabled)
        assertEquals(1, scheduler.scheduleCount)
    }

    @Test
    fun `time change while enabled reschedules work`() {
        val preferences = FakeFamilyNotificationPreferences(FamilyNotificationSettings(dailySummaryEnabled = true))
        val scheduler = FakeFamilyNotificationScheduler()
        val controller = FamilyNotificationSettingsController(preferences, scheduler)

        val settings = controller.updateDailySummaryTime(hour = 19, minute = 45)

        assertEquals(19, settings.hour)
        assertEquals(45, settings.minute)
        assertEquals(1, scheduler.rescheduleCount)
    }

    @Test
    fun `time change while disabled does not schedule work`() {
        val preferences = FakeFamilyNotificationPreferences()
        val scheduler = FakeFamilyNotificationScheduler()
        val controller = FamilyNotificationSettingsController(preferences, scheduler)

        controller.updateDailySummaryTime(hour = 7, minute = 15)

        assertEquals(0, scheduler.scheduleCount)
        assertEquals(0, scheduler.rescheduleCount)
    }

    @Test
    fun `deactivation keeps time and cancels work`() {
        val preferences =
            FakeFamilyNotificationPreferences(
                FamilyNotificationSettings(dailySummaryEnabled = true, hour = 18, minute = 20),
            )
        val scheduler = FakeFamilyNotificationScheduler()
        val controller = FamilyNotificationSettingsController(preferences, scheduler)

        val settings = controller.disableDailySummary()

        assertFalse(settings.dailySummaryEnabled)
        assertEquals(18, settings.hour)
        assertEquals(20, settings.minute)
        assertEquals(1, scheduler.cancelCount)
    }
}

private class FakeFamilyNotificationPreferences(
    initialSettings: FamilyNotificationSettings = FamilyNotificationSettings(),
) : FamilyNotificationPreferences {
    private var settings = initialSettings
    val savedSettings = mutableListOf<FamilyNotificationSettings>()

    override fun getSettings(): FamilyNotificationSettings = settings

    override fun saveSettings(settings: FamilyNotificationSettings) {
        this.settings = settings
        savedSettings += settings
    }
}

private class FakeFamilyNotificationScheduler : FamilyNotificationScheduler {
    var scheduleCount = 0
    var nextScheduleCount = 0
    var cancelCount = 0
    var rescheduleCount = 0

    override fun scheduleDailySummary(settings: FamilyNotificationSettings?) {
        scheduleCount += 1
    }

    override fun scheduleNextDailySummary(settings: FamilyNotificationSettings?) {
        nextScheduleCount += 1
    }

    override fun cancelDailySummary() {
        cancelCount += 1
    }

    override fun rescheduleDailySummary(settings: FamilyNotificationSettings?) {
        rescheduleCount += 1
    }
}
