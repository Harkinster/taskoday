package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.LocalDate
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
    fun `date and optional time are preserved separately`() {
        assertEquals(
            "2026-08-22",
            buildFamilyTaskDueDate(dateText = "2026-08-22").getOrThrow(),
        )
        assertEquals("18:30", buildFamilyTaskDueTime(timeText = "18:30").getOrThrow())
        assertNull(buildFamilyTaskDueTime(timeText = "").getOrThrow())
    }

    @Test
    fun `form creates a task without inventing midnight`() {
        val input = validateFamilyTaskCreateForm(validForm(time = "")).input

        assertEquals("2026-08-22", input?.dueDate)
        assertNull(input?.dueTime)
    }

    @Test
    fun `form preserves real midnight when selected`() {
        val input = validateFamilyTaskCreateForm(validForm(time = "00:00")).input

        assertEquals("2026-08-22", input?.dueDate)
        assertEquals("00:00", input?.dueTime)
    }

    @Test
    fun `legacy due at is still available for compatibility`() {
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

    @Test
    fun `recurrence summary is human readable`() {
        assertEquals("Une seule fois", familyTaskRecurrenceSummary(FamilyTaskRecurrence.NONE, emptyList()))
        assertEquals("Tous les jours", familyTaskRecurrenceSummary(FamilyTaskRecurrence.DAILY, emptyList()))
        assertEquals("Chaque semaine", familyTaskRecurrenceSummary(FamilyTaskRecurrence.WEEKLY, emptyList()))
        assertEquals("Lun. • Mer. • Ven.", familyTaskRecurrenceSummary(FamilyTaskRecurrence.SELECTED_WEEKDAYS, listOf(1, 3, 5)))
        assertEquals("Certains jours", familyTaskRecurrenceSummary(FamilyTaskRecurrence.SELECTED_WEEKDAYS, emptyList()))
        assertEquals("Tous les 2 mardis", familyTaskRecurrenceSummary(FamilyTaskRecurrence.SELECTED_WEEKDAYS, listOf(2), 2))
        assertEquals("Lundi et mercredi toutes les 3 semaines", familyTaskRecurrenceSummary(FamilyTaskRecurrence.SELECTED_WEEKDAYS, listOf(1, 3), 3))
    }

    @Test
    fun `custom recurrence validates interval minimum and selected weekdays`() {
        val noDay = validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.SELECTED_WEEKDAYS, selectedWeekdays = emptySet(), recurrenceInterval = 0))
        assertFalse(noDay.isValid)
        val daily = validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.DAILY, recurrenceInterval = 0)).input
        assertEquals(1, daily?.recurrenceInterval)
        val capped = validateFamilyTaskCreateForm(validForm(recurrence = FamilyTaskRecurrence.DAILY, recurrenceInterval = 99)).input
        assertEquals(52, capped?.recurrenceInterval)
    }

    @Test
    fun `date and time labels are formatted for parent input`() {
        assertEquals("22 août", formatFamilyTaskDateLabel("2026-08-22"))
        assertEquals("Choisir une date", formatFamilyTaskDateLabel("bad-date"))
        assertEquals("18:30", formatFamilyTaskTimeLabel("18:30"))
        assertEquals("Sans heure", formatFamilyTaskTimeLabel(""))
        assertEquals("2026-08-22", familyTaskDateFromDueAt("2026-08-22T18:30:00Z"))
        assertEquals("18:30", familyTaskTimeFromDueAt("2026-08-22T18:30:00Z"))
        assertEquals("22 août", familyTaskDueLabel(dueDate = "2026-08-22", dueTime = null, hasDueTime = false, dueAt = null))
        assertEquals("22 août à 00:00", familyTaskDueLabel(dueDate = "2026-08-22", dueTime = "00:00", hasDueTime = true, dueAt = null))
        assertEquals("22 août à 18:30", familyTaskDueLabel(dueDate = "2026-08-22", dueTime = "18:30", hasDueTime = true, dueAt = null))
    }

    @Test
    fun `initial date uses route prefill when valid`() {
        assertEquals(
            "2026-08-26",
            resolveFamilyTaskInitialDate(
                prefilledDate = "2026-08-26",
                fallback = LocalDate.of(2026, 8, 22),
            ),
        )
        assertEquals(
            "2026-08-22",
            resolveFamilyTaskInitialDate(
                prefilledDate = "not-a-date",
                fallback = LocalDate.of(2026, 8, 22),
            ),
        )
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
        recurrenceInterval: Int = 1,
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
            recurrenceInterval = recurrenceInterval,
        )
}
