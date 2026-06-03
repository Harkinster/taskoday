package com.example.taskoday.features.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.component.fantasy.ChestCard
import com.example.taskoday.core.ui.component.fantasy.DragonCard
import com.example.taskoday.core.ui.component.fantasy.EggProgressCard
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyHeader
import com.example.taskoday.core.ui.component.fantasy.FantasyProgressBar
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.InventoryLootCard
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.NestStatCard
import com.example.taskoday.core.ui.component.fantasy.ScrollCard
import com.example.taskoday.core.ui.component.fantasy.WishCard
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun NestScreen(
    onOpenPlanning: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenEggs: () -> Unit,
    onOpenDragons: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenScrolls: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    GamificationScaffold {
        item {
            FantasyHeader(
                title = "Le Nid",
                subtitle = "Le Gardien fait grandir son refuge avec ses routines, missions et quêtes.",
                assetResId = NestAssets.interfaceAsset("nid"),
                assetDescription = null,
                onAvatarClick = onOpenProfile,
            )
        }
        item {
            GuardianProgressCard(
                xp = 50,
                levelName = "Nid paisible",
                nextLevelLabel = "Nid éveillé",
                progress = 0.50f,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NestStatCard(
                    label = "Flammèches",
                    value = "20",
                    assetResId = NestAssets.interfaceAsset("flammeche"),
                    tone = FantasyTone.Ember,
                    modifier = Modifier.weight(1f),
                )
                NestStatCard(
                    label = "Cristaux",
                    value = "6",
                    assetResId = NestAssets.interfaceAsset("crystal"),
                    tone = FantasyTone.Violet,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            DragonCard(
                title = "Pyron",
                stage = "Bébé dragon braise",
                nextStep = "Prochaine évolution : jeune dragon",
                assetResId = NestAssets.dragonAsset("pyron", "baby"),
                contentDescription = "Dragon Pyron, stade bébé",
            )
        }
        item {
            EggProgressCard(
                title = "Œuf Pyron",
                status = "Chaleur douce",
                requirements = "Continue les routines simples pour nourrir sa flamme.",
                progress = 0.72f,
                assetResId = NestAssets.eggAsset("pyron", "glowing"),
                contentDescription = "Œuf Pyron lumineux",
            )
        }
        item {
            PerchOverviewCard(level = 1)
        }
        item {
            ChestCard(
                points = 4,
                pointsRequired = 5,
                unopenedChests = 1,
                assetResId = NestAssets.chestAsset("common"),
                contentDescription = "Coffre du Gardien",
            )
        }
        item {
            NavigationButtons(
                primaryLabel = "Journée",
                onPrimaryClick = onOpenPlanning,
                secondaryLabel = "Inventaire",
                onSecondaryClick = onOpenInventory,
            )
        }
        item {
            NavigationButtons(
                primaryLabel = "Œufs",
                onPrimaryClick = onOpenEggs,
                secondaryLabel = "Dragons",
                onSecondaryClick = onOpenDragons,
            )
        }
        item {
            NavigationButtons(
                primaryLabel = "Caverne aux Souhaits",
                onPrimaryClick = onOpenWishes,
                secondaryLabel = "Parchemins",
                onSecondaryClick = onOpenScrolls,
                outline = true,
            )
        }
    }
}

@Composable
fun InventoryScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Inventaire",
        subtitle = "Les petits trésors trouvés dans les coffres du Gardien.",
        assetResId = NestAssets.interfaceAsset("inventory_empty"),
        assetDescription = "Inventaire",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleLoot.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Inventaire vide",
                    message = "Continue tes quêtes pour attirer de nouveaux trésors.",
                    assetResId = NestAssets.interfaceAsset("inventory_empty"),
                    assetDescription = "Inventaire vide",
                )
            }
        } else {
            items(sampleLoot, key = { item -> item.key }) { item ->
                InventoryLootCard(
                    title = item.title,
                    rarity = item.rarityLabel,
                    quantity = item.quantity,
                    assetResId = item.assetResId,
                    contentDescription = "${item.title}, ${item.rarityLabel}",
                )
            }
        }
    }
}

@Composable
fun EggsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Œufs",
        subtitle = "Chaque œuf attend les bons objets pour éclore doucement.",
        assetResId = NestAssets.eggAsset("pyron", "sleeping"),
        assetDescription = "Œuf Pyron endormi",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleEggs.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun œuf découvert",
                    message = "Ton Nid attend sa prochaine étincelle.",
                    assetResId = NestAssets.interfaceAsset("egg_locked"),
                    assetDescription = "Œuf inconnu verrouillé",
                )
            }
        } else {
            items(sampleEggs, key = { egg -> egg.key }) { egg ->
                EggProgressCard(
                    title = egg.title,
                    status = egg.status,
                    requirements = egg.requirements,
                    progress = egg.progress,
                    assetResId = egg.assetResId,
                    contentDescription = egg.contentDescription,
                    locked = egg.locked,
                )
            }
        }
    }
}

@Composable
fun DragonsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Dragons",
        subtitle = "Les compagnons braise débloqués dans Le Nid.",
        assetResId = NestAssets.dragonAsset("pyron", "baby"),
        assetDescription = "Dragon Pyron bébé",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleDragons.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun dragon débloqué",
                    message = "Ton dragon t'attend pour la prochaine aventure.",
                    assetResId = NestAssets.dragonAsset("pyron", "baby"),
                    assetDescription = "Dragon Pyron bébé",
                )
            }
        } else {
            items(sampleDragons, key = { dragon -> dragon.key }) { dragon ->
                DragonCard(
                    title = dragon.title,
                    stage = dragon.stage,
                    nextStep = dragon.nextStep,
                    assetResId = dragon.assetResId,
                    contentDescription = dragon.contentDescription,
                )
            }
        }
    }
}

@Composable
fun WishesCaveScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Caverne aux Souhaits",
        subtitle = "Les Flammèches servent uniquement aux Souhaits créés par les parents.",
        assetResId = NestAssets.interfaceAsset("wish_cave"),
        assetDescription = "Caverne aux Souhaits",
        onOpenProfile = onOpenProfile,
    ) {
        item {
            NestStatCard(
                label = "Flammèches disponibles",
                value = "20",
                assetResId = NestAssets.interfaceAsset("flammeche"),
                tone = FantasyTone.Ember,
            )
        }
        if (sampleWishes.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun Souhait pour le moment",
                    message = "La Caverne aux Souhaits attend une nouvelle idée de parent.",
                    assetResId = NestAssets.interfaceAsset("wish_cave"),
                    assetDescription = "Caverne aux Souhaits",
                )
            }
        } else {
            items(sampleWishes, key = { wish -> wish.title }) { wish ->
                WishCard(
                    title = wish.title,
                    description = wish.description,
                    costLabel = "${wish.cost} Flammèches",
                    contentDescription = "Flammèche",
                    onMakeWish = {},
                )
            }
        }
    }
}

@Composable
fun ScrollsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Parchemins",
        subtitle = "Les Souhaits validés deviennent des Parchemins à utiliser en famille.",
        assetResId = NestAssets.scrollAsset("approved"),
        assetDescription = "Parchemin",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleScrolls.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun Parchemin pour le moment",
                    message = "Les Souhaits validés apparaîtront ici tranquillement.",
                    assetResId = NestAssets.scrollAsset("pending"),
                    assetDescription = "Parchemin",
                )
            }
        } else {
            items(sampleScrolls, key = { scroll -> scroll.code }) { scroll ->
                ScrollCard(
                    title = scroll.title,
                    code = scroll.code,
                    status = scroll.status,
                    assetResId = NestAssets.scrollAsset(scroll.statusKey),
                    contentDescription = "Parchemin ${scroll.status}",
                )
            }
        }
    }
}

@Composable
fun ParentRewardsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Souhaits parent",
        subtitle = "Valider, refuser ou suivre les demandes du Gardien.",
        assetResId = NestAssets.scrollAsset("pending"),
        assetDescription = "Parchemin",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleRequests.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucune demande en attente",
                    message = "Les Souhaits du Gardien apparaîtront ici quand ils seront prêts.",
                    assetResId = NestAssets.scrollAsset("pending"),
                    assetDescription = "Parchemin",
                )
            }
        } else {
            items(sampleRequests, key = { request -> request.childName + request.title }) { request ->
                ParentRequestCard(request = request)
            }
        }
    }
}

@Composable
private fun GuardianProgressCard(
    xp: Int,
    levelName: String,
    nextLevelLabel: String,
    progress: Float,
) {
    FantasyCard(tone = FantasyTone.Gold) {
        Text(text = "Gardien", style = MaterialTheme.typography.titleLarge, color = WoodBrownDark)
        Text(text = "$xp XP du Gardien", style = MaterialTheme.typography.headlineSmall, color = WoodBrownDark)
        FantasyProgressBar(progress = progress)
        Text(
            text = "$levelName vers $nextLevelLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
    }
}

@Composable
private fun PerchOverviewCard(level: Int) {
    FantasyCard(tone = FantasyTone.Moss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(
                assetResId = NestAssets.perchAsset(level),
                contentDescription = "Perchoir niveau $level",
                size = 62.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Perchoir du Nid", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                Text(text = "Niveau $level", style = MaterialTheme.typography.bodyMedium, color = MossGreen)
                Text(
                    text = "Le dragon actif garde sa place au chaud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun ParentRequestCard(request: ParentRequestUiItem) {
    FantasyCard(tone = FantasyTone.Violet) {
        Text(text = request.title, style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
        Text(text = request.childName, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        Text(text = request.status, style = MaterialTheme.typography.labelLarge, color = EmberOrange)
    }
}

@Composable
private fun NavigationButtons(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit,
    outline: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        FantasyButton(
            text = primaryLabel,
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f),
            style = if (outline) FantasyButtonStyle.Outline else FantasyButtonStyle.Filled,
        )
        FantasyButton(
            text = secondaryLabel,
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f),
            style = FantasyButtonStyle.Outline,
        )
    }
}

@Composable
private fun GamificationScaffold(
    content: LazyListScope.() -> Unit,
) {
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        FantasyScreenBackground(modifier = Modifier.statusBarsPadding().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.medium),
                contentPadding = PaddingValues(top = MaterialTheme.spacing.large, bottom = MaterialTheme.spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                content = content,
            )
        }
    }
}

@Composable
private fun GamificationListScreen(
    title: String,
    subtitle: String,
    assetResId: Int,
    assetDescription: String,
    onOpenProfile: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    GamificationScaffold {
        item {
            FantasyHeader(
                title = title,
                subtitle = subtitle,
                assetResId = assetResId,
                assetDescription = assetDescription,
                onAvatarClick = onOpenProfile,
            )
        }
        content()
    }
}

data class LootUiItem(
    val key: String,
    val title: String,
    val rarityLabel: String,
    val quantity: Int,
    val assetResId: Int,
)

data class EggUiItem(
    val key: String,
    val title: String,
    val status: String,
    val requirements: String,
    val progress: Float,
    val assetResId: Int,
    val locked: Boolean = false,
    val contentDescription: String = title,
)

data class DragonUiItem(
    val key: String,
    val title: String,
    val stage: String,
    val nextStep: String,
    val assetResId: Int,
    val contentDescription: String = title,
)

data class WishUiItem(
    val title: String,
    val description: String,
    val cost: Int,
)

data class ScrollUiItem(
    val title: String,
    val code: String,
    val status: String,
    val statusKey: String,
)

data class ParentRequestUiItem(
    val childName: String,
    val title: String,
    val status: String,
)

private val sampleLoot =
    listOf(
        LootUiItem("lanterne", "Lanterne", "commun", 1, NestAssets.inventoryItemAsset("lantern")),
        LootUiItem("pousse_feuille", "Pousse de feuille", "commun", 4, NestAssets.inventoryItemAsset("leaf_sprout")),
        LootUiItem("livre_magique", "Livre magique", "rare", 1, NestAssets.inventoryItemAsset("magic_book")),
        LootUiItem("charme_etoile", "Charme étoilé", "rare", 2, NestAssets.inventoryItemAsset("star_charm")),
        LootUiItem("cristal", "Cristal", "commun", 6, NestAssets.interfaceAsset("crystal")),
        LootUiItem("flammeches", "Flammèches", "commun", 20, NestAssets.interfaceAsset("flammeche")),
        LootUiItem("coffre_rare", "Coffre rare", "rare", 1, NestAssets.chestAsset("rare")),
    )

private val sampleEggs =
    listOf(
        EggUiItem("egg_pyron", "Œuf Pyron", "Chaleur douce", "Progression principale du Gardien", 0.72f, NestAssets.eggAsset("pyron", "glowing"), contentDescription = "Œuf Pyron lumineux"),
        EggUiItem("egg_fulmio", "Œuf Fulmio", "Énergie calme", "Activité physique et défis rapides", 0.34f, NestAssets.eggAsset("fulmio", "warm"), contentDescription = "Œuf Fulmio tiède"),
        EggUiItem("egg_sylvyn", "Œuf Sylvyn", "Racines paisibles", "Rangement, maison et aide familiale", 0.18f, NestAssets.eggAsset("sylvyn", "sleeping"), contentDescription = "Œuf Sylvyn endormi"),
        EggUiItem("egg_phenor", "Œuf Phenor", "Patience retrouvée", "Reprise après pause et persévérance", 0.48f, NestAssets.eggAsset("phenor", "glowing"), contentDescription = "Œuf Phenor lumineux"),
        EggUiItem("egg_lunarys", "Œuf Lunarys", "Veillée tranquille", "Routines du soir et calme", 0.82f, NestAssets.eggAsset("lunarys", "cracked"), contentDescription = "Œuf Lunarys fissuré"),
        EggUiItem("egg_chronyx", "Œuf Chronyx", "Temps régulier", "Séries, patience et habitudes", 0.27f, NestAssets.eggAsset("chronyx", "warm"), contentDescription = "Œuf Chronyx tiède"),
        EggUiItem("egg_ambrio", "Œuf Ambrio", "Cœur attentif", "Entraide et gentillesse", 0.10f, NestAssets.eggAsset("ambrio", "sleeping"), contentDescription = "Œuf Ambrio endormi"),
        EggUiItem("egg_cristao", "Œuf Cristao", "Concentration claire", "Lecture, devoirs et apprentissage", 1f, NestAssets.eggAsset("cristao", "hatching"), contentDescription = "Œuf Cristao en éclosion"),
    )

private val sampleDragons =
    listOf(
        DragonUiItem("dragon_pyron", "Pyron", "Bébé dragon braise", "Progression générale et motivation principale", NestAssets.dragonAsset("pyron", "baby"), contentDescription = "Dragon Pyron bébé"),
        DragonUiItem("dragon_fulmio", "Fulmio", "Jeune dragon tempête", "Activité physique et défis dynamiques", NestAssets.dragonAsset("fulmio", "young"), contentDescription = "Dragon Fulmio jeune"),
        DragonUiItem("dragon_sylvyn", "Sylvyn", "Dragon racine moyen", "Rangement, maison et aide familiale", NestAssets.dragonAsset("sylvyn", "medium"), contentDescription = "Dragon Sylvyn moyen"),
        DragonUiItem("dragon_phenor", "Phenor", "Dragon phénix jeune", "Reprise après pause, toujours sans punition", NestAssets.dragonAsset("phenor", "young"), contentDescription = "Dragon Phenor jeune"),
        DragonUiItem("dragon_lunarys", "Lunarys", "Grand dragon lunaire", "Routines du soir et sommeil", NestAssets.dragonAsset("lunarys", "large"), contentDescription = "Dragon Lunarys grand"),
        DragonUiItem("dragon_chronyx", "Chronyx", "Dragon chronos moyen", "Séries, régularité et patience", NestAssets.dragonAsset("chronyx", "medium"), contentDescription = "Dragon Chronyx moyen"),
        DragonUiItem("dragon_ambrio", "Ambrio", "Dragon cœur bébé", "Entraide, famille et comportement positif", NestAssets.dragonAsset("ambrio", "baby"), contentDescription = "Dragon Ambrio bébé"),
        DragonUiItem("dragon_cristao", "Cristao", "Dragon cristal légendaire", "Devoirs, lecture et concentration", NestAssets.dragonAsset("cristao", "legendary"), contentDescription = "Dragon Cristao légendaire"),
    )

private val sampleWishes =
    listOf(
        WishUiItem("Choisir le dessert", "Un Souhait du soir créé par un parent.", 10),
        WishUiItem("Cinéma maison", "Une séance familiale à planifier.", 20),
    )

private val sampleScrolls =
    listOf(
        ScrollUiItem("Choisir le dessert", "TASKO-12-AB34CD", "disponible", "approved"),
    )

private val sampleRequests =
    listOf(
        ParentRequestUiItem("Alex", "Choisir le dessert", "En attente"),
        ParentRequestUiItem("Alex", "Cinéma maison", "Refusé"),
    )
