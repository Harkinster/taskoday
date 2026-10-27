package com.example.taskoday

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.taskoday.features.gamification.NestHubTiles
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiV411LargeFontNestTest {
    @get:Rule val ui=createComposeRule()

    @Test fun collection_label_fits_and_both_destinations_work_at_150_percent() {
        var eggs=false
        var collection=false
        ui.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                MaterialTheme {
                    Box(Modifier.width(328.dp)) {
                        NestHubTiles({collection=true},{eggs=true},{},{})
                    }
                }
            }
        }
        val layouts=mutableListOf<TextLayoutResult>()
        ui.onNodeWithText("Collection",useUnmergedTree=true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {it(layouts)}
        assertEquals(1,layouts.single().lineCount)
        val layout=layouts.single()
        // The semantics result can retain the paragraph's available width (725px) while the
        // Text node wraps its 272px glyphs. Check the rendered line, not that allocation flag.
        assertTrue("Collection glyphs must fit",layout.getLineRight(0)<=layout.size.width+1)
        assertTrue("Collection line must fit vertically",layout.getLineBottom(0)<=layout.size.height+1)
        assertFalse("Collection must not be ellipsized",layout.isLineEllipsized(0))
        ui.onNodeWithText("Collection").performClick()
        ui.onNodeWithText("Œufs").performClick()
        ui.runOnIdle {assertTrue(collection);assertTrue(eggs)}
    }
}
