package com.example.taskoday.domain.model

import java.util.Locale

/** Product meaning of a FamilyTask. Historical uncategorised tasks remain collective. */
enum class FamilyActionType(val category: String) {
    HOUSE_QUEST("TASKODAY_HOUSE_QUEST"),
    PERSONAL_ROUTINE("TASKODAY_PERSONAL_ROUTINE"),
    PERSONAL_MISSION("TASKODAY_PERSONAL_MISSION"),
    ;

    companion object {
        /** Only documented legacy values may default to the collective type. */
        fun fromCategory(value: String?): FamilyActionType =
            when (value?.trim()?.uppercase(Locale.ROOT)) {
                null, "", "MAISON", HOUSE_QUEST.category -> HOUSE_QUEST
                PERSONAL_ROUTINE.category -> PERSONAL_ROUTINE
                PERSONAL_MISSION.category -> PERSONAL_MISSION
                else -> throw IllegalArgumentException("Catégorie d'action inconnue : $value")
            }
    }
}
