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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var activeCompanionKey by rememberSaveable { mutableStateOf("dragon_pyron") }
    var followedEggKey by rememberSaveable { mutableStateOf("egg_pyron") }
    val activeDragon = sampleDragons.firstOrNull { dragon -> dragon.key == activeCompanionKey }
    val followedEgg = sampleEggs.firstOrNull { egg -> egg.key == followedEggKey } ?: sampleEggs.first()

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
            WishAccessCard(
                onOpenWishes = onOpenWishes,
                onOpenScrolls = onOpenScrolls,
            )
        }
        item {
            ActiveNestDisplayCard(
                dragon = activeDragon,
                egg = followedEgg,
                perchLevel = 1,
            )
        }
        item {
            DragonCard(
                title = activeDragon?.title ?: followedEgg.title,
                stage = activeDragon?.stage ?: "Œuf suivi",
                nextStep = activeDragon?.nextStep ?: "Choisis un œuf à suivre dans le Bestiaire.",
                assetResId = activeDragon?.assetResId ?: followedEgg.assetResId,
                contentDescription = activeDragon?.contentDescription ?: followedEgg.contentDescription,
                badgeLabel = if (activeDragon != null) "Compagnon actif" else "Œuf suivi",
                primaryActionLabel = "Faire évoluer",
                onPrimaryAction = {},
                secondaryActionLabel = "Bestiaire du Nid",
                onSecondaryAction = { onOpenDragons() },
            )
        }
        item {
            EggProgressCard(
                title = "Œuf Pyron",
                status = "Chaleur douce",
                requirements = "Ajoute des pousses, potions ou fragments pour avancer à ton rythme.",
                progress = 0.72f,
                assetResId = NestAssets.eggAsset("pyron", "glowing"),
                contentDescription = "Œuf Pyron lumineux",
                materialLabel = "Matériaux libres : 72%",
                actionLabel = "Améliorer l'Œuf",
                onAction = {},
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
            ChestCard(
                points = 0,
                pointsRequired = 0,
                unopenedChests = 1,
                title = "Coffres en Cristaux",
                costLabel = "Commun 5 Cristaux, rare 15, épique 40.",
                actionLabel = "Ouvrir un coffre",
                onAction = {},
                assetResId = NestAssets.chestAsset("rare"),
                contentDescription = "Coffre rare",
                rarity = "rare",
            )
        }
        item {
            NavigationButtons(
                primaryLabel = "Routine",
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
                primaryLabel = "Dragons",
                onPrimaryClick = onOpenDragons,
                secondaryLabel = "Parchemins",
                onSecondaryClick = onOpenScrolls,
                outline = true,
            )
        }
        item {
            BestiaryPreviewCard(
                activeCompanionKey = activeCompanionKey,
                followedEggKey = followedEggKey,
                onSelectDragon = { key -> activeCompanionKey = key },
                onSelectEgg = { key ->
                    followedEggKey = key
                    activeCompanionKey = ""
                },
                onOpenDragons = onOpenDragons,
                onOpenEggs = onOpenEggs,
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
                    usageLabel = item.usageLabel,
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
                    materialLabel = egg.materialLabel,
                    actionLabel = egg.actionLabel,
                    onAction = {},
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
        subtitle = "Les compagnons du Gardien débloqués dans Le Nid.",
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
                    badgeLabel = if (dragon.active) "Compagnon actif" else null,
                    primaryActionLabel = if (dragon.active) "Faire évoluer" else "Définir comme compagnon",
                    onPrimaryAction = {},
                    secondaryActionLabel = "Voir l'artefact",
                    onSecondaryAction = {},
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
private fun WishAccessCard(
    onOpenWishes: () -> Unit,
    onOpenScrolls: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Ember) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(
                assetResId = NestAssets.interfaceAsset("flammeche"),
                contentDescription = "Flammèches",
                size = 52.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Flammèches : 20", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                Text(
                    text = "Les Souhaits et Parchemins restent dans Le Nid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
        NavigationButtons(
            primaryLabel = "Caverne aux Souhaits",
            onPrimaryClick = onOpenWishes,
            secondaryLabel = "Mes Parchemins",
            onSecondaryClick = onOpenScrolls,
            outline = true,
        )
    }
}

@Composable
private fun ActiveNestDisplayCard(
    dragon: DragonUiItem?,
    egg: EggUiItem,
    perchLevel: Int,
) {
    FantasyCard(tone = FantasyTone.Gold) {
        Text(text = "Affichage du Nid", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(
                assetResId = NestAssets.perchAsset(perchLevel),
                contentDescription = "Perchoir niveau $perchLevel",
                size = 72.dp,
            )
            FantasyAssetBubble(
                assetResId = dragon?.assetResId ?: egg.assetResId,
                contentDescription = dragon?.contentDescription ?: egg.contentDescription,
                size = 82.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dragon?.title ?: egg.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                )
                Text(
                    text = dragon?.let { "Compagnon actif" } ?: "Œuf suivi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MossGreen,
                )
                Text(
                    text = "Le perchoir soutient le compagnon ou l'œuf suivi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun BestiaryPreviewCard(
    activeCompanionKey: String,
    followedEggKey: String,
    onSelectDragon: (String) -> Unit,
    onSelectEgg: (String) -> Unit,
    onOpenDragons: () -> Unit,
    onOpenEggs: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Violet) {
        Text(text = "Bestiaire du Nid", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
        Text(
            text = "Choisis le compagnon affiché dans Le Nid ou l'œuf à suivre.",
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
        )
        sampleDragons.take(2).forEach { dragon ->
            BestiaryChoiceRow(
                title = dragon.title,
                subtitle = if (dragon.key == activeCompanionKey) "Compagnon actif" else "Dragon débloqué",
                assetResId = dragon.assetResId,
                contentDescription = dragon.contentDescription,
                actionLabel = if (dragon.key == activeCompanionKey) "Actif" else "Définir comme compagnon",
                enabled = dragon.key != activeCompanionKey,
                onClick = { onSelectDragon(dragon.key) },
            )
        }
        sampleEggs.take(2).forEach { egg ->
            BestiaryChoiceRow(
                title = egg.title,
                subtitle = if (egg.key == followedEggKey && activeCompanionKey.isBlank()) "Œuf suivi" else "Œuf découvert",
                assetResId = egg.assetResId,
                contentDescription = egg.contentDescription,
                actionLabel = "Suivre cet Œuf",
                enabled = true,
                onClick = { onSelectEgg(egg.key) },
            )
        }
        BestiaryChoiceRow(
            title = "Dragon non découvert",
            subtitle = "Fais éclore un Œuf pour rencontrer ton premier dragon.",
            assetResId = NestAssets.interfaceAsset("egg_locked"),
            contentDescription = "Œuf verrouillé",
            actionLabel = "Verrouillé",
            enabled = false,
            onClick = {},
        )
        NavigationButtons(
            primaryLabel = "Voir les Dragons",
            onPrimaryClick = onOpenDragons,
            secondaryLabel = "Voir les Œufs",
            onSecondaryClick = onOpenEggs,
            outline = true,
        )
    }
}

@Composable
private fun BestiaryChoiceRow(
    title: String,
    subtitle: String,
    assetResId: Int,
    contentDescription: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = WoodBrownDark)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        FantasyButton(
            text = actionLabel,
            onClick = onClick,
            style = if (enabled) FantasyButtonStyle.Outline else FantasyButtonStyle.Quiet,
            enabled = enabled,
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
    val usageLabel: String,
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
    val materialLabel: String? = null,
    val actionLabel: String? = "Améliorer l'Œuf",
)

data class DragonUiItem(
    val key: String,
    val title: String,
    val stage: String,
    val nextStep: String,
    val assetResId: Int,
    val contentDescription: String = title,
    val active: Boolean = false,
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
        LootUiItem("crystal", "Cristal", "ressource", 6, NestAssets.interfaceAsset("crystal"), "Ouvrir des Coffres"),
        LootUiItem("wood_logs", "Rondins de bois", "commun", 3, NestAssets.inventoryItemAsset("wood_logs"), "Perchoir"),
        LootUiItem("leaf_sprout", "Pousse de feuille", "commun", 4, NestAssets.inventoryItemAsset("leaf_sprout"), "Œuf ou Perchoir"),
        LootUiItem("mushroom", "Champignon", "commun", 2, NestAssets.inventoryItemAsset("mushroom"), "Œuf"),
        LootUiItem("potion", "Potion douce", "peu commun", 1, NestAssets.inventoryItemAsset("potion"), "Dragon ou Œuf"),
        LootUiItem("lantern", "Lanterne", "peu commun", 1, NestAssets.inventoryItemAsset("lantern"), "Perchoir"),
        LootUiItem("magic_book", "Livre magique", "rare", 1, NestAssets.inventoryItemAsset("magic_book"), "Dragon"),
        LootUiItem("star_charm", "Charme étoile", "épique", 1, NestAssets.inventoryItemAsset("star_charm"), "Artefact légendaire"),
    )

private val sampleEggs =
    listOf(
        EggUiItem("egg_pyron", "Œuf Pyron", "Chaleur douce", "Pousses, potions ou fragments au choix", 0.72f, NestAssets.eggAsset("pyron", "glowing"), contentDescription = "Œuf Pyron lumineux", materialLabel = "72% de matériaux"),
        EggUiItem("egg_fulmio", "Œuf Fulmio", "Souffle tranquille", "Matériaux libres, aucune tâche imposée", 0.34f, NestAssets.eggAsset("fulmio", "warm"), contentDescription = "Œuf Fulmio tiède", materialLabel = "34% de matériaux"),
        EggUiItem("egg_sylvyn", "Œuf Sylvyn", "Racines paisibles", "Rondins et pousses peuvent l'aider", 0.18f, NestAssets.eggAsset("sylvyn", "sleeping"), contentDescription = "Œuf Sylvyn endormi", materialLabel = "18% de matériaux"),
        EggUiItem("egg_phenor", "Œuf Phenor", "Patience retrouvée", "Un retour après pause peut aussi apporter du loot", 0.48f, NestAssets.eggAsset("phenor", "glowing"), contentDescription = "Œuf Phenor lumineux", materialLabel = "48% de matériaux"),
        EggUiItem("egg_lunarys", "Œuf Lunarys", "Veillée tranquille", "Choisis les objets que tu veux investir", 0.82f, NestAssets.eggAsset("lunarys", "cracked"), contentDescription = "Œuf Lunarys fissuré", materialLabel = "82% de matériaux"),
        EggUiItem("egg_chronyx", "Œuf Chronyx", "Temps régulier", "Les consommables guident sa progression", 0.27f, NestAssets.eggAsset("chronyx", "warm"), contentDescription = "Œuf Chronyx tiède", materialLabel = "27% de matériaux"),
        EggUiItem("egg_ambrio", "Œuf Ambrio", "Cœur attentif", "Les doublons deviendront des Cristaux ou fragments", 0.10f, NestAssets.eggAsset("ambrio", "sleeping"), contentDescription = "Œuf Ambrio endormi", materialLabel = "10% de matériaux"),
        EggUiItem("egg_cristao", "Œuf Cristao", "Concentration claire", "Prêt à éclore avec les bons objets", 1f, NestAssets.eggAsset("cristao", "hatching"), contentDescription = "Œuf Cristao en éclosion", materialLabel = "Prêt à éclore", actionLabel = "Faire éclore"),
    )

private val sampleDragons =
    listOf(
        DragonUiItem("dragon_pyron", "Pyron", "Bébé dragon braise", "Consommables choisis pour la prochaine évolution", NestAssets.dragonAsset("pyron", "baby"), contentDescription = "Dragon Pyron bébé", active = true),
        DragonUiItem("dragon_fulmio", "Fulmio", "Jeune dragon tempête", "Peut devenir compagnon quand le Gardien le souhaite", NestAssets.dragonAsset("fulmio", "young"), contentDescription = "Dragon Fulmio jeune"),
        DragonUiItem("dragon_sylvyn", "Sylvyn", "Dragon racine moyen", "Progression libre par objets et Cristaux", NestAssets.dragonAsset("sylvyn", "medium"), contentDescription = "Dragon Sylvyn moyen"),
        DragonUiItem("dragon_phenor", "Phenor", "Dragon phénix jeune", "Reprise douce, toujours sans punition", NestAssets.dragonAsset("phenor", "young"), contentDescription = "Dragon Phenor jeune"),
        DragonUiItem("dragon_lunarys", "Lunarys", "Grand dragon lunaire", "Le lore reste doux sans verrou de tâche", NestAssets.dragonAsset("lunarys", "large"), contentDescription = "Dragon Lunarys grand"),
        DragonUiItem("dragon_chronyx", "Chronyx", "Dragon chronos moyen", "Régularité et patience en inspiration", NestAssets.dragonAsset("chronyx", "medium"), contentDescription = "Dragon Chronyx moyen"),
        DragonUiItem("dragon_ambrio", "Ambrio", "Dragon cœur bébé", "Entraide et famille en inspiration", NestAssets.dragonAsset("ambrio", "baby"), contentDescription = "Dragon Ambrio bébé"),
        DragonUiItem("dragon_cristao", "Cristao", "Dragon cristal légendaire", "Artefact légendaire préparé", NestAssets.dragonAsset("cristao", "legendary"), contentDescription = "Dragon Cristao légendaire"),
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
