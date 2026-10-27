package com.example.taskoday.core.ui.component.forms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import org.junit.Assert.assertTrue
import org.junit.Test

class FantasyFormContrastTest {
    @Test fun `form text selected choices and errors meet normal text contrast`() {
        for ((foreground, background) in listOf(
            FormInk to ParchmentCream,
            FormViolet to ParchmentCream,
            ParchmentLight to FormViolet,
            FormError to ParchmentLight,
        )) {
            assertTrue("Form text contrast must reach 4.5:1", contrast(foreground, background) >= 4.5f)
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) /
            (minOf(a.luminance(), b.luminance()) + .05f)
}
