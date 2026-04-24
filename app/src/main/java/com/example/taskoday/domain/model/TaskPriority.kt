package com.example.taskoday.domain.model

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT,
    ;

    companion object {
        fun fromStorage(value: String): TaskPriority =
            when (value) {
                "MEDIUM" -> NORMAL
                else -> entries.firstOrNull { it.name == value } ?: NORMAL
            }
    }

    fun label(): String =
        when (this) {
            LOW -> "Basse"
            NORMAL -> "Normale"
            HIGH -> "Haute"
            URGENT -> "Urgente"
        }
}
