package com.example.taskoday.features.gamification

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.taskoday.core.ui.component.fantasy.ChestCard
import com.example.taskoday.core.ui.component.fantasy.DragonCard
import com.example.taskoday.core.ui.component.fantasy.EggProgressCard
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
                assetResId = NestAssets.NestBackground.resId,
                assetDescription = "Le Nid",
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
                    assetResId = NestAssets.Flameche.resId,
                    tone = FantasyTone.Ember,
                    modifier = Modifier.weight(1f),
                )
                NestStatCard(
                    label = "Cristaux",
                    value = "6",
                    assetResId = NestAssets.Crystal.resId,
                    tone = FantasyTone.Violet,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            ChestCard(points = 4, pointsRequired = 5, unopenedChests = 1)
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
        assetResId = NestAssets.ChestCommon.resId,
        assetDescription = "Coffre",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleLoot.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Inventaire vide",
                    message = "Continue tes quêtes pour attirer de nouveaux trésors.",
                    assetResId = NestAssets.ChestCommon.resId,
                    assetDescription = "Coffre",
                )
            }
        } else {
            items(sampleLoot, key = { item -> item.key }) { item ->
                InventoryLootCard(
                    title = item.title,
                    rarity = item.rarityLabel,
                    quantity = item.quantity,
                    assetResId = item.assetResId,
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
        assetResId = NestAssets.EggSolarSleeping.resId,
        assetDescription = "Œuf lumineux",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleEggs.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun œuf découvert",
                    message = "Ton Nid attend sa prochaine étincelle.",
                    assetResId = NestAssets.EggSolarSleeping.resId,
                    assetDescription = "Œuf lumineux",
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
        assetResId = NestAssets.DragonEmberBaby.resId,
        assetDescription = "Bébé dragon braise",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleDragons.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun dragon débloqué",
                    message = "Ton dragon t'attend pour la prochaine aventure.",
                    assetResId = NestAssets.DragonEmberBaby.resId,
                    assetDescription = "Bébé dragon braise",
                )
            }
        } else {
            items(sampleDragons, key = { dragon -> dragon.key }) { dragon ->
                DragonCard(
                    title = dragon.title,
                    stage = dragon.stage,
                    nextStep = dragon.nextStep,
                    assetResId = dragon.assetResId,
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
        assetResId = NestAssets.Flameche.resId,
        assetDescription = "Flammèche",
        onOpenProfile = onOpenProfile,
    ) {
        item {
            NestStatCard(
                label = "Flammèches disponibles",
                value = "20",
                assetResId = NestAssets.Flameche.resId,
                tone = FantasyTone.Ember,
            )
        }
        if (sampleWishes.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun Souhait pour le moment",
                    message = "La Caverne aux Souhaits attend une nouvelle idée de parent.",
                    assetResId = NestAssets.Flameche.resId,
                    assetDescription = "Flammèche",
                )
            }
        } else {
            items(sampleWishes, key = { wish -> wish.title }) { wish ->
                WishCard(
                    title = wish.title,
                    description = wish.description,
                    costLabel = "${wish.cost} Flammèches",
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
        assetResId = NestAssets.ScrollApproved.resId,
        assetDescription = "Parchemin",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleScrolls.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun Parchemin pour le moment",
                    message = "Les Souhaits validés apparaîtront ici tranquillement.",
                    assetResId = NestAssets.ScrollApproved.resId,
                    assetDescription = "Parchemin",
                )
            }
        } else {
            items(sampleScrolls, key = { scroll -> scroll.code }) { scroll ->
                ScrollCard(
                    title = scroll.title,
                    code = scroll.code,
                    status = scroll.status,
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
        assetResId = NestAssets.ScrollApproved.resId,
        assetDescription = "Parchemin",
        onOpenProfile = onOpenProfile,
    ) {
        if (sampleRequests.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucune demande en attente",
                    message = "Les Souhaits du Gardien apparaîtront ici quand ils seront prêts.",
                    assetResId = NestAssets.ScrollApproved.resId,
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
)

data class DragonUiItem(
    val key: String,
    val title: String,
    val stage: String,
    val nextStep: String,
    val assetResId: Int,
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
)

data class ParentRequestUiItem(
    val childName: String,
    val title: String,
    val status: String,
)

private val sampleLoot =
    listOf(
        LootUiItem("pomme_dragon", "Pomme dragon", "commun", 8, NestAssets.Flameche.resId),
        LootUiItem("petit_cristal", "Petit cristal", "commun", 6, NestAssets.Crystal.resId),
        LootUiItem("pierre_chaude", "Pierre chaude", "commun", 1, NestAssets.ChestCommon.resId),
        LootUiItem("rune_ancienne", "Rune ancienne", "rare", 3, NestAssets.ScrollApproved.resId),
        LootUiItem("fragment_oeuf", "Fragment d'œuf", "rare", 1, NestAssets.EggSolarCracked.resId),
    )

private val sampleEggs =
    listOf(
        EggUiItem("oeuf_braise", "Œuf braise", "Prêt à couver", "3 pommes, 2 cristaux, 1 pierre chaude", 0.72f, NestAssets.EggSolarGlowing.resId),
        EggUiItem("oeuf_lunaire", "Œuf lunaire", "À trouver", "Disponible dans une future saison", 0.18f, NestAssets.EggSolarSleeping.resId),
        EggUiItem("oeuf_racine", "Œuf racine", "À trouver", "Disponible dans une future saison", 0.12f, NestAssets.EggSolarWarm.resId),
    )

private val sampleDragons =
    listOf(
        DragonUiItem("dragon_braise", "Bébé dragon braise", "Bébé", "Prochaine évolution : jeune dragon", NestAssets.DragonEmberBaby.resId),
    )

private val sampleWishes =
    listOf(
        WishUiItem("Choisir le dessert", "Un Souhait du soir créé par un parent.", 10),
        WishUiItem("Cinéma maison", "Une séance familiale à planifier.", 20),
    )

private val sampleScrolls =
    listOf(
        ScrollUiItem("Choisir le dessert", "TASKO-12-AB34CD", "Parchemin disponible"),
    )

private val sampleRequests =
    listOf(
        ParentRequestUiItem("Alex", "Choisir le dessert", "En attente"),
        ParentRequestUiItem("Alex", "Cinéma maison", "Refusé"),
    )
