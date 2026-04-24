package com.example.taskoday.domain.model

enum class RoutineFrequency {
    DAILY,
    WEEKLY,
    CUSTOM,
    ;

    fun label(): String =
        when (this) {
            DAILY -> "Quotidienne"
            WEEKLY -> "Hebdomadaire"
            CUSTOM -> "Personnalisée"
        }
}
