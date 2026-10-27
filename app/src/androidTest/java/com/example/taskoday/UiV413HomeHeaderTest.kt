package com.example.taskoday

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.taskoday.features.taskodayv4.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiV413HomeHeaderTest {
    @get:Rule val ui=createComposeRule()

    private fun compareAt(scale:Float) {
        var profiles=0
        ui.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                MaterialTheme {
                    Column(Modifier.width(328.dp)) {
                        HomeHeaderStudy(HomeHeaderStudyVariant.RefinedWood,"Ma Maison",null,Modifier.testTag("A-reference")) {}
                        FantasyCompactHeader(trailingContent={
                            FantasyProfileMedallion({profiles++},Modifier.align(Alignment.CenterEnd).padding(end=16.dp))
                        })
                    }
                }
            }
        }
        val a=ui.onNodeWithTag("A-reference").fetchSemanticsNode().boundsInRoot
        val hybrid=ui.onNodeWithTag("home-header-hybrid").fetchSemanticsNode().boundsInRoot
        assertEquals(a.width,hybrid.width,1f)
        assertEquals("Hybrid keeps A's normal and enlarged-font height",a.height,hybrid.height,1f)
        val scope=hasAnyAncestor(hasTestTag("home-header-hybrid"))
        val title=ui.onNode(hasText("Ma Maison") and scope,useUnmergedTree=true)
        title.assert(isHeading())
        val layouts=mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {it(layouts)}
        val layout=layouts.single()
        assertEquals(1,layout.lineCount)
        assertFalse(layout.isLineEllipsized(0))
        assertTrue(layout.getLineRight(0)-layout.getLineLeft(0)<=layout.size.width+1)
        val profile=ui.onNodeWithContentDescription("Ouvrir mon profil")
        val touch=profile.fetchSemanticsNode().touchBoundsInRoot
        val minimum=with(ui.density) {48.dp.toPx()}
        assertTrue(touch.width>=minimum-1 && touch.height>=minimum-1)
        assertTrue(touch.left>=hybrid.left && touch.right<=hybrid.right)
        profile.performClick()
        ui.runOnIdle {assertEquals(1,profiles)}
    }
    @Test fun ordinary_maison_uses_hybrid_with_A_proportions()=compareAt(1f)
    @Test fun compact_large_font_keeps_title_and_profile_readable()=compareAt(1.3f)
    @Test fun non_maison_headers_keep_the_validated_rendering() {
        ui.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalHomeHeaderStudyVariant provides HomeHeaderStudyVariant.RefinedWood) {
                    Column(Modifier.width(328.dp)) {
                        FantasyCompactHeader(title="Exploration")
                        FantasyCompactHeader(title="Actions globales")
                    }
                }
            }
        }
        ui.onNodeWithTag("home-header-hybrid").assertDoesNotExist()
        ui.onNodeWithText("Exploration").assert(isHeading())
        ui.onNodeWithText("Actions globales").assert(isHeading())
    }
}
