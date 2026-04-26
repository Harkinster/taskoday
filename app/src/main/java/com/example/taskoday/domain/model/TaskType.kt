package com.example.taskoday.domain.model

enum class TaskType {
    DAILY,
    ONE_TIME,
    ;

    fun label(): String =
        when (this) {
            DAILY -> "Routine"
            ONE_TIME -> "Mission"
        }
}
