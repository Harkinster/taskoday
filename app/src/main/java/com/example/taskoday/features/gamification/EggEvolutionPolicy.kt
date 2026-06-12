package com.example.taskoday.features.gamification

import com.example.taskoday.core.ui.format.toTaskodayDisplayLabel
import com.example.taskoday.data.remote.dto.EggDto
import com.example.taskoday.data.remote.dto.InventoryDto

data class EggEvolutionActionState(
    val enabled: Boolean,
    val label: String,
    val requirementsLabel: String,
)

fun eggEvolutionActionState(
    egg: EggDto?,
    inventory: InventoryDto?,
    isSubmitting: Boolean = false,
): EggEvolutionActionState {
    if (isSubmitting) {
        return EggEvolutionActionState(
            enabled = false,
            label = "Évolution...",
            requirementsLabel = "Évolution en cours.",
        )
    }
    if (egg == null) {
        return EggEvolutionActionState(
            enabled = false,
            label = "Pas encore prêt",
            requirementsLabel = "Œuf non disponible.",
        )
    }
    if (egg.nextState == null) {
        return EggEvolutionActionState(
            enabled = false,
            label = "Évolution terminée",
            requirementsLabel = "Cet œuf a atteint son dernier état.",
        )
    }

    val ownedByKey = inventory?.items.orEmpty().associate { item -> item.key to item.quantity }
    val requirements =
        egg.requirements.entries
            .sortedBy { (key, _) -> key }
            .joinToString(", ") { (key, required) ->
                "${ownedByKey[key] ?: 0} / $required ${key.toTaskodayDisplayLabel()}"
            }
    val missing =
        egg.requirements.entries.filter { (key, required) ->
            (ownedByKey[key] ?: 0) < required
        }
    val canEvolve = inventory != null && missing.isEmpty()

    return EggEvolutionActionState(
        enabled = canEvolve,
        label =
            when {
                !canEvolve -> "Ressources insuffisantes"
                egg.state.equals("hatching", ignoreCase = true) -> "Faire éclore"
                else -> "Évoluer"
            },
        requirementsLabel =
            when {
                requirements.isBlank() && canEvolve -> "Prêt à évoluer."
                requirements.isBlank() -> "Ressources indisponibles."
                canEvolve -> "Prêt à évoluer • Requis : $requirements"
                else -> "Ressources insuffisantes • Requis : $requirements"
            },
    )
}

fun isBestiaryFamilyDiscovered(
    discovered: Boolean,
    eggOwned: Boolean,
    dragonOwned: Boolean,
): Boolean = discovered || eggOwned || dragonOwned
