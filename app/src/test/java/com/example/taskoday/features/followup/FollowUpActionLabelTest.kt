package com.example.taskoday.features.followup

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FollowUpActionLabelTest {
    @Test fun `tracking distinguishes all five combinations`() {
        val expected = mapOf(
            FamilyActionType.PERSONAL_ROUTINE to "Routine",
            FamilyActionType.PERSONAL_MISSION to "Mission",
            FamilyActionType.HOUSE_ROUTINE to "Routine Maison · Ada",
            FamilyActionType.HOUSE_MISSION to "Mission Maison · Ada",
            FamilyActionType.HOUSE_QUEST to "Quête Maison · Ada",
        )
        expected.forEach { (type, label) ->
            val item = FamilyTaskTodayItem(1, 2, "Action", listOf(FamilyTaskAssignee(27, "Ada")), null, null, null, false, null,
                FamilyTaskStatus.TODO, false, false, FamilyTaskPriority.NORMAL, category = type.category)
            assertEquals(label, item.followUpIdentityLabel())
        }
    }
}
