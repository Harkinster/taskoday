package com.example.taskoday.domain.model

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    ;

    fun label(): String =
        when (this) {
            TODO -> "À faire"
            IN_PROGRESS -> "En cours"
            DONE -> "Terminée"
        }
}
