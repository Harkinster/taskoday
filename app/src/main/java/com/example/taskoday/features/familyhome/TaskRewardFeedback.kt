package com.example.taskoday.features.familyhome

internal fun taskCompletionFeedback(pointsAwardedToMe: Int): String =
    if (pointsAwardedToMe > 0) "Tâche terminée · +$pointsAwardedToMe points" else "Tâche terminée."

internal fun taskValidationFeedback(pointsAwardedToMe: Int = 0): String =
    if (pointsAwardedToMe > 0) "Tâche validée · +$pointsAwardedToMe points" else "Tâche validée."
