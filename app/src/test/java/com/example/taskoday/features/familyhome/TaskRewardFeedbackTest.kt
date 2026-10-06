package com.example.taskoday.features.familyhome

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRewardFeedbackTest {
    @Test fun `completion shows only points awarded to current user`() {
        assertEquals("Tâche terminée · +20 points", taskCompletionFeedback(20))
        assertEquals("Tâche terminée.", taskCompletionFeedback(0))
    }

    @Test fun `validation does not imply points to the validating parent`() {
        assertEquals("Tâche validée.", taskValidationFeedback())
        assertEquals("Tâche validée · +20 points", taskValidationFeedback(20))
    }
}
