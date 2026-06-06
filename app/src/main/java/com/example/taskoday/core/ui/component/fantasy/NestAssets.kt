package com.example.taskoday.core.ui.component.fantasy

import com.example.taskoday.R

data class NestAsset(
    val futureName: String,
    val resId: Int,
)

data class DragonVisualFamily(
    val key: String,
    val name: String,
    val lineage: String,
    val lineageKey: String,
    val theme: String,
    val colors: String,
)

object DragonVisualRegistry {
    val families =
        listOf(
            DragonVisualFamily("fulmio", "Fulmio", "Tempete", "storm", "Mouvement, activite, defis physiques", "Bleu electrique, violet, blanc lumineux"),
            DragonVisualFamily("sylvyn", "Sylvyn", "Racine", "root", "Rangement, maison, aide familiale, stabilite", "Vert mousse, brun bois, dore naturel"),
            DragonVisualFamily("phenor", "Phenor", "Phenix", "phoenix", "Reprise apres pause, perseverance, renaissance", "Orange, or, rouge doux"),
            DragonVisualFamily("lunarys", "Lunarys", "Lunaire", "lunar", "Routines du soir, calme, sommeil", "Bleu nuit, violet, dore doux"),
            DragonVisualFamily("pyron", "Pyron", "Braise", "ember", "Progression principale, motivation, flamme interieure", "Orange braise, noir chaud, dore"),
            DragonVisualFamily("chronyx", "Chronyx", "Chronos", "chronos", "Regularite, series, patience, temps", "Dore, violet, brun ancien"),
            DragonVisualFamily("ambrio", "Ambrio", "Coeur", "heart", "Entraide, famille, gentillesse, comportement positif", "Dore, rose doux, orange chaud"),
            DragonVisualFamily("cristao", "Cristao", "Cristal", "crystal", "Devoirs, lecture, apprentissage, concentration", "Bleu cristallin, cyan, violet magique"),
        )

    fun family(key: String): DragonVisualFamily =
        families.firstOrNull { it.key == key.normalizeAssetKey() } ?: families.first { it.key == "pyron" }
}

// Ne pas referencer directement R.drawable depuis les ecrans : toujours passer par NestAssets.
object NestAssets {
    // Remplacer ce placeholder par l'asset reel quand disponible.
    val EggFulmioSleeping = NestAsset("asset_egg_fulmio_sleeping", R.drawable.asset_egg_fulmio_sleeping)
    val EggFulmioWarm = NestAsset("asset_egg_fulmio_warm", R.drawable.asset_egg_fulmio_warm)
    val EggFulmioGlowing = NestAsset("asset_egg_fulmio_glowing", R.drawable.asset_egg_fulmio_glowing)
    val EggFulmioCracked = NestAsset("asset_egg_fulmio_cracked", R.drawable.asset_egg_fulmio_cracked)
    val EggFulmioHatching = NestAsset("asset_egg_fulmio_hatching", R.drawable.asset_egg_fulmio_hatching)

    val EggSylvynSleeping = NestAsset("asset_egg_sylvyn_sleeping", R.drawable.asset_egg_sylvyn_sleeping)
    val EggSylvynWarm = NestAsset("asset_egg_sylvyn_warm", R.drawable.asset_egg_sylvyn_warm)
    val EggSylvynGlowing = NestAsset("asset_egg_sylvyn_glowing", R.drawable.asset_egg_sylvyn_glowing)
    val EggSylvynCracked = NestAsset("asset_egg_sylvyn_cracked", R.drawable.asset_egg_sylvyn_cracked)
    val EggSylvynHatching = NestAsset("asset_egg_sylvyn_hatching", R.drawable.asset_egg_sylvyn_hatching)

    val EggPhenorSleeping = NestAsset("asset_egg_phenor_sleeping", R.drawable.asset_egg_phenor_sleeping)
    val EggPhenorWarm = NestAsset("asset_egg_phenor_warm", R.drawable.asset_egg_phenor_warm)
    val EggPhenorGlowing = NestAsset("asset_egg_phenor_glowing", R.drawable.asset_egg_phenor_glowing)
    val EggPhenorCracked = NestAsset("asset_egg_phenor_cracked", R.drawable.asset_egg_phenor_cracked)
    val EggPhenorHatching = NestAsset("asset_egg_phenor_hatching", R.drawable.asset_egg_phenor_hatching)

    val EggLunarysSleeping = NestAsset("asset_egg_lunarys_sleeping", R.drawable.asset_egg_lunarys_sleeping)
    val EggLunarysWarm = NestAsset("asset_egg_lunarys_warm", R.drawable.asset_egg_lunarys_warm)
    val EggLunarysGlowing = NestAsset("asset_egg_lunarys_glowing", R.drawable.asset_egg_lunarys_glowing)
    val EggLunarysCracked = NestAsset("asset_egg_lunarys_cracked", R.drawable.asset_egg_lunarys_cracked)
    val EggLunarysHatching = NestAsset("asset_egg_lunarys_hatching", R.drawable.asset_egg_lunarys_hatching)

    val EggPyronSleeping = NestAsset("asset_egg_pyron_sleeping", R.drawable.asset_egg_pyron_sleeping)
    val EggPyronWarm = NestAsset("asset_egg_pyron_warm", R.drawable.asset_egg_pyron_warm)
    val EggPyronGlowing = NestAsset("asset_egg_pyron_glowing", R.drawable.asset_egg_pyron_glowing)
    val EggPyronCracked = NestAsset("asset_egg_pyron_cracked", R.drawable.asset_egg_pyron_cracked)
    val EggPyronHatching = NestAsset("asset_egg_pyron_hatching", R.drawable.asset_egg_pyron_hatching)

    val EggChronyxSleeping = NestAsset("asset_egg_chronyx_sleeping", R.drawable.asset_egg_chronyx_sleeping)
    val EggChronyxWarm = NestAsset("asset_egg_chronyx_warm", R.drawable.asset_egg_chronyx_warm)
    val EggChronyxGlowing = NestAsset("asset_egg_chronyx_glowing", R.drawable.asset_egg_chronyx_glowing)
    val EggChronyxCracked = NestAsset("asset_egg_chronyx_cracked", R.drawable.asset_egg_chronyx_cracked)
    val EggChronyxHatching = NestAsset("asset_egg_chronyx_hatching", R.drawable.asset_egg_chronyx_hatching)

    val EggAmbrioSleeping = NestAsset("asset_egg_ambrio_sleeping", R.drawable.asset_egg_ambrio_sleeping)
    val EggAmbrioWarm = NestAsset("asset_egg_ambrio_warm", R.drawable.asset_egg_ambrio_warm)
    val EggAmbrioGlowing = NestAsset("asset_egg_ambrio_glowing", R.drawable.asset_egg_ambrio_glowing)
    val EggAmbrioCracked = NestAsset("asset_egg_ambrio_cracked", R.drawable.asset_egg_ambrio_cracked)
    val EggAmbrioHatching = NestAsset("asset_egg_ambrio_hatching", R.drawable.asset_egg_ambrio_hatching)

    val EggCristaoSleeping = NestAsset("asset_egg_cristao_sleeping", R.drawable.asset_egg_cristao_sleeping)
    val EggCristaoWarm = NestAsset("asset_egg_cristao_warm", R.drawable.asset_egg_cristao_warm)
    val EggCristaoGlowing = NestAsset("asset_egg_cristao_glowing", R.drawable.asset_egg_cristao_glowing)
    val EggCristaoCracked = NestAsset("asset_egg_cristao_cracked", R.drawable.asset_egg_cristao_cracked)
    val EggCristaoHatching = NestAsset("asset_egg_cristao_hatching", R.drawable.asset_egg_cristao_hatching)

    val DragonFulmioBaby = NestAsset("asset_dragon_fulmio_baby", R.drawable.asset_dragon_fulmio_baby)
    val DragonFulmioYoung = NestAsset("asset_dragon_fulmio_young", R.drawable.asset_dragon_fulmio_young)
    val DragonFulmioMedium = NestAsset("asset_dragon_fulmio_medium", R.drawable.asset_dragon_fulmio_medium)
    val DragonFulmioLarge = NestAsset("asset_dragon_fulmio_large", R.drawable.asset_dragon_fulmio_large)
    val DragonFulmioLegendary = NestAsset("asset_dragon_fulmio_legendary", R.drawable.asset_dragon_fulmio_legendary)

    val DragonSylvynBaby = NestAsset("asset_dragon_sylvyn_baby", R.drawable.asset_dragon_sylvyn_baby)
    val DragonSylvynYoung = NestAsset("asset_dragon_sylvyn_young", R.drawable.asset_dragon_sylvyn_young)
    val DragonSylvynMedium = NestAsset("asset_dragon_sylvyn_medium", R.drawable.asset_dragon_sylvyn_medium)
    val DragonSylvynLarge = NestAsset("asset_dragon_sylvyn_large", R.drawable.asset_dragon_sylvyn_large)
    val DragonSylvynLegendary = NestAsset("asset_dragon_sylvyn_legendary", R.drawable.asset_dragon_sylvyn_legendary)

    val DragonPhenorBaby = NestAsset("asset_dragon_phenor_baby", R.drawable.asset_dragon_phenor_baby)
    val DragonPhenorYoung = NestAsset("asset_dragon_phenor_young", R.drawable.asset_dragon_phenor_young)
    val DragonPhenorMedium = NestAsset("asset_dragon_phenor_medium", R.drawable.asset_dragon_phenor_medium)
    val DragonPhenorLarge = NestAsset("asset_dragon_phenor_large", R.drawable.asset_dragon_phenor_large)
    val DragonPhenorLegendary = NestAsset("asset_dragon_phenor_legendary", R.drawable.asset_dragon_phenor_legendary)

    val DragonLunarysBaby = NestAsset("asset_dragon_lunarys_baby", R.drawable.asset_dragon_lunarys_baby)
    val DragonLunarysYoung = NestAsset("asset_dragon_lunarys_young", R.drawable.asset_dragon_lunarys_young)
    val DragonLunarysMedium = NestAsset("asset_dragon_lunarys_medium", R.drawable.asset_dragon_lunarys_medium)
    val DragonLunarysLarge = NestAsset("asset_dragon_lunarys_large", R.drawable.asset_dragon_lunarys_large)
    val DragonLunarysLegendary = NestAsset("asset_dragon_lunarys_legendary", R.drawable.asset_dragon_lunarys_legendary)

    val DragonPyronBaby = NestAsset("asset_dragon_pyron_baby", R.drawable.asset_dragon_pyron_baby)
    val DragonPyronYoung = NestAsset("asset_dragon_pyron_young", R.drawable.asset_dragon_pyron_young)
    val DragonPyronMedium = NestAsset("asset_dragon_pyron_medium", R.drawable.asset_dragon_pyron_medium)
    val DragonPyronLarge = NestAsset("asset_dragon_pyron_large", R.drawable.asset_dragon_pyron_large)
    val DragonPyronLegendary = NestAsset("asset_dragon_pyron_legendary", R.drawable.asset_dragon_pyron_legendary)

    val DragonChronyxBaby = NestAsset("asset_dragon_chronyx_baby", R.drawable.asset_dragon_chronyx_baby)
    val DragonChronyxYoung = NestAsset("asset_dragon_chronyx_young", R.drawable.asset_dragon_chronyx_young)
    val DragonChronyxMedium = NestAsset("asset_dragon_chronyx_medium", R.drawable.asset_dragon_chronyx_medium)
    val DragonChronyxLarge = NestAsset("asset_dragon_chronyx_large", R.drawable.asset_dragon_chronyx_large)
    val DragonChronyxLegendary = NestAsset("asset_dragon_chronyx_legendary", R.drawable.asset_dragon_chronyx_legendary)

    val DragonAmbrioBaby = NestAsset("asset_dragon_ambrio_baby", R.drawable.asset_dragon_ambrio_baby)
    val DragonAmbrioYoung = NestAsset("asset_dragon_ambrio_young", R.drawable.asset_dragon_ambrio_young)
    val DragonAmbrioMedium = NestAsset("asset_dragon_ambrio_medium", R.drawable.asset_dragon_ambrio_medium)
    val DragonAmbrioLarge = NestAsset("asset_dragon_ambrio_large", R.drawable.asset_dragon_ambrio_large)
    val DragonAmbrioLegendary = NestAsset("asset_dragon_ambrio_legendary", R.drawable.asset_dragon_ambrio_legendary)

    val DragonCristaoBaby = NestAsset("asset_dragon_cristao_baby", R.drawable.asset_dragon_cristao_baby)
    val DragonCristaoYoung = NestAsset("asset_dragon_cristao_young", R.drawable.asset_dragon_cristao_young)
    val DragonCristaoMedium = NestAsset("asset_dragon_cristao_medium", R.drawable.asset_dragon_cristao_medium)
    val DragonCristaoLarge = NestAsset("asset_dragon_cristao_large", R.drawable.asset_dragon_cristao_large)
    val DragonCristaoLegendary = NestAsset("asset_dragon_cristao_legendary", R.drawable.asset_dragon_cristao_legendary)

    val ChestCommon = NestAsset("asset_chest_common", R.drawable.asset_chest_common)
    val ChestRare = NestAsset("asset_chest_rare", R.drawable.asset_chest_rare)
    val ChestEpic = NestAsset("asset_chest_epic", R.drawable.asset_chest_epic)

    val ScrollPending = NestAsset("asset_scroll_pending", R.drawable.asset_scroll_pending)
    val ScrollApproved = NestAsset("asset_scroll_approved", R.drawable.asset_scroll_approved)
    val ScrollUsed = NestAsset("asset_scroll_used", R.drawable.asset_scroll_used)
    val ScrollRefused = NestAsset("asset_scroll_refused", R.drawable.asset_scroll_refused)

    val InterfaceCristal = NestAsset("asset_icon_cristal", R.drawable.asset_icon_cristal)
    val InterfaceFlammeche = NestAsset("asset_icon_flammeche", R.drawable.asset_icon_flammeche)
    val InterfaceInventoryEmpty = NestAsset("asset_icon_inventaire_vide", R.drawable.asset_icon_inventaire_vide)
    val InterfaceNest = NestAsset("asset_icon_nid", R.drawable.asset_icon_nid)
    val InterfaceEggLocked = NestAsset("asset_icon_oeuf_verrouille", R.drawable.asset_icon_oeuf_verrouille)
    val LogoTaskoday = NestAsset("asset_logo_taskoday", R.drawable.taskoday_reference_wordmark)
    val SplashBackgroundTaskoday = NestAsset("asset_splash_background_taskoday", R.drawable.taskoday_world_background)

    val ItemFeatherQuill = NestAsset("asset_item_feather_quill", R.drawable.asset_item_feather_quill)
    val ItemGoldCoin = NestAsset("asset_item_gold_coin", R.drawable.asset_item_gold_coin)
    val ItemLantern = NestAsset("asset_item_lantern", R.drawable.asset_item_lantern)
    val ItemLeafSprout = NestAsset("asset_item_leaf_sprout", R.drawable.asset_item_leaf_sprout)
    val ItemMagicBook = NestAsset("asset_item_magic_book", R.drawable.asset_item_magic_book)
    val ItemMushroom = NestAsset("asset_item_mushroom", R.drawable.asset_item_mushroom)
    val ItemPotion = NestAsset("asset_item_potion", R.drawable.asset_item_potion)
    val ItemSealedLetter = NestAsset("asset_item_sealed_letter", R.drawable.asset_item_sealed_letter)
    val ItemStarCharm = NestAsset("asset_item_star_charm", R.drawable.asset_item_star_charm)
    val ItemWoodLogs = NestAsset("asset_item_wood_logs", R.drawable.asset_item_wood_logs)

    val PerchLevel1 = NestAsset("asset_perch_level1", R.drawable.asset_perch_level1)
    val PerchLevel2 = NestAsset("asset_perch_level2", R.drawable.asset_perch_level2)
    val PerchLevel3 = NestAsset("asset_perch_level3", R.drawable.asset_perch_level3)
    val PerchLevel4 = NestAsset("asset_perch_level4", R.drawable.asset_perch_level4)
    val PerchLevel5 = NestAsset("asset_perch_level5", R.drawable.asset_perch_level5)

    val EggSolarSleeping = EggPyronSleeping
    val EggSolarWarm = EggPyronWarm
    val EggSolarGlowing = EggPyronGlowing
    val EggSolarCracked = EggPyronCracked
    val EggSolarHatching = EggPyronHatching
    val EggUnknownLocked = InterfaceEggLocked

    val DragonEmberBaby = DragonPyronBaby
    val DragonEmberYoung = DragonPyronYoung
    val DragonEmberMedium = DragonPyronMedium
    val DragonEmberLarge = DragonPyronLarge
    val DragonEmberLegendary = DragonPyronLegendary

    val Flameche = InterfaceFlammeche
    val Crystal = InterfaceCristal
    val NestBackground = InterfaceNest
    val WishCaveBackground = InterfaceNest
    val InventoryEmpty = InterfaceInventoryEmpty

    private val eggAssets =
        mapOf(
            "fulmio" to mapOf("sleeping" to EggFulmioSleeping, "warm" to EggFulmioWarm, "glowing" to EggFulmioGlowing, "cracked" to EggFulmioCracked, "hatching" to EggFulmioHatching),
            "sylvyn" to mapOf("sleeping" to EggSylvynSleeping, "warm" to EggSylvynWarm, "glowing" to EggSylvynGlowing, "cracked" to EggSylvynCracked, "hatching" to EggSylvynHatching),
            "phenor" to mapOf("sleeping" to EggPhenorSleeping, "warm" to EggPhenorWarm, "glowing" to EggPhenorGlowing, "cracked" to EggPhenorCracked, "hatching" to EggPhenorHatching),
            "lunarys" to mapOf("sleeping" to EggLunarysSleeping, "warm" to EggLunarysWarm, "glowing" to EggLunarysGlowing, "cracked" to EggLunarysCracked, "hatching" to EggLunarysHatching),
            "pyron" to mapOf("sleeping" to EggPyronSleeping, "warm" to EggPyronWarm, "glowing" to EggPyronGlowing, "cracked" to EggPyronCracked, "hatching" to EggPyronHatching),
            "chronyx" to mapOf("sleeping" to EggChronyxSleeping, "warm" to EggChronyxWarm, "glowing" to EggChronyxGlowing, "cracked" to EggChronyxCracked, "hatching" to EggChronyxHatching),
            "ambrio" to mapOf("sleeping" to EggAmbrioSleeping, "warm" to EggAmbrioWarm, "glowing" to EggAmbrioGlowing, "cracked" to EggAmbrioCracked, "hatching" to EggAmbrioHatching),
            "cristao" to mapOf("sleeping" to EggCristaoSleeping, "warm" to EggCristaoWarm, "glowing" to EggCristaoGlowing, "cracked" to EggCristaoCracked, "hatching" to EggCristaoHatching),
        )

    private val dragonAssets =
        mapOf(
            "fulmio" to mapOf("baby" to DragonFulmioBaby, "young" to DragonFulmioYoung, "medium" to DragonFulmioMedium, "large" to DragonFulmioLarge, "legendary" to DragonFulmioLegendary),
            "sylvyn" to mapOf("baby" to DragonSylvynBaby, "young" to DragonSylvynYoung, "medium" to DragonSylvynMedium, "large" to DragonSylvynLarge, "legendary" to DragonSylvynLegendary),
            "phenor" to mapOf("baby" to DragonPhenorBaby, "young" to DragonPhenorYoung, "medium" to DragonPhenorMedium, "large" to DragonPhenorLarge, "legendary" to DragonPhenorLegendary),
            "lunarys" to mapOf("baby" to DragonLunarysBaby, "young" to DragonLunarysYoung, "medium" to DragonLunarysMedium, "large" to DragonLunarysLarge, "legendary" to DragonLunarysLegendary),
            "pyron" to mapOf("baby" to DragonPyronBaby, "young" to DragonPyronYoung, "medium" to DragonPyronMedium, "large" to DragonPyronLarge, "legendary" to DragonPyronLegendary),
            "chronyx" to mapOf("baby" to DragonChronyxBaby, "young" to DragonChronyxYoung, "medium" to DragonChronyxMedium, "large" to DragonChronyxLarge, "legendary" to DragonChronyxLegendary),
            "ambrio" to mapOf("baby" to DragonAmbrioBaby, "young" to DragonAmbrioYoung, "medium" to DragonAmbrioMedium, "large" to DragonAmbrioLarge, "legendary" to DragonAmbrioLegendary),
            "cristao" to mapOf("baby" to DragonCristaoBaby, "young" to DragonCristaoYoung, "medium" to DragonCristaoMedium, "large" to DragonCristaoLarge, "legendary" to DragonCristaoLegendary),
        )

    private val chestAssets =
        mapOf("common" to ChestCommon, "rare" to ChestRare, "epic" to ChestEpic)

    private val scrollAssets =
        mapOf("pending" to ScrollPending, "approved" to ScrollApproved, "used" to ScrollUsed, "refused" to ScrollRefused, "expired" to ScrollRefused)

    private val interfaceAssets =
        mapOf(
            "cristal" to InterfaceCristal,
            "crystal" to InterfaceCristal,
            "flammeche" to InterfaceFlammeche,
            "flameche" to InterfaceFlammeche,
            "inventaire_vide" to InterfaceInventoryEmpty,
            "inventory_empty" to InterfaceInventoryEmpty,
            "nid" to InterfaceNest,
            "nest" to InterfaceNest,
            "oeuf_verrouille" to InterfaceEggLocked,
            "egg_locked" to InterfaceEggLocked,
            "wish_cave" to InterfaceNest,
            "logo_taskoday" to LogoTaskoday,
            "splash_background_taskoday" to SplashBackgroundTaskoday,
        )

    private val inventoryItemAssets =
        mapOf(
            "feather_quill" to ItemFeatherQuill,
            "gold_coin" to ItemGoldCoin,
            "lantern" to ItemLantern,
            "leaf_sprout" to ItemLeafSprout,
            "magic_book" to ItemMagicBook,
            "mushroom" to ItemMushroom,
            "potion" to ItemPotion,
            "sealed_letter" to ItemSealedLetter,
            "star_charm" to ItemStarCharm,
            "wood_logs" to ItemWoodLogs,
        )

    // Remplacer ces fallbacks par les assets reels `asset_artifact_{dragon}_legendary` quand disponibles.
    private val legendaryArtifactAssets =
        DragonVisualRegistry.families.associate { family ->
            family.key to NestAsset("asset_artifact_${family.key}_legendary", ItemStarCharm.resId)
        }

    private val perchAssets =
        mapOf(1 to PerchLevel1, 2 to PerchLevel2, 3 to PerchLevel3, 4 to PerchLevel4, 5 to PerchLevel5)

    fun eggAsset(dragonKey: String, eggState: String): Int =
        eggAssets[dragonKey.normalizeAssetKey()]?.get(eggState.normalizeAssetKey())?.resId ?: EggUnknownLocked.resId

    fun dragonAsset(dragonKey: String, dragonStage: String): Int =
        dragonAssets[dragonKey.normalizeAssetKey()]?.get(dragonStage.normalizeAssetKey())?.resId ?: DragonEmberBaby.resId

    fun chestAsset(rarity: String): Int =
        chestAssets[rarity.normalizeAssetKey()]?.resId ?: ChestCommon.resId

    fun scrollAsset(status: String): Int =
        scrollAssets[status.normalizeAssetKey()]?.resId ?: ScrollPending.resId

    fun interfaceAsset(type: String): Int =
        interfaceAssets[type.normalizeAssetKey()]?.resId ?: InterfaceNest.resId

    fun inventoryItemAsset(type: String): Int =
        inventoryItemAssets[type.normalizeAssetKey()]?.resId ?: InterfaceCristal.resId

    fun itemAsset(itemKey: String): Int =
        inventoryItemAsset(itemKey)

    fun artifactAsset(dragonKey: String): Int =
        legendaryArtifactAssets[dragonKey.normalizeAssetKey()]?.resId ?: ItemStarCharm.resId

    fun perchAsset(level: Int): Int =
        perchAssets[level.coerceIn(1, 5)]?.resId ?: PerchLevel1.resId
}

private fun String.normalizeAssetKey(): String =
    trim()
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')
