package com.example.taskoday

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.features.taskodayv4.*
import com.example.taskoday.navigation.TaskodayDestination
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production components, isolated from account data. Real app journeys are exercised separately. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class UiV411AccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun custom_buttons_have_button_roles_and_keyboard_activation() {
        var purpleClicks = 0
        var journeyClicks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode=LocalInputModeManager.current
            MaterialTheme {
                Column {
                    FantasyPurpleButton("Commencer QA", onClick = { purpleClicks++ })
                    FantasyButton("Terminer QA", onClick = { journeyClicks++ })
                }
            }
        }
        val purple = compose.onNodeWithContentDescription("Commencer QA")
        val journey = compose.onNodeWithText("Terminer QA")
        purple.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        journey.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        purple.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        purple.performKeyInput { pressKey(Key.Enter) }
        journey.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        journey.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1,purpleClicks); assertEquals(1,journeyClicks) }
    }

    @Test fun disabled_button_cannot_be_activated() {
        var clicks = 0
        compose.setContent { MaterialTheme { FantasyPurpleButton("Indisponible QA", enabled = false, onClick = { clicks++ }) } }
        compose.onNodeWithContentDescription("Indisponible QA").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0,clicks) }
    }

    @Test fun profile_has_a_label_role_and_minimum_touch_area() {
        var opened = false
        compose.setContent { MaterialTheme { FantasyProfileMedallion({ opened=true }) } }
        val profile=compose.onNodeWithContentDescription("Ouvrir mon profil")
        profile.assertHasClickAction().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Button))
        val node=profile.fetchSemanticsNode()
        val minimum=with(compose.density) { 48.dp.toPx() }
        assertTrue(node.touchBoundsInRoot.width>=minimum-1)
        assertTrue(node.touchBoundsInRoot.height>=minimum-1)
        profile.performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun child_navigation_has_real_bounds_and_no_parent_commands() {
        var route: String?=null
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode=LocalInputModeManager.current
            MaterialTheme {
                FantasyHomeNavigationBar(listOf(TaskodayDestination.FamilyHome,TaskodayDestination.Exploration,TaskodayDestination.Nest),
                    TaskodayDestination.Exploration.route,false,{route=it.route},{error("Child creation must not be exposed")},Modifier.fillMaxWidth())
            }
        }
        compose.onNodeWithContentDescription("Suivi").assertDoesNotExist()
        compose.onNodeWithContentDescription("Créer une action").assertDoesNotExist()
        for (label in listOf("Maison","Exploration","Le Nid")) {
            val destination=compose.onNodeWithContentDescription(label)
            destination.assertHasClickAction().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Tab))
            val minimum=with(compose.density) { 48.dp.toPx() }
            val b=destination.fetchSemanticsNode().touchBoundsInRoot
            assertTrue("$label touch width",b.width>=minimum-1)
            assertTrue("$label touch height",b.height>=minimum-1)
        }
        compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithContentDescription("Le Nid").performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        compose.onNodeWithContentDescription("Le Nid").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(TaskodayDestination.Nest.route,route) }
    }

    @Test fun section_titles_are_exposed_as_headings() {
        compose.setContent { MaterialTheme { FantasySectionHeading("Actions du jour") } }
        compose.onNodeWithText("Actions du jour").assert(isHeading())
    }
}
