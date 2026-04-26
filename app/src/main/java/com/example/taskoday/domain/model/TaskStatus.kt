package com.example.taskoday.domain.model

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    ;

    fun label(): String =
        when (this) {
            TODO -> "\u00C0 faire"
            IN_PROGRESS -> "En cours"
            DONE -> "Termin\u00E9e"
        }
}
