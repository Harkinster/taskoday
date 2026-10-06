package com.example.taskoday.features.followup

import com.example.taskoday.domain.model.FamilyActionScope
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.features.activity.journalLabel

fun FamilyTaskTodayItem.followUpIdentityLabel(): String {
    val identity = FamilyActionType.fromWire(scope.name, kind.name, category)
    val participants = if (assignees.isEmpty()) "Maison" else assignees.joinToString { it.displayName }
    return if (scope == FamilyActionScope.HOUSE) "${identity.journalLabel()} · $participants" else identity.journalLabel()
}
