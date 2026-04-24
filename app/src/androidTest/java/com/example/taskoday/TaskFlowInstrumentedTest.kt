package com.example.taskoday

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TaskFlowInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun task_flow_create_list_detail_edit_done_delete() {
        val suffix = System.currentTimeMillis().toString().takeLast(6)
        val initialTitle = "Tâche UI $suffix"
        val initialDescription = "Description initiale $suffix"
        val updatedTitle = "Tâche UI modifiée $suffix"
        val updatedDescription = "Description modifiée $suffix"

        waitForAppLaunch()
        openTasksTab()

        createTask(initialTitle, initialDescription)
        assertTaskInTasksList(initialTitle)

        openTaskDetail(initialTitle)
        openTaskEditFromDetail()

        editTask(updatedTitle, updatedDescription)
        assertDetailContentVisible(updatedTitle, updatedDescription)

        markTaskDone()
        assertTaskDone()

        deleteTask()
        assertTaskRemovedFromTasksList(updatedTitle)
    }

    private fun waitForAppLaunch() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(TaskodayTestTags.NavTasks, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun openTasksTab() {
        composeRule
            .onNodeWithTag(TaskodayTestTags.NavTasks, useUnmergedTree = true)
            .performClick()
    }

    private fun createTask(title: String, description: String) {
        composeRule
            .onNodeWithTag(TaskodayTestTags.TasksAddFab)
            .performClick()

        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditTitleField)
            .performTextInput(title)
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditDescriptionField)
            .performTextInput(description)

        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditSaveButton)
            .performClick()
    }

    private fun assertTaskInTasksList(title: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTaskDetail(title: String) {
        composeRule
            .onNode(hasText(title) and hasClickAction(), useUnmergedTree = true)
            .performClick()
    }

    private fun openTaskEditFromDetail() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(TaskodayTestTags.TaskDetailEditButton, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskDetailEditButton, useUnmergedTree = true)
            .performClick()
    }

    private fun editTask(updatedTitle: String, updatedDescription: String) {
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditTitleField)
            .performTextClearance()
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditTitleField)
            .performTextInput(updatedTitle)

        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditDescriptionField)
            .performTextClearance()
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditDescriptionField)
            .performTextInput(updatedDescription)

        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskEditSaveButton)
            .performClick()
    }

    private fun assertDetailContentVisible(title: String, description: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title, useUnmergedTree = true).assertTextContains(title)
        composeRule.onNodeWithText(description, useUnmergedTree = true).assertTextContains(description)
    }

    private fun markTaskDone() {
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskDetailMarkDoneButton)
            .performClick()
    }

    private fun assertTaskDone() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText("Statut : Terminée", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskDetailStatusText, useUnmergedTree = true)
            .assertTextContains("Terminée")
    }

    private fun deleteTask() {
        composeRule
            .onNodeWithTag(TaskodayTestTags.TaskDetailDeleteButton)
            .performClick()
    }

    private fun assertTaskRemovedFromTasksList(title: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithTag(TaskodayTestTags.TasksAddFab, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }
}
