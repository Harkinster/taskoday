package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.CompletionReward
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRewardFeedbackTest {
    @Test fun `completion displays the personal reward bundle`() {
        assertEquals("T\u00e2che termin\u00e9e \u00b7 +30 pts \u00b7 +3 Flamm\u00e8ches \u00b7 +6 Cristaux", taskCompletionFeedback(CompletionReward(30, 3, 6)))
        assertEquals("T\u00e2che termin\u00e9e.", taskCompletionFeedback(CompletionReward()))
    }

    @Test fun `mission bonus is explicitly acknowledged`() {
        assertEquals("T\u00e2che termin\u00e9e \u00b7 +30 pts \u00b7 +3 Flamm\u00e8ches \u00b7 +6 Cristaux \u00b7 Bonus Mission : +2 Cristaux", taskCompletionFeedback(CompletionReward(30, 3, 6, 2)))
    }

    @Test fun `validation does not imply a reward for the validating parent`() {
        assertEquals("T\u00e2che valid\u00e9e.", taskValidationFeedback(CompletionReward()))
        assertEquals("T\u00e2che valid\u00e9e \u00b7 +30 pts \u00b7 +3 Flamm\u00e8ches \u00b7 +4 Cristaux", taskValidationFeedback(CompletionReward(30, 3, 4)))
    }
}
