package com.example.taskoday.core.ui.component.fantasy

import com.example.taskoday.R

data class NestAsset(
    val futureName: String,
    val placeholderResId: Int,
) {
    val resId: Int
        get() = placeholderResId
}

object NestAssets {
    val EggSolarSleeping = NestAsset("asset_egg_solar_sleeping", R.drawable.placeholder_egg_luminous)
    val EggSolarWarm = NestAsset("asset_egg_solar_warm", R.drawable.placeholder_egg_luminous)
    val EggSolarGlowing = NestAsset("asset_egg_solar_glowing", R.drawable.placeholder_egg_luminous)
    val EggSolarCracked = NestAsset("asset_egg_solar_cracked", R.drawable.placeholder_egg_luminous)
    val EggSolarHatching = NestAsset("asset_egg_solar_hatching", R.drawable.placeholder_egg_luminous)

    val DragonEmberBaby = NestAsset("asset_dragon_ember_baby", R.drawable.placeholder_dragon_ember)
    val DragonEmberYoung = NestAsset("asset_dragon_ember_young", R.drawable.placeholder_dragon_ember)
    val DragonEmberMedium = NestAsset("asset_dragon_ember_medium", R.drawable.placeholder_dragon_ember)
    val DragonEmberLarge = NestAsset("asset_dragon_ember_large", R.drawable.placeholder_dragon_ember)

    val ChestCommon = NestAsset("asset_chest_common", R.drawable.placeholder_chest)
    val ChestRare = NestAsset("asset_chest_rare", R.drawable.placeholder_chest)
    val ChestEpic = NestAsset("asset_chest_epic", R.drawable.placeholder_chest)

    val ScrollApproved = NestAsset("asset_scroll_approved", R.drawable.placeholder_scroll)
    val Flameche = NestAsset("asset_flameche", R.drawable.placeholder_flame)
    val Crystal = NestAsset("asset_crystal", R.drawable.placeholder_crystal)
    val NestBackground = NestAsset("asset_nest_background", R.drawable.placeholder_nest)
}
