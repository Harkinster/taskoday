package com.example.taskoday.domain.model

enum class PlanningItemType(
    val apiValue: String,
) {
    ROUTINE("routine"),
    MISSION("mission"),
    QUEST("quest"),
}
