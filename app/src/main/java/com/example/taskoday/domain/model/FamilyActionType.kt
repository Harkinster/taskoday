package com.example.taskoday.domain.model

/** Product meaning of a FamilyTask. Historical uncategorised tasks remain collective. */
enum class FamilyActionType(val category: String) {
    HOUSE_QUEST("TASKODAY_HOUSE_QUEST"),
    PERSONAL_ROUTINE("TASKODAY_PERSONAL_ROUTINE"),
    PERSONAL_MISSION("TASKODAY_PERSONAL_MISSION"),
    ;

    companion object {
        fun fromCategory(value: String?): FamilyActionType =
            entries.firstOrNull { it.category.equals(value, ignoreCase = true) } ?: HOUSE_QUEST
    }
}
