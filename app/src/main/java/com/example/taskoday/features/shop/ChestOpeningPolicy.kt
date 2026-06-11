package com.example.taskoday.features.shop

import com.example.taskoday.data.repository.toRemoteUserMessage
import retrofit2.HttpException

internal data class ChestActionState(
    val label: String,
    val enabled: Boolean,
    val balanceLabel: String,
)

internal fun chestActionState(
    crystalsBalance: Int?,
    crystalCost: Int,
    hasRemoteCatalog: Boolean,
    isSubmitting: Boolean,
): ChestActionState {
    if (!hasRemoteCatalog || crystalsBalance == null) {
        return ChestActionState(
            label = "Aperçu",
            enabled = true,
            balanceLabel = "$crystalCost Cristaux",
        )
    }

    val balanceLabel = "$crystalsBalance / $crystalCost Cristaux"
    return when {
        isSubmitting ->
            ChestActionState(
                label = "Ouverture...",
                enabled = false,
                balanceLabel = balanceLabel,
            )

        crystalsBalance < crystalCost ->
            ChestActionState(
                label = "Cristaux insuffisants",
                enabled = false,
                balanceLabel = balanceLabel,
            )

        else ->
            ChestActionState(
                label = "Ouvrir",
                enabled = true,
                balanceLabel = balanceLabel,
            )
    }
}

internal fun missingCrystalsMessage(
    crystalsBalance: Int,
    crystalCost: Int,
): String {
    val missing = (crystalCost - crystalsBalance).coerceAtLeast(0)
    return if (missing == 1) {
        "Il te manque 1 Cristal pour ouvrir ce coffre."
    } else {
        "Il te manque $missing Cristaux pour ouvrir ce coffre."
    }
}

internal fun Throwable.toChestOpenUserMessage(): String {
    if (this is HttpException && code() == 400) {
        val backendBody = response()?.errorBody()?.string().orEmpty()
        if (backendBody.contains("Cristaux insuffisants", ignoreCase = true)) {
            val balances =
                Regex("""balance=(\d+),\s*required=(\d+)""")
                    .find(backendBody)
                    ?.groupValues
                    ?.drop(1)
                    ?.mapNotNull(String::toIntOrNull)
            if (balances?.size == 2) {
                return missingCrystalsMessage(
                    crystalsBalance = balances[0],
                    crystalCost = balances[1],
                )
            }
            return "Cristaux insuffisants."
        }
    }
    return toRemoteUserMessage("Impossible d'ouvrir ce coffre.")
}
