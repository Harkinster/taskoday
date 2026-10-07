package com.example.taskoday.features.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun `loading guest can reach login while a remote account is not logged out`() {
        assertTrue(shouldOfferHomeLogin(hasRemoteSession = false))
        assertFalse(shouldOfferHomeLogin(hasRemoteSession = true))
    }
}
