package com.example.taskoday.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyTaskQuickRouteTest {
    @Test
    fun `home list and floating add can share the same quick route`() {
        assertEquals("family_home/create?quick=true&kind=HOUSE_QUEST", TaskodayDestination.FamilyTaskCreate.createQuickRoute())
        assertEquals("family_home/create?date=2026-10-04&quick=true&kind=HOUSE_QUEST", TaskodayDestination.FamilyTaskCreate.createQuickRoute("2026-10-04"))
        assertEquals("family_home/create?quick=true&kind=PERSONAL_ROUTINE&memberId=27", TaskodayDestination.FamilyTaskCreate.createQuickRoute(kind = com.example.taskoday.domain.model.FamilyActionType.PERSONAL_ROUTINE, memberId = 27L))
        assertEquals("family_home/create?quick=true&kind=PERSONAL_MISSION", TaskodayDestination.FamilyTaskCreate.createQuickRoute(kind = com.example.taskoday.domain.model.FamilyActionType.PERSONAL_MISSION))
        assertEquals("family_home/create?quick=true&kind=PERSONAL_MISSION&memberId=26", TaskodayDestination.FamilyTaskCreate.createQuickRoute(kind = com.example.taskoday.domain.model.FamilyActionType.PERSONAL_MISSION, memberId = 26L))
    }
}
