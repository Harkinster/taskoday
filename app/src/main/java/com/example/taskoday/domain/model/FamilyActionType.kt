package com.example.taskoday.domain.model

import java.util.Locale

enum class FamilyActionScope { PERSONAL, HOUSE }

enum class FamilyActionKind { ROUTINE, MISSION, QUEST }

/** Supported scope/kind pair. category is retained only as a wire compatibility value. */
enum class FamilyActionType(val scope: FamilyActionScope, val kind: FamilyActionKind, val category: String) {
    HOUSE_QUEST(FamilyActionScope.HOUSE, FamilyActionKind.QUEST, "TASKODAY_HOUSE_QUEST"),
    HOUSE_ROUTINE(FamilyActionScope.HOUSE, FamilyActionKind.ROUTINE, "TASKODAY_HOUSE_ROUTINE"),
    HOUSE_MISSION(FamilyActionScope.HOUSE, FamilyActionKind.MISSION, "TASKODAY_HOUSE_MISSION"),
    PERSONAL_ROUTINE(FamilyActionScope.PERSONAL, FamilyActionKind.ROUTINE, "TASKODAY_PERSONAL_ROUTINE"),
    PERSONAL_MISSION(FamilyActionScope.PERSONAL, FamilyActionKind.MISSION, "TASKODAY_PERSONAL_MISSION"),
    ;

    companion object {
        /** Only documented legacy values may default to the collective type. */
        fun fromCategory(value: String?): FamilyActionType =
            when (value?.trim()?.uppercase(Locale.ROOT)) {
                null, "", "MAISON", HOUSE_QUEST.category -> HOUSE_QUEST
                PERSONAL_ROUTINE.category -> PERSONAL_ROUTINE
                PERSONAL_MISSION.category -> PERSONAL_MISSION
                HOUSE_ROUTINE.category -> HOUSE_ROUTINE
                HOUSE_MISSION.category -> HOUSE_MISSION
                else -> throw IllegalArgumentException("Catégorie d'action inconnue : $value")
            }

        fun fromWire(scope: String?, kind: String?, category: String?): FamilyActionType {
            if (scope == null && kind == null) return fromCategory(category)
            val parsedScope = FamilyActionScope.valueOf(requireNotNull(scope))
            val parsedKind = FamilyActionKind.valueOf(requireNotNull(kind))
            val result = entries.singleOrNull { it.scope == parsedScope && it.kind == parsedKind }
                ?: throw IllegalArgumentException("Combinaison d'action non supportée.")
            if (category != null && fromCategory(category) != result) error("Catégorie incompatible avec scope/kind.")
            return result
        }
    }
}
