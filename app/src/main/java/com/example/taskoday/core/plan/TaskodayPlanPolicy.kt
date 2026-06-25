package com.example.taskoday.core.plan

enum class TaskodayPlanFeature(
    val displayName: String,
) {
    Child("enfant"),
    Routine("routine active"),
    Mission("mission active"),
    Quest("quete active"),
    Wish("souhait actif"),
}

object TaskodayPlanPolicy {
    const val isPremium: Boolean = false

    const val freeMaxChildren: Int = 1
    const val freeMaxActiveRoutines: Int = 5
    const val freeMaxActiveMissions: Int = 3
    const val freeMaxActiveQuests: Int = 1
    const val freeMaxActiveWishes: Int = 3

    private const val premiumSoftLimit: Int = 999

    fun limitFor(feature: TaskodayPlanFeature): Int =
        if (isPremium) {
            premiumSoftLimit
        } else {
            when (feature) {
                TaskodayPlanFeature.Child -> freeMaxChildren
                TaskodayPlanFeature.Routine -> freeMaxActiveRoutines
                TaskodayPlanFeature.Mission -> freeMaxActiveMissions
                TaskodayPlanFeature.Quest -> freeMaxActiveQuests
                TaskodayPlanFeature.Wish -> freeMaxActiveWishes
            }
        }

    fun canCreate(
        feature: TaskodayPlanFeature,
        currentCount: Int,
    ): Boolean = currentCount < limitFor(feature)

    fun usageLabel(
        feature: TaskodayPlanFeature,
        currentCount: Int,
    ): String {
        val limit = limitFor(feature)
        return if (isPremium) {
            "Premium actif : limite levee."
        } else {
            "${currentCount.coerceAtLeast(0)} / $limit ${feature.displayName}(s) gratuits"
        }
    }

    fun limitReachedMessage(): String =
        "Limite gratuite atteinte. Le Premium permettra d'en creer davantage."
}
