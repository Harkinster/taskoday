package com.example.taskoday.features.taskodayv4

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.ParchmentCream
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableStatusContrastTest {
    @Test fun `small overdue labels meet normal-text contrast on parchment`() {
        for (background in listOf(ParchmentLight,ParchmentCream,JourneyParchment,Color(0xFFE9D7B1))) {
            val a=JourneyDangerInk.luminance();val b=background.luminance()
            val contrast=(maxOf(a,b)+.05f)/(minOf(a,b)+.05f)
            assertTrue("Status text contrast must be at least 4.5; was $contrast",contrast>=4.5f)
        }
    }
}
