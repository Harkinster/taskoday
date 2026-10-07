package com.example.taskoday.navigation

import com.example.taskoday.domain.model.AuthenticatedUser
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountLandingNavigationTest {
    @Test fun `child login and session restoration open exploration`() {
        assertEquals(TaskodayDestination.Exploration.route, user("CHILD").preferredAppRoute(false))
    }
    @Test fun `parent with family keeps family home`() {
        assertEquals(TaskodayDestination.FamilyHome.route, user("PARENT").preferredAppRoute(false))
    }
    @Test fun `parent without family keeps onboarding`() {
        assertEquals(TaskodayDestination.FamilyHousehold.route, user("PARENT").copy(familyIds = emptyList()).preferredAppRoute(false))
    }
    @Test fun `explicit local mode keeps routines landing`() {
        assertEquals(TaskodayDestination.Home.route, user("CHILD").preferredAppRoute(true))
    }
    @Test fun `unknown session does not pretend to be authenticated family home`() {
        assertEquals(TaskodayDestination.Home.route, (null as AuthenticatedUser?).preferredAppRoute(false))
    }
    @Test fun `brand navigation returns real parent or child to family home`() {
        assertEquals(TaskodayDestination.FamilyHome, accountHomeDestination(true, false))
        assertEquals(TaskodayDestination.Exploration, accountHomeDestination(true, false, true))
    }
    @Test fun `brand navigation preserves explicit local child mode`() {
        assertEquals(TaskodayDestination.Home, accountHomeDestination(true, true))
    }
    @Test fun `brand navigation preserves demo session`() {
        assertEquals(TaskodayDestination.Home, accountHomeDestination(false, false))
    }
    @Test fun `nest crystal entry opens canonical resource utility chest section`() {
        assertEquals("shop?section=chests", chestUtilityRoute())
    }
    private fun user(role: String) = AuthenticatedUser(1L, "ada@example.com", role, true, listOf(7L), "Ada")
}
