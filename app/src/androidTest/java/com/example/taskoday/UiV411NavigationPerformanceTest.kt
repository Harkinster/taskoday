package com.example.taskoday

import android.os.Debug
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Authenticated real app, genuine local backend; never runs against production. */
class UiV411NavigationPerformanceTest {
    @get:Rule val ui=createEmptyComposeRule()
    private fun requireQaBackend() {
        check(BuildConfig.APPLICATION_ID.endsWith(".familyqa"))
        val expected=InstrumentationRegistry.getArguments().getString("qaBaseUrl") ?: "http://10.0.2.2:18765/"
        check(expected in (18765..18769).map {"http://10.0.2.2:$it/"})
        check(BuildConfig.TASKODAY_BASE_URL==expected)
        val connection=java.net.URL(expected+"qa/isolation").openConnection().apply {connectTimeout=5000;readTimeout=5000}
        val marker=connection.getInputStream().bufferedReader().use {JSONObject(it.readText())}
        check(marker.getString("kind")=="TASKODAY_LOCAL_FAMILY_QA" && !marker.getBoolean("production"))
    }
    private fun hasText(value:String,substring:Boolean=false)=
        ui.onAllNodesWithText(value,substring=substring).fetchSemanticsNodes().isNotEmpty()

    @Test fun real_parent_navigation_including_create_has_minimum_touch_areas() {
        requireQaBackend()
        val areas=JSONArray()
        ActivityScenario.launch(MainActivity::class.java).use {
            ui.waitUntil(45_000) {hasText("Ma Maison")}
            for(label in listOf("Maison","Exploration","Suivi","Le Nid","Ouvrir mon profil","Créer une action")) {
                val node=ui.onNodeWithContentDescription(label).fetchSemanticsNode()
                val b=node.touchBoundsInRoot
                val minimum=with(ui.density) {48.dp.toPx()}
                assertTrue("$label touch width",b.width>=minimum-1)
                assertTrue("$label touch height",b.height>=minimum-1)
                assertTrue(node.config.contains(SemanticsActions.OnClick))
                areas.put(JSONObject().put("label",label).put("touch_width_px",b.width).put("touch_height_px",b.height)
                    .put("role",node.config[SemanticsProperties.Role].toString()))
            }
        }
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,"actual-navigation-touch-areas.json")
            .writeText(areas.toString(2))
    }

    @Test fun measure_real_parent_navigation_and_memory_after_repeated_visits() {
        requireQaBackend()
        val args=InstrumentationRegistry.getArguments()
        val companion=args.getString("qaCompanion") ?: "Eclair QA"
        val observations=JSONArray()
        val memory=JSONArray()
        val semantics=JSONArray()
        ActivityScenario.launch(MainActivity::class.java).use {
            ui.waitUntil(45_000) { hasText("Ma Maison") }
            for (label in listOf("Maison","Exploration","Suivi","Le Nid","Ouvrir mon profil")) {
                val node=ui.onNodeWithContentDescription(label).fetchSemanticsNode()
                val b=node.touchBoundsInRoot
                val minimum=with(ui.density) {48.dp.toPx()}
                assertTrue("$label real app touch width",b.width>=minimum-1)
                assertTrue("$label real app touch height",b.height>=minimum-1)
                assertTrue("$label real app click action",node.config.contains(SemanticsActions.OnClick))
                semantics.put(JSONObject().put("label",label).put("role",node.config[SemanticsProperties.Role].toString())
                    .put("touch_width_px",b.width).put("touch_height_px",b.height).put("click_action",true))
            }
            for (iteration in 0..4) {
                for (destination in listOf("Exploration","Suivi","Le Nid","Maison")) {
                    val start=SystemClock.elapsedRealtimeNanos()
                    ui.onNodeWithContentDescription(destination).performClick()
                    ui.waitUntil(45_000) {
                        when(destination) {
                            "Maison" -> hasText("Ma Maison")
                            "Exploration" -> hasText("Mon parcours · QA Papa") && hasText("actions accomplies",true)
                            "Suivi" -> hasText("Membres du foyer") && hasText("actions accomplies",true)
                            else -> hasText(companion) && hasText("Flammèches",true)
                        }
                    }
                    observations.put(JSONObject().put("iteration",iteration).put("tab",destination)
                        .put("ui_settled_ms",(SystemClock.elapsedRealtimeNanos()-start)/1_000_000.0))
                }
                memory.put(JSONObject().put("iteration",iteration).put("pss_kb",Debug.getPss())
                    .put("java_heap_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
                    .put("native_heap_bytes",Debug.getNativeHeapAllocatedSize()))
            }
        }
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val name=args.getString("outputName") ?: "navigation-performance.json"
        check(name.matches(Regex("[a-zA-Z0-9-]+\\.json")))
        File(target.filesDir,name).writeText(JSONObject()
            .put("measurement","Native Compose test clock; includes test synchronization; software AVD")
            .put("observations",observations).put("memory",memory).put("actual_app_semantics",semantics).toString(2))
        assertEquals(20,observations.length())
    }

}
