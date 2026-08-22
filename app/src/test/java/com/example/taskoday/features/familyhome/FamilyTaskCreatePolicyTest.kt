package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTaskCreatePolicyTest {
    @Test
    fun `title is required`() {
        val validation = validateFamilyTaskCreateForm(validForm(title = "   "))

        assertFalse(validation.isValid)
        assertNull(validation.input)
        assertEquals("Le titre est obligatoire.", validation.errorMessage)
    }

    @Test
    fun `house task keeps assignees empty`() {
        val input =
            validateFamilyTaskCreateForm(
                validForm(assigneeUserIds = emptySet()),
            ).input

        assertEquals(emptyList<Long>(), input?.assigneeUserIds)
    }

    @Test
    fun `parent child and multi assignment are sorted`() {
        val input =
            validateFamilyTaskCreateForm(
                validForm(assigneeUserIds = setOf(30L, 10L, 20L)),
            ).input

        assertEquals(listOf(10L, 20L, 30L), input?.assigneeUserIds)
    }

    @Test
    fun `none daily and weekly recurrence are accepted`() {
        assertEquals(FamilyTaskRecurrence.NONE, validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.NONE)).input?.recurrence)
        assertEquals(FamilyTaskRecurrence.DAILY, validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.DAILY)).input?.recurrence)
        assertEquals(FamilyTaskRecurrence.WEEKLY, validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.WEEKLY)).input?.recurrence)
    }

    @Test
    fun `selected weekdays require at least one day`() {
        val invalid =
            validateFamilyTaskCreateForm(
                validForm(
                    recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                    selectedWeekdays = emptySet(),
                ),
            )
        val valid =
            validateFamilyTaskCreateForm(
                validForm(
                    recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                    selectedWeekdays = setOf(5, 1),
                ),
            )

        assertFalse(invalid.isValid)
        assertEquals("Choisis au moins un jour.", invalid.errorMessage)
        assertEquals(listOf(1, 5), valid.input?.selectedWeekdays)
    }

    @Test
    fun `date and time are converted to backend datetime`() {
        assertEquals(
            "2026-08-22T18:30:00Z",
            buildFamilyTaskDueAt(dateText = "2026-08-22", timeText = "18:30").getOrThrow(),
        )
        assertEquals(
            "2026-08-22T00:00:00Z",
            buildFamilyTaskDueAt(dateText = "2026-08-22", timeText = "").getOrThrow(),
        )
    }

    @Test
    fun `validation gamification and priority are preserved`() {
        val input =
            validateFamilyTaskCreateForm(
                validForm(
                    validationRequired = true,
                    gamificationEnabled = true,
                    priority = FamilyTaskPriority.URGENT,
                ),
            ).input

        assertTrue(input?.validationRequired == true)
        assertTrue(input?.gamificationEnabled == true)
        assertEquals(FamilyTaskPriority.URGENT, input?.priority)
    }

    private fun validForm(
        title: String = "Sortir les poubelles",
        description: String = "Bac jaune",
        date: String = "2026-08-22",
        time: String = "18:30",
        recurrence: FamilyTaskRecurrence = FamilyTaskRecurrence.NONE,
        selectedWeekdays: Set<Int> = emptySet(),
        assigneeUserIds: Set<Long> = setOf(10L),
        validationRequired: Boolean = false,
        gamificationEnabled: Boolean = false,
        priority: FamilyTaskPriority = FamilyTaskPriority.NORMAL,
    ): FamilyTaskCreateForm =
        FamilyTaskCreateForm(
            title = title,
            description = description,
            date = date,
            time = time,
            recurrence = recurrence,
            selectedWeekdays = selectedWeekdays,
            assigneeUserIds = assigneeUserIds,
            validationRequired = validationRequired,
            gamificationEnabled = gamificationEnabled,
            priority = priority,
        )
}
