package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskPriority
import java.util.Locale

const val FAMILY_TASK_FILTER_ALL = "all"
const val FAMILY_TASK_FILTER_HOUSE = "house"

fun familyTaskMemberFilterKey(memberId: Long): String = "member_$memberId"

fun buildFamilyTaskListFilters(members: List<FamilyTaskMember>): List<FamilyTaskListFilterOption> =
    buildList {
        add(FamilyTaskListFilterOption(key = FAMILY_TASK_FILTER_ALL, label = "Toutes"))
        add(FamilyTaskListFilterOption(key = FAMILY_TASK_FILTER_HOUSE, label = "Maison"))
        members
            .sortedBy { member -> member.displayName.lowercase(Locale.FRANCE) }
            .forEach { member ->
                add(
                    FamilyTaskListFilterOption(
                        key = familyTaskMemberFilterKey(member.userId),
                        label = member.displayName,
                    ),
                )
            }
    }

fun filterFamilyTaskDefinitions(
    tasks: List<FamilyTaskDefinition>,
    filterKey: String,
): List<FamilyTaskDefinition> {
    val activeTasks = tasks.filter { task -> task.active }
    return activeTasks
        .filter { task ->
            when {
                filterKey == FAMILY_TASK_FILTER_ALL -> true
                filterKey == FAMILY_TASK_FILTER_HOUSE -> task.assignees.isEmpty()
                filterKey.startsWith("member_") -> {
                    val memberId = filterKey.removePrefix("member_").toLongOrNull()
                    memberId != null && task.assignees.any { assignee -> assignee.id == memberId }
                }
                else -> true
            }
        }
        .sortedWith(compareBy<FamilyTaskDefinition> { task -> task.sortDateKey().orEmpty() }.thenBy { task -> task.title.lowercase(Locale.FRANCE) })
}

fun FamilyTaskDefinition.assignmentLabel(): String =
    assignees.assignmentLabel()

fun FamilyTaskDefinition.scheduleLabel(): String =
    familyTaskDueLabel(
        dueDate = dueDate,
        dueTime = dueTime,
        hasDueTime = hasDueTime,
        dueAt = dueAt,
    ) ?: "Aucune date"

fun FamilyTaskDefinition.listMetadataLabels(): List<String> =
    buildList {
        add(familyTaskRecurrenceSummary(recurrence, selectedWeekdays))
        familyTaskPriorityListLabel(priority)?.let { add(it) }
        if (validationRequired) add("Validation parent")
        if (gamificationEnabled) add("Gamification")
    }

private fun List<FamilyTaskAssignee>.assignmentLabel(): String =
    if (isEmpty()) {
        "Maison"
    } else {
        joinToString { assignee -> assignee.displayName }
    }

private fun FamilyTaskDefinition.sortDateKey(): String? =
    familyTaskDateFromFields(dueDate = dueDate, dueAt = dueAt)

private fun familyTaskPriorityListLabel(priority: FamilyTaskPriority): String? =
    when (priority) {
        FamilyTaskPriority.LOW -> "Priorite basse"
        FamilyTaskPriority.NORMAL,
        FamilyTaskPriority.UNKNOWN,
        -> null
        FamilyTaskPriority.HIGH -> "Prioritaire"
        FamilyTaskPriority.URGENT -> "Urgent"
    }

