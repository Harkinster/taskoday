package com.example.taskoday

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskoday.features.notifications.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccountNotificationsUiTest {
    @get:Rule val ui = createComposeRule()
    @Test fun parent_preferences_remain_usable_at_360dp_with_large_font() {
        var enabled=false;var category:NoticeKind?=null;var picked=false
        ui.setContent {
            val d=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density,1.3f)) {
                Box(Modifier.width(360.dp)) {
                    NotificationPreferencesContent(FamilyNotificationsUiState(owner=901,parent=true,hasFamily=true,isBusy=false),
                        {enabled=it},{kind,_->category=kind},{},{picked=it},{},{},{},{})
                }
            }
        }
        ui.onNodeWithContentDescription("Activer les notifications").performClick()
        ui.runOnIdle {assertTrue(enabled)}
        ui.onNodeWithContentDescription("Missions à valider").performScrollTo().performClick()
        ui.runOnIdle {assertEquals(NoticeKind.VALIDATION_NEEDED,category)}
        ui.onNodeWithText("Début · 22:00").performScrollTo().assertIsDisplayed().performClick()
        ui.runOnIdle {assertTrue(picked)}
    }
    @Test fun child_preferences_do_not_expose_parent_categories() {
        ui.setContent {
            NotificationPreferencesContent(FamilyNotificationsUiState(owner=902,parent=false,hasFamily=true,isBusy=false),{},{_,_->},{},{},{},{},{},{})
        }
        ui.onNodeWithContentDescription("Missions à valider").assertDoesNotExist()
        ui.onNodeWithText("Résumé du foyer").assertDoesNotExist()
        ui.onNodeWithContentDescription("Échéances personnelles").assertExists()
    }
    @Test fun preferences_and_ledger_survive_recreation_and_remain_account_scoped() {
        val c=InstrumentationRegistry.getInstrumentation().targetContext
        // Dedicated test context protects the real QA user's preferences and owner.
        val test=c.createDeviceProtectedStorageContext()
        val gate=SharedPreferencesNotificationSessionGate(test)
        val prefs=SharedPreferencesFamilyNotificationPreferences(test,gate,Gson())
        val ledger=SharedPreferencesAccountNoticeLedger(test,Gson())
        try {
            gate.bind(901,true,91);prefs.saveSettings(FamilyNotificationSettings(enabled=true,quietStartMinute=123))
            val first=gate.current()!!
            ledger.write(first,NotificationLedger(42,mapOf("qa-key" to 40L)))
            gate.bind(902,false,92)
            assertFalse(prefs.getSettings().enabled)
            prefs.saveSettings(FamilyNotificationSettings(personalDue=false))
            assertTrue(ledger.read(gate.current()!!).delivered.isEmpty())
            gate.bind(901,true,91)
            assertTrue(prefs.getSettings().enabled);assertEquals(123,prefs.getSettings().quietStartMinute)
            assertEquals(42L,SharedPreferencesAccountNoticeLedger(test,Gson()).read(gate.current()!!).cursorMs)
            val stale=gate.current()!!;gate.invalidate()
            assertNull(gate.withCurrent(stale){true});assertFalse(prefs.getSettings().enabled)
        } finally {
            listOf("taskoday_notification_session","taskoday_account_notifications","taskoday_notification_ledger").forEach {test.deleteSharedPreferences(it)}
        }
    }
}
