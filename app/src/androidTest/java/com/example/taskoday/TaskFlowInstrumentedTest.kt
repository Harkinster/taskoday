package com.example.taskoday

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.URL
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine authenticated family UI, including durable creation, edit, completion and removal. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TaskFlowInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) = ui.waitUntil(45_000) {
        ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    private fun hideKeyboard() {
        ui.activityRule.scenario.onActivity {
            WindowCompat.getInsetsController(it.window,it.window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
        ui.waitForIdle()
    }
    private fun scrollTo(text: String) {
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }
    private fun click(text: String) { ui.onNodeWithText(text).performClick() }
    private fun waitExplorationLoaded() = ui.waitUntil(45_000) {
        ui.onAllNodesWithText("actions accomplies",substring=true).fetchSemanticsNodes().isNotEmpty()
    }
    private fun form(title: String, description: String, editing: Boolean) {
        waitText(if(editing) "Modifier l'action" else "Nouvelle mission")
        waitText("Titre de l’action")
        val field=ui.onNodeWithText("Titre de l’action")
        field.performTextClearance();field.performTextInput(title);hideKeyboard()
        scrollTo("Plus d'options");click("Plus d'options")
        scrollTo("Note (facultatif)")
        ui.onNodeWithText("Note (facultatif)").performTextClearance()
        ui.onNodeWithText("Note (facultatif)").performTextInput(description);hideKeyboard()
        click(if(editing) "Enregistrer" else "Créer la tâche")
    }
    private fun assertDetail(title: String, description: String) {
        waitText("Détail de l’action")
        ui.onNodeWithText(title).assertExists()
        scrollTo(description);ui.onNodeWithText(description).assertIsDisplayed()
    }

    @Test fun task_flow_create_list_detail_edit_done_delete() {
        check(BuildConfig.APPLICATION_ID.endsWith(".familyqa"))
        val base=BuildConfig.TASKODAY_BASE_URL
        check(base in (18765..18769).map {"http://10.0.2.2:$it/"})
        val connection=URL(base+"qa/isolation").openConnection().apply {connectTimeout=5000;readTimeout=5000}
        val marker=connection.getInputStream().bufferedReader().use {JSONObject(it.readText())}
        check(marker.getString("kind")=="TASKODAY_LOCAL_FAMILY_QA" && !marker.getBoolean("production"))
        // Explicit fixture: genuine QA Parent session, prepared by the guarded QA harness.
        waitText("Ma Maison");ui.onNodeWithContentDescription("Suivi").assertExists()
        ui.onNodeWithContentDescription("Exploration").performClick()
        waitText("Mon parcours · QA Papa");waitText("Ajouter")
        val suffix=System.nanoTime().toString().takeLast(10)
        val initial="QA B08 FINAL FLOW $suffix"
        val updated="$initial edited"
        val note="Note QA $suffix"
        val updatedNote="Note QA modifiée $suffix"
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"qa-task-flow.json")
            .writeText(JSONObject().put("initial",initial).put("updated",updated).put("stage","started").toString())
        click("Ajouter");click("Mission")
        form(initial,note,false)
        waitText("Ajouter");waitExplorationLoaded();scrollTo(initial)
        ui.onNodeWithText(initial).assertIsDisplayed().performClick()
        assertDetail(initial,note)
        scrollTo("Modifier");click("Modifier")
        form(updated,updatedNote,true)
        assertDetail(updated,updatedNote)
        scrollTo("Terminer");click("Terminer")
        waitText("Terminée")
        ui.onNodeWithText("Terminée").assertExists()
        scrollTo("Désactiver / Supprimer");click("Désactiver / Supprimer")
        waitText("Retirer cette tâche ?");click("Retirer")
        waitText("Ajouter");waitExplorationLoaded()
        // Scan the real list, including offscreen rows, rather than only the current viewport.
        val list=ui.onNode(hasScrollToIndexAction())
        list.performScrollToIndex(0)
        ui.onAllNodesWithText(updated).assertCountEquals(0)
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"qa-task-flow.json")
            .writeText(JSONObject().put("initial",initial).put("updated",updated).put("stage","created_edited_completed_removed").toString())
    }
}
