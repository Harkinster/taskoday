package com.example.taskoday.core.ui.format

private val taskodayDisplayLabels =
    mapOf(
        "pomme_dragon" to "Pomme dragon",
        "petit_cristal" to "Petit cristal",
        "plume_douce" to "Plume douce",
        "pierre_chaude" to "Pierre chaude",
        "rune_ancienne" to "Rune ancienne",
        "fragment_oeuf" to "Fragment d'œuf",
        "oeuf_braise" to "Œuf Braise",
        "essence_braise" to "Essence Braise",
        "oeuf_lunaire" to "Œuf Lunaire",
        "essence_lunaire" to "Essence Lunaire",
        "common" to "Commun",
        "rare" to "Rare",
        "epic" to "Épique",
        "material" to "Matériau",
        "artifact" to "Artefact",
        "egg" to "Œuf",
        "essence" to "Essence",
        "fragment" to "Fragment",
        "consumable" to "Consommable",
        "fire" to "Feu",
    )

fun String.toTaskodayDisplayLabel(): String {
    val normalized = trim().lowercase().replace('-', '_').replace(' ', '_')
    return taskodayDisplayLabels[normalized]
        ?: normalized
            .replace('_', ' ')
            .replaceFirstChar { first -> first.uppercase() }
}
