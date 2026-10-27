package com.example.taskoday.features.taskodayv4

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeHeaderStudyGateTest {
    @Test fun `ordinary launches and builds outside QA keep the approved current header`() {
        for(request in listOf(null,"wood-refined","parchment","unknown")) {
            assertEquals(HomeHeaderStudyVariant.Current,HomeHeaderStudyVariant.fromRequest(false,request))
        }
        assertEquals(HomeHeaderStudyVariant.Current,HomeHeaderStudyVariant.fromRequest(true,null))
        assertEquals(HomeHeaderStudyVariant.Current,HomeHeaderStudyVariant.fromRequest(true,"unknown"))
    }
}
