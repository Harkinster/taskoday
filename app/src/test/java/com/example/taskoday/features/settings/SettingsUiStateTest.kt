package com.example.taskoday.features.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun `default profile state is loading and not offline`() {
        val state = SettingsUiState()

        assertEquals("Profil", state.profileName)
        assertEquals("Chargement du profil...", state.profileSubtitle)
        assertNotEquals("Profil local", state.profileName)
        assertNotEquals("Mode hors-ligne", state.profileSubtitle)
    }

    @Test
    fun `offline profile labels remain available when explicitly set`() {
        val state =
            SettingsUiState(
                profileName = "Profil local",
                profileSubtitle = "Mode hors-ligne",
            )

        assertEquals("Profil local", state.profileName)
        assertEquals("Mode hors-ligne", state.profileSubtitle)
    }
}
