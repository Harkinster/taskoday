package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyActionType

/** Fail closed until the authenticated identity has been resolved. */
data class FamilyTaskAccessPolicy(val userId: Long? = null, val role: String? = null) {
    val canManage: Boolean get() = userId != null && role.equals("PARENT", ignoreCase = true)

    fun canView(assignees: List<FamilyTaskAssignee>): Boolean =
        canManage || (userId != null && role.equals("CHILD", ignoreCase = true) &&
            (assignees.isEmpty() || assignees.any { it.id == userId }))

    fun canView(task: FamilyTaskDefinition): Boolean =
        userId != null && (canManage || role.equals("CHILD", ignoreCase = true)) &&
            (FamilyActionType.fromCategory(task.category) == FamilyActionType.HOUSE_QUEST || canView(task.assignees))

    fun quickAction(task: FamilyTaskTodayItem): FamilyTaskQuickAction? =
        quickActionFor(task)?.takeIf { action ->
            canView(task.assignees) && (canManage || action == FamilyTaskQuickAction.COMPLETE)
        }

    companion object {
        fun forUser(user: AuthenticatedUser) = FamilyTaskAccessPolicy(user.id, user.role)
    }
}
