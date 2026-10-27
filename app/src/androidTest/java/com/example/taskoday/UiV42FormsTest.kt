package com.example.taskoday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.component.forms.*
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.features.familyhome.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiV42FormsTest {
    @get:Rule val ui = createComposeRule()

    private fun content(width: Int = 328, children: @Composable () -> Unit) {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                FantasyFormTheme {
                    Column(Modifier.width(width.dp).verticalScroll(rememberScrollState())) { children() }
                }
            }
        }
    }

    private fun readable(label: String) {
        val node = ui.onNodeWithText(label, useUnmergedTree = true)
        node.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        for (line in 0 until layout.lineCount) {
            assertFalse(label, layout.isLineEllipsized(line))
            assertTrue(label, layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1)
        }
    }

    @Test fun house_kind_choices_keep_all_types_and_explain_the_selected_kind() {
        content {
            var state by remember { mutableStateOf(FamilyTaskCreateUiState(isLoadingMembers = false)) }
            ActionKindCard(state) { state = state.copy(actionType = it) }
        }
        for (label in listOf("Quête", "Routine", "Mission")) readable(label)
        ui.onNodeWithText("Routine").performClick().assertIsSelected()
        ui.onNodeWithText("Quête").assertIsNotSelected()
        readable("Un geste qui revient, selon le rythme choisi.")
    }

    @Test fun personal_and_editing_constraints_remain_visible_and_enforced() {
        var changed = false
        content {
            ActionKindCard(FamilyTaskCreateUiState(taskId = 4, actionType = FamilyActionType.PERSONAL_ROUTINE)) { changed = true }
        }
        ui.onNodeWithText("Quête").assertDoesNotExist()
        ui.onNodeWithText("Routine").assertIsSelected().assertIsNotEnabled()
        ui.onNodeWithText("Mission").assertIsNotEnabled().performClick()
        ui.runOnIdle { assertFalse(changed) }
    }

    private fun recurrenceAt(width: Int) {
        var selectedDay = -1
        var interval = -1
        content(width) {
            RecurrenceCard(
                FamilyTaskCreateUiState(actionType = FamilyActionType.HOUSE_ROUTINE,
                    recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS, selectedWeekdays = setOf(1, 3),
                    isCustomRecurrence = true, customRecurrenceUnit = CustomRecurrenceUnit.WEEKS, recurrenceInterval = 2),
                {}, { selectedDay = it }, {}, { interval = it }, {}, {},
            )
        }
        for (label in listOf("Tous les jours", "Chaque semaine", "Certains jours", "Personnaliser",
            "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim", "Sans date de fin")) readable(label)
        ui.onNodeWithText("Dim").performScrollTo().performClick()
        ui.onNodeWithText("+").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(7, selectedDay); assertEquals(3, interval) }
    }

    @Test fun recurrence_controls_fit_360dp_at_large_font() = recurrenceAt(328)
    @Test fun recurrence_controls_fit_390dp_at_large_font() = recurrenceAt(358)

    @Test fun themed_fields_keep_editing_and_password_semantics() {
        content {
            var title by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            FantasyFormField(title, { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth())
            FantasyFormField(password, { password = it }, label = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        }
        ui.onNodeWithText("Titre").performTextInput("Une mission")
        ui.onNodeWithText("Une mission").assertExists()
        ui.onNodeWithText("Mot de passe").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).performTextInput("test-local")
        // InputText is intentionally available to Compose tests; EditableText is what
        // the password field presents, and must remain transformed.
        ui.onNodeWithText("Mot de passe").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.EditableText, AnnotatedString("•".repeat(10))))
    }

    @Test fun footer_keeps_primary_disabled_and_back_usable_at_large_font() {
        var backs = 0
        var submits = 0
        content {
            FantasyFormBottomBar({ backs++ }, { submits++ }, "Créer la tâche", primaryEnabled = false, backLabel = "Annuler")
        }
        ui.onNodeWithContentDescription("Créer la tâche").assertIsNotEnabled().performClick()
        ui.onNodeWithText("Annuler").performClick()
        ui.runOnIdle { assertEquals(1, backs); assertEquals(0, submits) }
    }
}
