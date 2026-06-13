package com.example.taskoday.features.home

import com.example.taskoday.domain.model.CompletionReward
import com.example.taskoday.domain.model.PlanningItemType

internal fun rewardPreviewFor(type: PlanningItemType): CompletionReward =
    when (type) {
        PlanningItemType.ROUTINE -> CompletionReward(xp = 5, flammeches = 2, crystals = 1)
        PlanningItemType.MISSION -> CompletionReward(xp = 15, flammeches = 6, crystals = 3)
        PlanningItemType.QUEST -> CompletionReward(xp = 30, flammeches = 12, crystals = 6)
    }

internal fun CompletionReward.compactLabel(): String =
    buildList {
        if (xp > 0) add("+$xp XP")
        if (flammeches > 0) add("+$flammeches ${if (flammeches == 1) "Flammèche" else "Flammèches"}")
        if (crystals > 0) add("+$crystals ${if (crystals == 1) "Cristal" else "Cristaux"}")
    }.joinToString(" • ")
