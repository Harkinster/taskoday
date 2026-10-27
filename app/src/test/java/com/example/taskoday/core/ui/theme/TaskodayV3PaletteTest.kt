package com.example.taskoday.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.toArgb

class TaskodayV3PaletteTest {
    @Test
    fun brandAndSurfaceTokensMatchApprovedV33Palette() {
        assertEquals(0xFF6F5AE8.toInt(), TaskodayV3Colors.brandPrimary.toArgb())
        assertEquals(0xFF3E2F73.toInt(), TaskodayV3Colors.brandDeep.toArgb())
        assertEquals(0xFFEEE9FF.toInt(), TaskodayV3Colors.lavender.toArgb())
        assertEquals(0xFFF6F2FF.toInt(), TaskodayV3Colors.lavenderMist.toArgb())
        assertEquals(0xFFF7F4EF.toInt(), TaskodayV3Colors.background.toArgb())
        assertEquals(0xFFFFFDFC.toInt(), TaskodayV3Colors.surface.toArgb())
    }

    @Test
    fun brandTextAndSemanticLabelsMeetAaContrastOnTheirSurfaces() {
        assertTrue(contrast(TaskodayV3Colors.textOnPrimary, TaskodayV3Colors.brandPrimary) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.textSecondary, TaskodayV3Colors.background) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.routineInk, TaskodayV3Colors.routineSoft) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.missionInk, TaskodayV3Colors.missionSoft) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.questInk, TaskodayV3Colors.questSoft) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.successInk, TaskodayV3Colors.successSoft) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.infoInk, TaskodayV3Colors.infoSoft) >= 4.5)
        assertTrue(contrast(TaskodayV3Colors.errorInk, TaskodayV3Colors.errorSoft) >= 4.5)
    }

    private fun contrast(foreground: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color): Double {
        val first = luminance(foreground)
        val second = luminance(background)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    private fun luminance(color: androidx.compose.ui.graphics.Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
