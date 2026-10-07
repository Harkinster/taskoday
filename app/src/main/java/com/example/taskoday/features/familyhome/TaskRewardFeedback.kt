package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.CompletionReward

internal fun taskCompletionFeedback(pointsAwardedToMe: Int): String =
    taskCompletionFeedback(CompletionReward(xp = pointsAwardedToMe))

internal fun taskCompletionFeedback(reward: CompletionReward): String {
    if (reward.xp <= 0 && reward.flammeches <= 0 && reward.crystals <= 0) return "T\u00e2che termin\u00e9e."
    val values = buildList {
        if (reward.xp > 0) add("+${reward.xp} pts")
        if (reward.flammeches > 0) add("+${reward.flammeches} Flamm\u00e8ches")
        if (reward.crystals > 0) add("+${reward.crystals} Cristaux")
        if (reward.missionBonusCrystals > 0) add("Bonus Mission : +${reward.missionBonusCrystals} Cristaux")
    }
    return "T\u00e2che termin\u00e9e \u00b7 ${values.joinToString(" \u00b7 ")}"
}

internal fun taskValidationFeedback(pointsAwardedToMe: Int = 0): String =
    if (pointsAwardedToMe > 0) "T\u00e2che valid\u00e9e \u00b7 +$pointsAwardedToMe pts" else "T\u00e2che valid\u00e9e."

internal fun taskValidationFeedback(reward: CompletionReward): String =
    if (reward.xp > 0 || reward.flammeches > 0 || reward.crystals > 0) {
        taskCompletionFeedback(reward).replace("T\u00e2che termin\u00e9e", "T\u00e2che valid\u00e9e")
    } else {
        "T\u00e2che valid\u00e9e."
    }
