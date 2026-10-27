package com.example.taskoday.features.auth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationContrastTest {
    @Test fun `registration parchment supports dark body and secondary text`() {
        for (ink in listOf(InkBrown, InkMuted)) {
            assertTrue("Registration text contrast must be at least 4.5", contrast(ink, RegistrationFormBackground) >= 4.5f)
        }
    }

    private fun contrast(text: Color, background: Color): Float =
        (maxOf(text.luminance(), background.luminance()) + 0.05f) /
            (minOf(text.luminance(), background.luminance()) + 0.05f)
}
