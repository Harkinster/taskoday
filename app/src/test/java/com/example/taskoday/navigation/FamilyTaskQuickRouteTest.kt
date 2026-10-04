package com.example.taskoday.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyTaskQuickRouteTest {
    @Test
    fun `home list and floating add share the quick form route`() {
        assertEquals("family_home/create?quick=true", TaskodayDestination.FamilyTaskCreate.createRoute(quick = true))
        assertEquals("family_home/create?date=2026-10-04&quick=true", TaskodayDestination.FamilyTaskCreate.createRoute("2026-10-04", quick = true))
    }
}
