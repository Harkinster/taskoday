package com.example.taskoday.features.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
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
            TaskodayHeader(
                title = "Le Nid",
                subtitle = "Ton Gardien grandit avec les routines, missions et quetes.",
                avatarInitials = "AB",
                onAvatarClick = onOpenProfile,
            )
        }
        item {
            GuardianProgressCard(
                xp = 50,
                levelName = "Vieux Nid",
                nextLevelLabel = "Nid reveille",
                progress = 0.50f,
            )
        }
        item {
            ChestProgressCard(points = 4, pointsRequired = 5, unopenedChests = 1)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NeonButton(
                    text = "Journee",
                    onClick = onOpenPlanning,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = "Inventaire",
                    onClick = onOpenInventory,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NeonButton(text = "Oeufs", onClick = onOpenEggs, modifier = Modifier.weight(1f))
                NeonButton(text = "Dragons", onClick = onOpenDragons, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NeonButton(
                    text = "Souhaits",
                    onClick = onOpenWishes,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = "Parchemins",
                    onClick = onOpenScrolls,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
            }
        }
    }
}

@Composable
fun InventoryScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Inventaire",
        subtitle = "Objets trouves dans les coffres.",
        onOpenProfile = onOpenProfile,
    ) {
        items(sampleLoot, key = { item -> item.key }) { item ->
            LootItemCard(item = item)
        }
    }
}

@Composable
fun EggsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Oeufs",
        subtitle = "Les oeufs attendent les bons objets pour eclore.",
        onOpenProfile = onOpenProfile,
    ) {
        items(sampleEggs, key = { egg -> egg.key }) { egg ->
            EggCard(egg = egg)
        }
    }
}

@Composable
fun DragonsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Dragons",
        subtitle = "Les compagnons debloques dans le Nid.",
        onOpenProfile = onOpenProfile,
    ) {
        items(sampleDragons, key = { dragon -> dragon.key }) { dragon ->
            DragonCard(dragon = dragon)
        }
    }
}

@Composable
fun WishesCaveScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Caverne aux Souhaits",
        subtitle = "Les Flammèches servent uniquement aux Souhaits.",
        onOpenProfile = onOpenProfile,
    ) {
        item {
            FlammecheBadge(balance = 20)
        }
        items(sampleWishes, key = { wish -> wish.title }) { wish ->
            RewardWishCard(wish = wish)
        }
    }
}

@Composable
fun ScrollsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Parchemins",
        subtitle = "Les Souhaits validés deviennent des Parchemins.",
        onOpenProfile = onOpenProfile,
    ) {
        items(sampleScrolls, key = { scroll -> scroll.code }) { scroll ->
            ScrollCard(scroll = scroll)
        }
    }
}

@Composable
fun ParentRewardsScreen(
    onOpenProfile: () -> Unit,
) {
    GamificationListScreen(
        title = "Souhaits parent",
        subtitle = "Valider, refuser ou suivre les demandes.",
        onOpenProfile = onOpenProfile,
    ) {
        items(sampleRequests, key = { request -> request.childName + request.title }) { request ->
            ParentRequestCard(request = request)
        }
    }
}

@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    tone: NeonTone = NeonTone.Cyan,
    content: @Composable () -> Unit,
) {
    NeonCard(modifier = modifier, tone = tone) {
        content()
    }
}

@Composable
fun GuardianProgressCard(
    xp: Int,
    levelName: String,
    nextLevelLabel: String,
    progress: Float,
) {
    MagicCard(tone = NeonTone.Cyan) {
        Text(text = "Gardien", style = MaterialTheme.typography.titleLarge, color = StarWhite)
        Text(text = "$xp XP du Gardien", style = MaterialTheme.typography.headlineSmall, color = StarWhite)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = NeonCyan,
            trackColor = Color.White.copy(alpha = 0.16f),
        )
        Text(text = "$levelName -> $nextLevelLabel", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
fun FlammecheBadge(balance: Int) {
    MagicCard(tone = NeonTone.Warning) {
        Text(text = "Flammèches", style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = "$balance disponibles", style = MaterialTheme.typography.headlineSmall, color = StarWhite)
    }
}

@Composable
fun ChestProgressCard(
    points: Int,
    pointsRequired: Int,
    unopenedChests: Int,
) {
    MagicCard(tone = NeonTone.Violet) {
        Text(text = "Coffres", style = MaterialTheme.typography.titleLarge, color = StarWhite)
        Text(text = "$points/$pointsRequired points coffre", style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = "$unopenedChests coffre en attente", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
fun LootItemCard(item: LootUiItem) {
    MagicCard(tone = if (item.rarity == "rare") NeonTone.Violet else NeonTone.Blue) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
                Text(text = item.rarity, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(text = "x${item.quantity}", style = MaterialTheme.typography.titleLarge, color = StarWhite)
        }
    }
}

@Composable
fun EggCard(egg: EggUiItem) {
    MagicCard(tone = NeonTone.Warning) {
        Text(text = egg.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = egg.status, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(text = egg.requirements, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
fun DragonCard(dragon: DragonUiItem) {
    MagicCard(tone = NeonTone.Success) {
        Text(text = dragon.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = dragon.stage, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(text = dragon.nextStep, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
fun RewardWishCard(wish: WishUiItem) {
    MagicCard(tone = NeonTone.Blue) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = wish.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
                Text(text = wish.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            Text(text = "${wish.cost} Flammèches", style = MaterialTheme.typography.titleMedium, color = StarWhite)
        }
    }
}

@Composable
fun ScrollCard(scroll: ScrollUiItem) {
    MagicCard(tone = NeonTone.Cyan) {
        Text(text = scroll.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = scroll.code, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(text = scroll.status, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
fun ParentRequestCard(request: ParentRequestUiItem) {
    MagicCard(tone = NeonTone.Warning) {
        Text(text = request.title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
        Text(text = request.childName, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(text = request.status, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun GamificationScaffold(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
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
    onOpenProfile: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    GamificationScaffold {
        item {
            TaskodayHeader(
                title = title,
                subtitle = subtitle,
                avatarInitials = "AB",
                onAvatarClick = onOpenProfile,
            )
        }
        content()
    }
}

data class LootUiItem(
    val key: String,
    val title: String,
    val rarity: String,
    val quantity: Int,
)

data class EggUiItem(
    val key: String,
    val title: String,
    val status: String,
    val requirements: String,
)

data class DragonUiItem(
    val key: String,
    val title: String,
    val stage: String,
    val nextStep: String,
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
        LootUiItem("pomme_dragon", "Pomme dragon", "common", 8),
        LootUiItem("petit_cristal", "Petit cristal", "common", 6),
        LootUiItem("pierre_chaude", "Pierre chaude", "common", 1),
        LootUiItem("rune_ancienne", "Rune ancienne", "rare", 3),
        LootUiItem("fragment_oeuf", "Fragment d'oeuf", "rare", 1),
    )

private val sampleEggs =
    listOf(
        EggUiItem("oeuf_braise", "Oeuf braise", "Disponible", "3 pommes, 2 cristaux, 1 pierre chaude"),
        EggUiItem("oeuf_lunaire", "Oeuf lunaire", "A trouver", "Disponible dans une future saison"),
        EggUiItem("oeuf_racine", "Oeuf racine", "A trouver", "Disponible dans une future saison"),
    )

private val sampleDragons =
    listOf(
        DragonUiItem("dragon_braise", "Dragon braise", "Bebe", "Prochaine evolution : jeune"),
    )

private val sampleWishes =
    listOf(
        WishUiItem("Choisir le dessert", "Bonus du soir cree par un parent.", 10),
        WishUiItem("Cinema maison", "Une seance familiale a planifier.", 20),
    )

private val sampleScrolls =
    listOf(
        ScrollUiItem("Choisir le dessert", "TASKO-12-AB34CD", "available"),
    )

private val sampleRequests =
    listOf(
        ParentRequestUiItem("Alex", "Choisir le dessert", "pending"),
        ParentRequestUiItem("Alex", "Cinema maison", "refused"),
    )
