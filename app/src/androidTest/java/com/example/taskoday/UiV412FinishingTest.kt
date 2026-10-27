package com.example.taskoday

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.taskoday.features.taskodayv4.*
import com.example.taskoday.features.familyhome.FamilyTaskListHeader
import com.example.taskoday.features.gamification.NestEmptyEggCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class UiV412FinishingTest {
    @get:Rule val ui=createComposeRule()
    private fun content(children: @Composable () -> Unit) {
        ui.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.3f)) {
                MaterialTheme {Box(Modifier.width(328.dp)) {children()}}
            }
        }
    }
    private fun minimumTarget(label:String) {
        val b=ui.onNodeWithContentDescription(label).fetchSemanticsNode().touchBoundsInRoot
        val min=with(ui.density) {48.dp.toPx()}
        assertTrue(label,b.width>=min-1 && b.height>=min-1)
    }
    private fun readable(label:String) {
        val layouts=mutableListOf<TextLayoutResult>()
        ui.onNodeWithText(label,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {it(layouts)}
        val result=layouts.single()
        for(line in 0 until result.lineCount) {
            assertFalse(label,result.isLineEllipsized(line))
            // Centered labels keep the paragraph's available width in the semantic result.
            // Check the actual glyph span; its paragraph-relative offset is not clipping.
            assertTrue(label,result.getLineRight(line)-result.getLineLeft(line)<=result.size.width+1)
        }
        assertTrue(label,result.getLineBottom(result.lineCount-1)<=result.size.height+1)
    }
    @Test fun today_and_upcoming_remain_two_accessible_working_tabs_at_130_percent() {
        content {
            var today by remember {mutableStateOf(true)}
            Row {
                FantasyTrailTab("Aujourd'hui",today,{today=true},Modifier.weight(1f))
                FantasyTrailTab("À venir",!today,{today=false},Modifier.weight(1f))
            }
        }
        ui.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Tab)).assertCountEquals(2)
        ui.onNodeWithText("Toutes").assertDoesNotExist()
        minimumTarget("Aujourd'hui");minimumTarget("À venir")
        readable("Aujourd'hui");readable("À venir")
        ui.onNodeWithContentDescription("Aujourd'hui").assertIsSelected()
        ui.onNodeWithContentDescription("À venir").performClick().assertIsSelected()
        ui.onNodeWithContentDescription("Aujourd'hui").assertIsNotSelected()
    }
    @Test fun journal_and_add_keep_distinct_callbacks_and_keyboard_activation() {
        var journal=0;var add=0
        lateinit var input:InputModeManager
        content {
            input=LocalInputModeManager.current
            Row {
                FantasyFramedButton("Mon journal",{journal++},Modifier.weight(1f))
                FantasyFramedButton("Ajouter",{add++},Modifier.weight(1f))
            }
        }
        for(label in listOf("Mon journal","Ajouter")) {
            minimumTarget(label);readable(label)
            ui.onNodeWithContentDescription(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Button))
        }
        ui.onNodeWithContentDescription("Mon journal").performClick()
        ui.runOnIdle {assertTrue(input.requestInputMode(InputMode.Keyboard))}
        ui.onNodeWithContentDescription("Ajouter").performSemanticsAction(SemanticsActions.RequestFocus) {assertTrue(it())}
        ui.onNodeWithContentDescription("Ajouter").performKeyInput {pressKey(Key.Enter)}
        ui.runOnIdle {assertEquals(1,journal);assertEquals(1,add)}
    }
    @Test fun disabled_framed_action_stays_disabled() {
        var count=0
        content {FantasyFramedButton("Indisponible",{count++},enabled=false)}
        ui.onNodeWithContentDescription("Indisponible").assertIsNotEnabled()
        ui.runOnIdle {assertEquals(0,count)}
    }
    @Test fun empty_egg_card_preserves_copy_and_egg_navigation_at_large_font() {
        var eggs=0
        content {NestEmptyEggCard {eggs++}}
        readable("Aucun œuf ne repose encore dans ton Nid.")
        readable("Continue tes aventures pour en découvrir.")
        minimumTarget("Voir mes œufs")
        ui.onNodeWithContentDescription("Voir mes œufs").performClick()
        ui.runOnIdle {assertEquals(1,eggs)}
    }
    @Test fun global_header_without_parent_callback_has_no_creation_action() {
        var back=0
        content {FamilyTaskListHeader(2,{back++},null)}
        ui.onNodeWithContentDescription("Ajouter une tâche").assertDoesNotExist()
        ui.onNodeWithContentDescription("Retour").performClick()
        ui.runOnIdle {assertEquals(1,back)}
    }
    @Test fun parent_creation_and_global_title_remain_readable_and_clickable() {
        var add=0
        content {FamilyTaskListHeader(2,{}, {add++})}
        readable("Actions globales");readable("Ajouter une tâche")
        minimumTarget("Ajouter une tâche")
        ui.onNodeWithContentDescription("Ajouter une tâche").performClick()
        ui.runOnIdle {assertEquals(1,add)}
    }
}
