package com.example.taskoday.features.gamification

import com.example.taskoday.data.remote.dto.ChestDropDto

internal fun canOpenChest(currentCrystals: Int, cost: Int): Boolean = currentCrystals >= cost && cost > 0

internal fun chestRevealText(drops: List<ChestDropDto>): String = drops.joinToString { drop ->
    "${drop.quantity} ${drop.title}"
}

internal fun wishRequestStatusLabel(status: String): String =
    when (status.uppercase()) {
        "PENDING" -> "En attente"
        "APPROVED" -> "Acceptée"
        "REJECTED" -> "Refusée"
        "CANCELLED", "CANCELED" -> "Annulée"
        else -> "Statut indisponible"
    }

internal fun chestTypeLabel(chestType: String): String =
    when (chestType.uppercase()) {
        "COMMON" -> "commun"
        "RARE" -> "rare"
        "EPIC" -> "épique"
        else -> "spécial"
    }
