package com.example.taskoday.features.gamification

import com.example.taskoday.data.remote.dto.ChestDropDto

internal fun canOpenChest(currentCrystals: Int, cost: Int): Boolean = currentCrystals >= cost && cost > 0

internal fun chestRevealText(drops: List<ChestDropDto>): String = drops.joinToString { drop ->
    "${drop.quantity} ${drop.title}"
}
