package com.example.taskoday.domain.model

enum class DayPart {
    MATIN,
    MATINEE,
    MIDI,
    APRES_MIDI,
    SOIR,
    SOIREE,
    ;

    fun label(): String =
        when (this) {
            MATIN -> "Matin"
            MATINEE -> "Matin\u00E9e"
            MIDI -> "Midi"
            APRES_MIDI -> "Apr\u00E8s-midi"
            SOIR -> "Soir"
            SOIREE -> "Soir\u00E9e"
        }

    fun emoji(): String =
        when (this) {
            MATIN -> "\uD83C\uDF1E"
            MATINEE -> "\uD83E\uDDF8"
            MIDI -> "\uD83C\uDF7D\uFE0F"
            APRES_MIDI -> "\uD83C\uDFAE"
            SOIR -> "\uD83C\uDF19"
            SOIREE -> "\u2728"
        }

    companion object {
        fun fromHour(hour: Int): DayPart =
            when (hour) {
                in 5..8 -> MATIN
                in 9..11 -> MATINEE
                in 12..13 -> MIDI
                in 14..17 -> APRES_MIDI
                in 18..20 -> SOIR
                else -> SOIREE
            }
    }
}
