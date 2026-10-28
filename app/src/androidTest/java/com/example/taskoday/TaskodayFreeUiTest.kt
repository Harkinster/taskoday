package com.example.taskoday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.theme.TaskodayTheme
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.features.settings.ActiveChildCard
import com.example.taskoday.features.settings.SettingsUiState
import com.example.taskoday.features.shop.ParentRewardEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskodayFreeUiTest {
    @get:Rule val ui = createComposeRule()
    @Test fun add_child_remains_enabled_with_two_existing_children_at_large_font() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                TaskodayTheme {
                    Column(Modifier.width(328.dp).verticalScroll(rememberScrollState())) {
                        ActiveChildCard(SettingsUiState(isParentUser = true, isChildrenListReady = true,
                            pairedChildren = listOf(ParentChild(1,"QA One",""),ParentChild(2,"QA Two",""))),
                            {}, { _,_,_,_ -> }, { _,_ -> }, {}, {}, false)
                    }
                }
            }
        }
        ui.onNodeWithText("Ajouter un enfant").performScrollTo().assertIsEnabled().performClick()
        ui.onNodeWithText("Email enfant").assertExists()
        ui.onNodeWithText("Voir Premium").assertDoesNotExist()
    }
    @Test fun wish_creation_remains_available_without_a_subscription_counter() {
        var creates = 0
        ui.setContent {
            TaskodayTheme {
                Column(Modifier.width(328.dp).verticalScroll(rememberScrollState())) {
                    ParentRewardEditor(null, false, { _,_,_,_ -> creates++ }, { _,_,_,_,_ -> }, {})
                }
            }
        }
        ui.onNodeWithText("Titre").performScrollTo().performTextInput("QA wish")
        ui.onNodeWithText("Coût en Flammèches").performScrollTo().performTextInput("10")
        ui.onNodeWithText("Créer le souhait").performScrollTo().assertIsEnabled().performClick()
        ui.runOnIdle { assertEquals(1, creates) }
        ui.onNodeWithText("Voir Premium").assertDoesNotExist()
    }
}
