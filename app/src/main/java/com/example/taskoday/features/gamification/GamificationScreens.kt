package com.example.taskoday.features.gamification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.example.taskoday.core.ui.component.fantasy.EggProgressCard
import com.example.taskoday.core.ui.component.fantasy.ChestCard
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyBadge
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyCompactButton
import com.example.taskoday.core.ui.component.fantasy.FantasyHeader
import com.example.taskoday.core.ui.component.fantasy.FantasyProgressBar
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.InventoryLootCard
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.ScrollCard
import com.example.taskoday.core.ui.format.toTaskodayDisplayLabel
import com.example.taskoday.core.ui.theme.CrystalBlue
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.data.remote.dto.ChestDto
import com.example.taskoday.data.remote.dto.BestiaryFamilyDto
import com.example.taskoday.data.remote.dto.DragonDto
import com.example.taskoday.data.remote.dto.EggDto
import com.example.taskoday.data.remote.dto.InventoryDto
import com.example.taskoday.data.remote.dto.InventoryItemDto
import com.example.taskoday.data.remote.dto.RequiredResourceDto

data class RecentNestReward(
    val actionTitle: String,
    val xp: Int = 0,
    val flammeches: Int = 0,
    val crystals: Int = 0,
)

internal fun shouldShowRecentNestReward(reward: RecentNestReward?): Boolean =
    reward != null &&
        reward.actionTitle.isNotBlank() &&
        (reward.xp > 0 || reward.flammeches > 0 || reward.crystals > 0)

@Composable
fun NestScreen(
    viewModel: NestViewModel,
    recentReward: RecentNestReward? = null,
    recentRewardEventId: Long = 0L,
    onRecentRewardConsumed: () -> Unit = {},
    onOpenInventory: () -> Unit,
    onOpenDragons: () -> Unit,
    onOpenEggs: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenChests: () -> Unit,
    onOpenScrolls: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var visibleRecentReward by remember { mutableStateOf<RecentNestReward?>(null) }
    val nestEggs =
        if (uiState.hasRemoteSession) {
            uiState.eggs?.eggs.orEmpty().map { egg -> egg.toUiItem(uiState.inventory) }
        } else {
            emptyList()
        }
    val activeDragon =
        if (uiState.hasRemoteSession) {
            uiState.dragons?.activeCompanion?.toUiItem()
        } else {
            null
        }
    val activeNestEgg = selectActiveNestEgg(nestEggs)
    LaunchedEffect(recentRewardEventId) {
        if (recentReward == null) return@LaunchedEffect
        if (shouldShowRecentNestReward(recentReward)) {
            visibleRecentReward = recentReward
        }
        onRecentRewardConsumed()
    }

    GamificationScaffold(backgroundResId = com.example.taskoday.R.drawable.chronodria_nest_environment_v1) {
        item { NestIdentityHeader(onOpenProfile = onOpenProfile) }
        uiState.userMessage?.let { message ->
            item { FantasyStateCard(title = "Information du Nid", message = message, assetResId = NestAssets.interfaceAsset("nid")) }
        }
        item { NestCreatureStage(dragon = activeDragon, egg = activeNestEgg, onOpenEggs = onOpenEggs) }
        item {
            NestHubTiles(
                onOpenInventory = onOpenInventory,
                onOpenEggs = onOpenEggs,
                onOpenChests = onOpenChests,
                onOpenWishes = onOpenWishes,
            )
        }
        item {
            NestResourcesPanel(
                hasRemoteSession = uiState.hasRemoteSession,
                isLoading = uiState.isLoading,
                points = uiState.personalRewards?.taskodayPoints,
                flames = uiState.personalRewards?.flames,
                crystals = uiState.personalRewards?.crystals,
                onOpenWishes = onOpenWishes,
                onOpenChests = onOpenChests,
            )
        }
        visibleRecentReward?.let { reward -> item { RecentNestRewardCard(reward = reward, onDismiss = { visibleRecentReward = null }) } }
        if (activeNestEgg != null || activeDragon != null) {
            item {
                NestActiveEggCard(
                    egg = activeNestEgg,
                    onOpenEggs = onOpenEggs,
                    onEvolveEgg = { activeNestEgg?.id?.let(viewModel::evolveEgg) },
                )
            }
        }
    }
    uiState.hatchingCelebration?.let { celebration ->
        HatchingCelebrationDialog(
            celebration = celebration,
            onDiscoverDragon = {
                viewModel.consumeHatchingCelebration()
                onOpenDragons()
            },
            onDismiss = viewModel::consumeHatchingCelebration,
        )
    }
}

@Composable
fun InventoryScreen(
    viewModel: NestViewModel,
    onOpenProfile: () -> Unit,
    onBackToNest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inventoryItems =
        if (uiState.hasRemoteSession) {
            buildList {
                uiState.inventory?.items?.mapTo(this) { it.toUiItem() }
                uiState.inventory?.chests?.mapTo(this) { it.toUiItem() }
            }
        } else {
            sampleLoot
        }
    GamificationListScreen(
        title = "Inventaire",
        subtitle = "Les petits trésors trouvés dans les coffres du Gardien.",
        assetResId = NestAssets.interfaceAsset("inventory_empty"),
        assetDescription = "Inventaire",
        onOpenProfile = onOpenProfile,
        onBackToNest = onBackToNest,
        message = uiState.userMessage,
    ) {
        if (inventoryItems.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Inventaire vide",
                    message = "Les objets trouvés dans les coffres apparaîtront ici.",
                    assetResId = NestAssets.interfaceAsset("inventory_empty"),
                    assetDescription = "Inventaire vide",
                )
            }
        } else {
            items(inventoryItems, key = { item -> item.key }) { item ->
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
    viewModel: NestViewModel,
    onOpenProfile: () -> Unit,
    onBackToNest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eggs =
        if (uiState.hasRemoteSession) {
            uiState.eggs?.eggs.orEmpty().map { it.toUiItem(uiState.inventory) }
        } else {
            sampleEggs
        }
    GamificationListScreen(
        title = "Œufs",
        subtitle = "Chaque œuf attend les bons objets pour éclore doucement.",
        assetResId = NestAssets.eggAsset("pyron", "sleeping"),
        assetDescription = "Œuf Pyron endormi",
        onOpenProfile = onOpenProfile,
        onBackToNest = onBackToNest,
        message = uiState.userMessage,
    ) {
        if (eggs.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun œuf pour le moment",
                    message = "Les œufs découverts apparaîtront ici, tranquillement.",
                    assetResId = NestAssets.interfaceAsset("egg_locked"),
                    assetDescription = "Œuf inconnu verrouillé",
                )
            }
        } else {
            items(eggs, key = { egg -> egg.key }) { egg ->
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
                    actionEnabled = egg.actionEnabled,
                    onAction = { egg.id?.let(viewModel::evolveEgg) },
                )
            }
        }
    }
}

@Composable
fun DragonsScreen(
    viewModel: NestViewModel,
    onOpenProfile: () -> Unit,
    onBackToNest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dragons =
        if (uiState.hasRemoteSession) {
            uiState.bestiary?.families.orEmpty().map { family ->
                family.toDragonUiItem(uiState.dragons?.dragons.orEmpty().firstOrNull { it.dragonKey == "dragon_${family.familyId}" })
            }
        } else {
            sampleDragons
        }
    val eggs =
        if (uiState.hasRemoteSession) {
            uiState.bestiary?.families.orEmpty().map { family ->
                family.toEggUiItem(
                    egg = uiState.eggs?.eggs.orEmpty().firstOrNull { it.eggKey == "oeuf_${family.familyId}" },
                    inventory = uiState.inventory,
                )
            }
        } else {
            sampleEggs
        }
    GamificationListScreen(
        title = "Bestiaire",
        subtitle = "Chaque famille rassemble son œuf et ses évolutions de dragon.",
        assetResId = NestAssets.dragonAsset("pyron", "baby"),
        assetDescription = "Dragon Pyron bébé",
        onOpenProfile = onOpenProfile,
        onBackToNest = onBackToNest,
        message = uiState.userMessage,
    ) {
        if (dragons.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun dragon débloqué",
                    message = "Ton dragon t'attend pour la prochaine aventure.",
                    assetResId = NestAssets.dragonAsset("pyron", "baby"),
                    assetDescription = "Dragon Pyron bébé",
                )
            }
        } else {
            items(dragons, key = { dragon -> dragon.key }) { dragon ->
                val familyKey = dragon.key.removePrefix("dragon_")
                val egg = eggs.firstOrNull { item -> item.familyKey == familyKey }
                FamilyBestiaryCard(
                    dragon = dragon,
                    egg = egg,
                    onActivate = { dragon.id?.let(viewModel::activateDragon) },
                    onEvolveEgg = { egg?.id?.let(viewModel::evolveEgg) },
                    onEvolveDragon = { dragon.id?.let(viewModel::evolveDragon) },
                )
            }
        }
    }
}

@Composable
fun ScrollsScreen(
    viewModel: NestViewModel,
    onOpenProfile: () -> Unit,
    onBackToNest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrolls =
        if (uiState.hasRemoteSession) {
            uiState.scrolls?.scrolls.orEmpty().map {
                ScrollUiItem(
                    title = "Parchemin de Souhait",
                    code = it.code,
                    status = it.status,
                    statusKey = it.status,
                )
            }
        } else {
            sampleScrolls
        }
    GamificationListScreen(
        title = "Parchemins",
        subtitle = "Les Souhaits validés deviennent des Parchemins à utiliser en famille.",
        assetResId = NestAssets.scrollAsset("approved"),
        assetDescription = "Parchemin",
        onOpenProfile = onOpenProfile,
        onBackToNest = onBackToNest,
        message = uiState.userMessage,
    ) {
        if (scrolls.isEmpty()) {
            item {
                FantasyStateCard(
                    title = "Aucun Parchemin pour le moment",
                    message = "Les Souhaits validés apparaîtront ici tranquillement.",
                    assetResId = NestAssets.scrollAsset("pending"),
                    assetDescription = "Parchemin",
                )
            }
        } else {
            items(scrolls, key = { scroll -> scroll.code }) { scroll ->
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
private fun GuardianProgressCard(
    xp: Int,
    levelName: String,
    nextLevelLabel: String,
    progress: Float,
) {
    FantasyCard(tone = FantasyTone.Gold) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(
                assetResId = NestAssets.interfaceAsset("nid"),
                contentDescription = null,
                size = 56.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "Gardien", style = MaterialTheme.typography.titleLarge, color = WoodBrownDark)
                Text(text = "$xp XP du Gardien", style = MaterialTheme.typography.headlineSmall, color = WoodBrownDark)
            }
        }
        FantasyProgressBar(progress = progress)
        Text(
            text = "$levelName vers $nextLevelLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
    }
}

@Composable
private fun RecentNestRewardCard(
    reward: RecentNestReward,
    onDismiss: () -> Unit,
) {
    val rows = recentNestRewardRows(reward)
    FantasyCard(tone = FantasyTone.Gold, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(
                assetResId = NestAssets.interfaceAsset("flammeche"),
                contentDescription = null,
                size = 50.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Le Nid brille un peu plus !",
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Grâce à « ${reward.actionTitle} », tes gains viennent d'arriver ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                RecentNestRewardRow(row)
            }
        }
        FantasyProgressBar(progress = 1f, height = 6.dp)
        FantasyButton(
            text = "Continue tes aventures",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Quiet,
        )
    }
}

@Composable
private fun RecentNestRewardRow(row: RecentNestRewardDisplayRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FantasyAssetBubble(
            assetResId = row.assetResId,
            contentDescription = row.label,
            size = 32.dp,
        )
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.value,
            style = MaterialTheme.typography.titleSmall,
            color = row.color,
            maxLines = 1,
        )
    }
}

@Composable
private fun HatchingCelebrationDialog(
    celebration: NestHatchingCelebration,
    onDiscoverDragon: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dragonTitle = celebration.dragonTitle?.takeIf { it.isNotBlank() }
    val familyLabel =
        celebration.dragonKey
            ?.removePrefix("dragon_")
            ?.takeIf { it.isNotBlank() }
            ?.toTaskodayDisplayLabel()
    val stageLabel = celebration.dragonStage?.toFantasyStateLabel()
    Dialog(onDismissRequest = onDismiss) {
        FantasyCard(tone = FantasyTone.Gold, contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FantasyAssetBubble(
                    assetResId = hatchingCelebrationAsset(celebration),
                    contentDescription = dragonTitle ?: "Dragon découvert",
                    size = 78.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    FantasyBadge(text = "Éclosion", tone = FantasyTone.Moss)
                    Text(
                        text = "Un dragon s'éveille !",
                        style = MaterialTheme.typography.titleLarge,
                        color = WoodBrownDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            dragonTitle
                                ?: "Ton œuf vient d'éclore.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MagicViolet,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "Une nouvelle créature rejoint ton aventure dans Chronodria.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            if (celebration.hasDragonDetails) {
                val details =
                    listOfNotNull(
                        familyLabel?.let { "Famille $it" },
                        stageLabel,
                    ).joinToString(" • ")
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MossGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Tu peux maintenant le choisir comme compagnon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
            FantasyProgressBar(progress = 1f, height = 6.dp)
            FantasyButton(
                text = "Découvrir mon dragon",
                onClick = onDiscoverDragon,
                modifier = Modifier.fillMaxWidth(),
            )
            FantasyButton(
                text = "Plus tard",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FantasyButtonStyle.Quiet,
            )
        }
    }
}

private fun hatchingCelebrationAsset(celebration: NestHatchingCelebration): Int {
    val dragonKey = celebration.dragonKey ?: return NestAssets.interfaceAsset("nid")
    val stage = celebration.dragonStage ?: return NestAssets.interfaceAsset("nid")
    return NestAssets.dragonAsset(dragonKey.removePrefix("dragon_").toVisualFamily(), stage)
}

private data class RecentNestRewardDisplayRow(
    val label: String,
    val value: String,
    val assetResId: Int,
    val color: Color,
)

private fun recentNestRewardRows(reward: RecentNestReward): List<RecentNestRewardDisplayRow> =
    buildList {
        if (reward.xp > 0) {
            add(
                RecentNestRewardDisplayRow(
                    label = "XP du Gardien",
                    value = "+${reward.xp} XP",
                    assetResId = NestAssets.interfaceAsset("nid"),
                    color = MossGreen,
                ),
            )
        }
        if (reward.flammeches > 0) {
            add(
                RecentNestRewardDisplayRow(
                    label = "Flammèches",
                    value = "+${reward.flammeches}",
                    assetResId = NestAssets.interfaceAsset("flammeche"),
                    color = EmberOrange,
                ),
            )
        }
        if (reward.crystals > 0) {
            add(
                RecentNestRewardDisplayRow(
                    label = "Cristaux",
                    value = "+${reward.crystals}",
                    assetResId = NestAssets.interfaceAsset("crystal"),
                    color = CrystalBlue,
                ),
            )
        }
    }

@Composable
private fun NestCurrencyBar(
    flammeches: Int,
    crystals: Int,
    onOpenWishes: () -> Unit,
    onOpenChests: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Night, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            CurrencyPill(
                label = "Flammèches",
                value = flammeches.toString(),
                assetResId = NestAssets.interfaceAsset("flammeche"),
                tone = FantasyTone.Ember,
                modifier = Modifier.weight(1f),
                onClick = onOpenWishes,
            )
            CurrencyPill(
                label = "Cristaux",
                value = crystals.toString(),
                assetResId = NestAssets.interfaceAsset("crystal"),
                tone = FantasyTone.Violet,
                modifier = Modifier.weight(1f),
                onClick = onOpenChests,
            )
        }
    }
}

@Composable
private fun CurrencyPill(
    label: String,
    value: String,
    assetResId: Int,
    tone: FantasyTone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(ParchmentLight.copy(alpha = 0.92f), tone.soft.copy(alpha = 0.72f))))
                .border(1.dp, tone.accent.copy(alpha = 0.62f), shape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = label, size = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(text = value, style = MaterialTheme.typography.labelLarge, color = WoodBrownDark, maxLines = 1)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun NestIdentityHeader(onOpenProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Le Nid", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xA823493E))
            .border(1.dp, SoftGold.copy(alpha = 0.78f), CircleShape).clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Person, contentDescription = "Profil", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun NestResourcesPanel(
    hasRemoteSession: Boolean, isLoading: Boolean, points: Int?, flames: Int?, crystals: Int?,
    onOpenWishes: () -> Unit, onOpenChests: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Color(0xB823493E)).border(1.dp, SoftGold.copy(alpha = 0.64f), RoundedCornerShape(24.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        when {
            !hasRemoteSession -> Text("Connecte-toi pour consulter tes ressources personnelles.",
                style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.padding(10.dp))
            isLoading || points == null || flames == null || crystals == null -> Text("Chargement des ressources...",
                style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.padding(10.dp))
            else -> {
                ResourceBalance("Points", points.toString(), com.example.taskoday.R.drawable.v2_resource_xp,
                    Color(0xFFFFD66D), Modifier.weight(1f))
                ResourceBalance("Flammèches", flames.toString(), NestAssets.interfaceAsset("flammeche"),
                    Color(0xFFFFB36B), Modifier.weight(1f), onOpenWishes)
                ResourceBalance("Cristaux", crystals.toString(), NestAssets.interfaceAsset("crystal"),
                    Color(0xFFB9E8FF), Modifier.weight(1f), onOpenChests)
            }
        }
    }
}

@Composable
private fun ResourceBalance(label: String, value: String, icon: Int, tint: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier.then(actionModifier).heightIn(min = 54.dp).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = tint)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NestCreatureStage(dragon: DragonUiItem?, egg: EggUiItem?, onOpenEggs: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(326.dp), contentAlignment = Alignment.Center) {
        if (dragon != null || egg != null) {
            val art = dragon?.assetResId ?: egg!!.assetResId
            Image(painterResource(art), contentDescription = dragon?.contentDescription ?: egg?.contentDescription,
                modifier = Modifier.fillMaxWidth().height(316.dp).padding(2.dp), contentScale = ContentScale.Fit)
            Column(Modifier.align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp))
                .background(Color(0xB823493E)).padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(dragon?.title ?: egg!!.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                Text(dragon?.stage ?: "œuf suivi ? ${egg!!.status}", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        } else {
            Image(painter = painterResource(com.example.taskoday.R.drawable.chronodria_nest_empty_state_v1),
                contentDescription = "Nid vide décoré sans œuf ni compagnon",
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(322.dp).padding(bottom = 44.dp),
                contentScale = ContentScale.Fit)
            Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
                .background(Color(0xC923493E)).border(1.dp, SoftGold.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text("Ton Nid est prêt à t'accueillir", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text("Aucun œuf ni compagnon pour le moment.", style = MaterialTheme.typography.labelSmall, color = Color.White)
                FantasyCompactButton(text = "Voir les œufs", onClick = onOpenEggs, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

@Composable
private fun NestActiveEggCard(
    egg: EggUiItem?,
    onOpenEggs: () -> Unit,
    onEvolveEgg: () -> Unit,
) {
    if (egg == null) {
        FantasyStateCard(
            title = "Aucun œuf ne repose encore dans ton Nid.",
            message = "Continue tes aventures pour en découvrir.",
            assetResId = NestAssets.interfaceAsset("egg_locked"),
            assetDescription = "Œuf à découvrir",
        )
        FantasyButton(
            text = "Voir mes œufs",
            onClick = onOpenEggs,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Outline,
        )
        return
    }

    FantasyCard(tone = FantasyTone.Gold, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(
                assetResId = egg.assetResId,
                contentDescription = egg.contentDescription,
                size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = egg.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                egg.familyLabel?.let { family ->
                    Text(
                        text = "Famille $family",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = egg.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MossGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        egg.nextStateLabel?.let { nextState ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Prochaine étape", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Text(
                    text = nextState,
                    style = MaterialTheme.typography.labelLarge,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (egg.resourceRows.isEmpty()) {
                Text(
                    text = "Aucune ressource requise pour le moment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            } else {
                egg.resourceRows.forEach { resource ->
                    EggResourceRequirementRow(resource)
                }
            }
        }
        if (!egg.actionLabel.isNullOrBlank()) {
            FantasyButton(
                text = egg.actionLabel,
                onClick = onEvolveEgg,
                modifier = Modifier.fillMaxWidth(),
                style = FantasyButtonStyle.Quiet,
                enabled = egg.actionEnabled && egg.id != null,
            )
        }
        FantasyButton(
            text = "Voir mes œufs",
            onClick = onOpenEggs,
            modifier = Modifier.align(Alignment.Start),
            style = FantasyButtonStyle.Outline,
        )
    }
}

@Composable
private fun EggResourceRequirementRow(resource: EggResourceUiItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FantasyAssetBubble(
            assetResId = NestAssets.itemAsset(resource.key, resource.category),
            contentDescription = resource.title,
            size = 34.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = resource.title,
                style = MaterialTheme.typography.bodyMedium,
                color = WoodBrownDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${resource.ownedQuantity} possédés • ${resource.requiredQuantity} nécessaires",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FantasyBadge(
            text =
                if (resource.missingQuantity > 0) {
                    "Manque ${resource.missingQuantity}"
                } else {
                    "OK"
                },
            tone = if (resource.missingQuantity > 0) FantasyTone.Ember else FantasyTone.Moss,
        )
    }
}

@Composable
private fun NestHubTiles(
    onOpenInventory: () -> Unit, onOpenEggs: () -> Unit,
    onOpenChests: () -> Unit, onOpenWishes: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        NestHubAction("Œufs", NestAssets.interfaceAsset("egg_locked"), Modifier.weight(1f), onOpenEggs)
        NestHubAction("Collection", NestAssets.interfaceAsset("inventory_empty"), Modifier.weight(1f), onOpenInventory)
        NestHubAction("Coffres", NestAssets.chestAsset("common"), Modifier.weight(1f), onOpenChests)
        NestHubAction("Caverne", NestAssets.interfaceAsset("wish_cave"), Modifier.weight(1f), onOpenWishes)
    }
}

@Composable
private fun NestHubAction(label: String, assetResId: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 82.dp).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xA823493E))
            .border(1.dp, SoftGold.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(assetResId), contentDescription = null, modifier = Modifier.size(34.dp), contentScale = ContentScale.Fit)
        }
        Text(label, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xB823493E))
            .padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall,
            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
    }
}

@Composable
private fun NestHubTile(
    title: String,
    subtitle: String,
    assetResId: Int,
    tone: FantasyTone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FantasyCard(
        modifier = modifier.clickable(onClick = onClick),
        tone = tone,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = title, size = 46.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BestiaryPreviewCard(
    dragons: List<DragonUiItem>,
    eggs: List<EggUiItem>,
    activeCompanionKey: String,
    onSelectDragon: (DragonUiItem) -> Unit,
    onSelectEgg: (EggUiItem) -> Unit,
    onOpenDragons: () -> Unit,
    onOpenEggs: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Violet, contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(text = "Bestiaire du Nid", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                Text(
                    text = "Compagnon affiché et œuf suivi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FantasyBadge(text = "Hub", tone = FantasyTone.Violet)
        }
        dragons.take(2).forEach { dragon ->
            val isActive = dragon.active || dragon.key == activeCompanionKey
            BestiaryChoiceRow(
                title = dragon.title,
                subtitle = "Dragon débloqué",
                assetResId = dragon.assetResId,
                contentDescription = dragon.contentDescription,
                actionLabel = if (isActive) "Actif" else "Choisir",
                enabled = !isActive,
                badgeTone = if (isActive) FantasyTone.Moss else FantasyTone.Gold,
                onClick = { onSelectDragon(dragon) },
            )
        }
        eggs.take(1).forEach { egg ->
            BestiaryChoiceRow(
                title = egg.title,
                subtitle = "Œuf découvert",
                assetResId = egg.assetResId,
                contentDescription = egg.contentDescription,
                actionLabel = "Voir",
                enabled = true,
                badgeTone = FantasyTone.Gold,
                onClick = { onSelectEgg(egg) },
            )
        }
        BestiaryChoiceRow(
            title = "Dragon non découvert",
            subtitle = "Fais éclore un Œuf pour rencontrer ton premier dragon.",
            assetResId = NestAssets.interfaceAsset("egg_locked"),
            contentDescription = "Œuf verrouillé",
            actionLabel = "Verrouillé",
            enabled = false,
            badgeTone = FantasyTone.Night,
            onClick = {},
        )
        NavigationButtons(
            primaryLabel = "Bestiaire complet",
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
    badgeTone: FantasyTone = FantasyTone.Gold,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ParchmentLight.copy(alpha = 0.98f),
                            Color(0xFFFFE8B8).copy(alpha = 0.88f),
                            MagicViolet.copy(alpha = 0.12f),
                        ),
                    ),
                )
                .border(1.2.dp, SoftGold.copy(alpha = 0.72f), shape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
            if (enabled) {
                FantasyCompactButton(
                    text = actionLabel,
                    onClick = onClick,
                    modifier = Modifier.widthIn(max = 92.dp),
                )
            } else {
                FantasyBadge(text = actionLabel, tone = badgeTone, modifier = Modifier.widthIn(max = 96.dp))
            }
        }
    }
}

@Composable
private fun FamilyBestiaryCard(
    dragon: DragonUiItem,
    egg: EggUiItem?,
    onActivate: () -> Unit,
    onEvolveEgg: () -> Unit,
    onEvolveDragon: () -> Unit,
) {
    val cardTone =
        when {
            dragon.active -> FantasyTone.Gold
            dragon.discovered -> FantasyTone.Violet
            else -> FantasyTone.Night
        }
    FantasyCard(tone = cardTone, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(
                assetResId = dragon.assetResId,
                contentDescription = dragon.contentDescription,
                size = 58.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = dragon.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                dragon.familyLabel?.takeIf { label -> label != dragon.title }?.let { familyLabel ->
                    Text(
                        text = familyLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (dragon.owned) {
                    Text(
                        text = "Stade actuel",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = dragon.stage,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FantasyProgressBar(progress = dragon.progress)
                if (dragon.owned) {
                    Text(
                        text = "${dragon.progressPercent}% de progression",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FantasyBadge(
                text = if (dragon.active) "Compagnon" else if (dragon.discovered) "Découvert" else "Verrouillé",
                tone = if (dragon.active) FantasyTone.Moss else if (dragon.discovered) FantasyTone.Gold else FantasyTone.Night,
            )
        }
        if (dragon.active) {
            Text(
                text = "Compagnon actuel",
                style = MaterialTheme.typography.labelLarge,
                color = MossGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (dragon.owned) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Prochaine évolution",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dragon.nextStageLabel ?: "Stade actuel maximal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            dragon.resourceRows.forEach { resource ->
                EggResourceRequirementRow(resource)
            }
        }
        if (!dragon.active && dragon.discovered && dragon.id != null) {
            FantasyButton(
                text = "Définir comme compagnon",
                onClick = onActivate,
                style = FantasyButtonStyle.Outline,
            )
        }
        if (dragon.nextStageLabel != null && dragon.id != null) {
            FantasyButton(
                text = "Faire évoluer",
                onClick = onEvolveDragon,
                style = FantasyButtonStyle.Quiet,
                enabled = dragon.canEvolve,
            )
        }
        if (egg != null) {
            BestiaryChoiceRow(
                title = egg.title,
                subtitle = if (egg.locked) egg.status else "Œuf possédé • État : ${egg.status}",
                assetResId = egg.assetResId,
                contentDescription = egg.contentDescription,
                actionLabel = "${(egg.progress * 100).toInt()} %",
                enabled = false,
                badgeTone = FantasyTone.Violet,
                onClick = {},
            )
            Text(
                text = egg.requirements,
                style = MaterialTheme.typography.bodySmall,
                color = if (egg.actionEnabled) MossGreen else InkMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!egg.locked && egg.id != null) {
                FantasyButton(
                    text = egg.actionLabel ?: "Évoluer",
                    onClick = onEvolveEgg,
                    style = FantasyButtonStyle.Quiet,
                    enabled = egg.actionEnabled,
                )
            }
        }
        if (dragon.eggStatesLabel.isNotBlank()) {
            Text(
                text = "Œuf : ${dragon.eggStatesLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
        if (dragon.dragonStagesLabel.isNotBlank()) {
            Text(
                text = "Dragon : ${dragon.dragonStagesLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Artefact légendaire",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
            FantasyBadge(
                text = if (dragon.artifactOwned >= dragon.artifactRequired) "Possédé" else "Verrouillé",
                tone = if (dragon.artifactOwned >= dragon.artifactRequired) FantasyTone.Moss else FantasyTone.Night,
            )
        }
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
    backgroundResId: Int? = null,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        if (backgroundResId == null) {
            FantasyScreenBackground(modifier = Modifier.statusBarsPadding().padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.medium),
                    contentPadding = PaddingValues(top = MaterialTheme.spacing.large, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    content = content,
                )
            }
        } else {
            Box(Modifier.fillMaxSize().statusBarsPadding().padding(innerPadding)) {
                Image(painter = painterResource(backgroundResId), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.medium),
                    contentPadding = PaddingValues(top = MaterialTheme.spacing.large, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    content = content,
                )
            }
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
    onBackToNest: (() -> Unit)? = null,
    message: String? = null,
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
                onBackClick = onBackToNest,
            )
        }
        if (!message.isNullOrBlank()) {
            item {
                FantasyStateCard(
                    title = "Information du Nid",
                    message = message,
                    assetResId = NestAssets.interfaceAsset("nid"),
                )
            }
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

data class EggResourceUiItem(
    val key: String,
    val title: String,
    val ownedQuantity: Int,
    val requiredQuantity: Int,
    val category: String = "material",
) {
    val missingQuantity: Int
        get() = (requiredQuantity - ownedQuantity).coerceAtLeast(0)
}

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
    val actionEnabled: Boolean = true,
    val id: Long? = null,
    val familyKey: String = key.removePrefix("egg_").removePrefix("oeuf_"),
    val familyLabel: String? = null,
    val progressPercent: Int = (progress.coerceIn(0f, 1f) * 100).toInt(),
    val nextStateLabel: String? = null,
    val resourceRows: List<EggResourceUiItem> = emptyList(),
    val hatched: Boolean = false,
)

data class DragonUiItem(
    val key: String,
    val title: String,
    val stage: String,
    val nextStep: String,
    val assetResId: Int,
    val contentDescription: String = title,
    val active: Boolean = false,
    val progress: Float = 0.45f,
    val id: Long? = null,
    val discovered: Boolean = true,
    val eggStatesLabel: String = "",
    val dragonStagesLabel: String = "",
    val artifactOwned: Int = 0,
    val artifactRequired: Int = 1,
    val canEvolve: Boolean = false,
    val owned: Boolean = true,
    val familyLabel: String? = null,
    val progressPercent: Int = (progress.coerceIn(0f, 1f) * 100).toInt(),
    val nextStageLabel: String? = null,
    val resourceRows: List<EggResourceUiItem> = emptyList(),
)

data class ScrollUiItem(
    val title: String,
    val code: String,
    val status: String,
    val statusKey: String,
)

private fun InventoryItemDto.toUiItem(): LootUiItem =
    LootUiItem(
        key = key,
        title = key.toTaskodayDisplayLabel(),
        rarityLabel = "${rarity.toTaskodayDisplayLabel()} • ${category.toTaskodayDisplayLabel()}",
        quantity = quantity,
        assetResId = NestAssets.itemAsset(key, category),
        usageLabel = category.toTaskodayDisplayLabel(),
    )

private fun ChestDto.toUiItem(): LootUiItem =
    LootUiItem(
        key = "chest_$id",
        title = name,
        rarityLabel = "${rarity.toTaskodayDisplayLabel()} • Coffre possédé",
        quantity = 1,
        assetResId = NestAssets.chestAsset(rarity),
        usageLabel = "À ouvrir dans la Caverne",
    )

internal fun selectActiveNestEgg(eggs: List<EggUiItem>): EggUiItem? {
    val ownedEggs = eggs.filter { egg -> !egg.locked && !egg.hatched && egg.id != null }
    return ownedEggs.firstOrNull { egg -> egg.nextStateLabel != null && egg.progressPercent in 1..99 }
        ?: ownedEggs.firstOrNull { egg -> egg.nextStateLabel != null }
        ?: ownedEggs.firstOrNull()
}

internal fun eggResourceRows(
    egg: EggDto?,
    inventory: InventoryDto?,
): List<EggResourceUiItem> {
    if (egg == null) return emptyList()
    val inventoryItemsByKey = inventory?.items.orEmpty().associateBy { item -> item.key }
    return egg.requirements.entries
        .sortedBy { (key, _) -> key }
        .map { (key, required) ->
            val inventoryItem = inventoryItemsByKey[key]
            EggResourceUiItem(
                key = key,
                title = key.toTaskodayDisplayLabel(),
                ownedQuantity = inventoryItem?.quantity ?: 0,
                requiredQuantity = required,
                category = inventoryItem?.category ?: "material",
            )
        }
}

internal fun eggNextStateLabel(egg: EggDto): String? = egg.nextState?.toFantasyStateLabel()

internal fun dragonNextStageLabel(nextStage: String?): String? = nextStage?.toFantasyStateLabel()

internal fun dragonResourceRows(resources: List<RequiredResourceDto>?): List<EggResourceUiItem> =
    resources.orEmpty()
        .sortedBy { resource -> resource.itemKey }
        .map { resource ->
            EggResourceUiItem(
                key = resource.itemKey,
                title = resource.title.ifBlank { resource.itemKey.toTaskodayDisplayLabel() },
                ownedQuantity = resource.ownedQuantity,
                requiredQuantity = resource.requiredQuantity,
            )
        }

private fun EggDto.toUiItem(inventory: InventoryDto?): EggUiItem {
    val family = eggKey.removePrefix("oeuf_").removePrefix("egg_")
    val actionState = eggEvolutionActionState(this, inventory)
    val displayTitle = title.ifBlank { eggKey.toTaskodayDisplayLabel() }
    val progress = progressPercent.coerceIn(0, 100)
    val requirementsLabel =
        actionState.requirementsLabel
    return EggUiItem(
        key = eggKey,
        title = displayTitle,
        status = state.toFantasyStateLabel(),
        requirements = requirementsLabel,
        progress = progress / 100f,
        assetResId = NestAssets.eggAsset(family.toVisualFamily(), state),
        contentDescription = "$displayTitle, état ${state.toFantasyStateLabel()}",
        materialLabel = "$progress% de progression",
        actionLabel = actionState.label,
        actionEnabled = actionState.enabled,
        id = id,
        familyKey = family,
        familyLabel = family.toTaskodayDisplayLabel(),
        progressPercent = progress,
        nextStateLabel = eggNextStateLabel(this),
        resourceRows = eggResourceRows(this, inventory),
        hatched = isHatched(),
    )
}

internal fun DragonDto.toUiItem(): DragonUiItem {
    val family = dragonKey.removePrefix("dragon_")
    val progress = progressPercent.coerceIn(0, 100)
    val currentStageKey = currentStage ?: stage
    val nextStageLabel = dragonNextStageLabel(nextStage)
    return DragonUiItem(
        key = dragonKey,
        title = title,
        stage = currentStageKey.toFantasyStateLabel(),
        nextStep = nextStageLabel ?: "Stade actuel : ${currentStageKey.toFantasyStateLabel()}",
        assetResId = NestAssets.dragonAsset(family.toVisualFamily(), currentStageKey),
        contentDescription = "$title, stade ${currentStageKey.toFantasyStateLabel()}",
        active = activeCompanion,
        progress = progress / 100f,
        id = id,
        canEvolve = nextStageLabel != null && canEvolve,
        familyLabel = family.toTaskodayDisplayLabel(),
        progressPercent = progress,
        nextStageLabel = nextStageLabel,
        resourceRows = dragonResourceRows(requiredResources),
    )
}

internal fun BestiaryFamilyDto.toDragonUiItem(dragon: DragonDto?): DragonUiItem {
    val familyDiscovered = isBestiaryFamilyDiscovered(discovered, eggOwned, dragonOwned)
    val backendDragon = dragon ?: this.dragon
    val currentStageKey = backendDragon?.currentStage ?: currentDragonStage ?: backendDragon?.stage
    val progress = (backendDragon?.progressPercent ?: progressPercent).coerceIn(0, 100)
    val nextStageLabel = if (dragonOwned) dragonNextStageLabel(nextDragonStage ?: backendDragon?.nextStage) else null
    val canEvolve = nextStageLabel != null && (dragonCanEvolve ?: backendDragon?.canEvolve ?: false)
    return DragonUiItem(
        key = "dragon_$familyId",
        title = backendDragon?.title?.ifBlank { "Dragon $familyName" } ?: familyName,
        stage =
            currentStageKey?.toFantasyStateLabel()
                ?: if (familyDiscovered) "Dragon non obtenu" else "Non découvert",
        nextStep = if (dragonOwned) "$progress% de progression" else "Fais éclore l'œuf de cette famille.",
        assetResId = NestAssets.dragonAsset(familyId.toVisualFamily(), currentStageKey ?: "baby"),
        contentDescription =
            "$familyName, ${
                currentStageKey?.toFantasyStateLabel()
                    ?: if (familyDiscovered) "dragon non obtenu" else "verrouillé"
            }",
        active = activeCompanion,
        progress = progress / 100f,
        id = backendDragon?.id,
        discovered = familyDiscovered,
        eggStatesLabel = eggStates.joinToString(" • ") { "${it.state.toFantasyStateLabel()} ${if (it.unlocked) "✓" else "—"}" },
        dragonStagesLabel = dragonStages.joinToString(" • ") { "${it.state.toFantasyStateLabel()} ${if (it.unlocked) "✓" else "—"}" },
        artifactOwned = legendaryArtifact.owned,
        artifactRequired = legendaryArtifact.required,
        canEvolve = canEvolve,
        owned = dragonOwned,
        familyLabel = familyName,
        progressPercent = progress,
        nextStageLabel = nextStageLabel,
        resourceRows = dragonResourceRows(dragonRequiredResources ?: backendDragon?.requiredResources),
    )
}

private fun BestiaryFamilyDto.toEggUiItem(
    egg: EggDto?,
    inventory: InventoryDto?,
): EggUiItem {
    val actionState = eggEvolutionActionState(egg, inventory)
    val progress = (egg?.progressPercent ?: progressPercent).coerceIn(0, 100)
    return EggUiItem(
        key = "oeuf_$familyId",
        title = "Œuf $familyName",
        status = currentEggState?.toFantasyStateLabel() ?: "Verrouillé",
        requirements = if (eggOwned) actionState.requirementsLabel else "Œuf non découvert",
        progress = progress / 100f,
        assetResId = NestAssets.eggAsset(familyId.toVisualFamily(), currentEggState ?: "sleeping"),
        locked = !eggOwned,
        contentDescription = "Œuf $familyName, ${currentEggState?.toFantasyStateLabel() ?: "verrouillé"}",
        materialLabel = "$progress% de progression",
        actionLabel = if (eggOwned) actionState.label else null,
        actionEnabled = eggOwned && actionState.enabled,
        id = egg?.id,
        familyKey = familyId,
        familyLabel = familyName,
        progressPercent = progress,
        nextStateLabel = egg?.let(::eggNextStateLabel),
        resourceRows = eggResourceRows(egg, inventory),
        hatched = egg?.isHatched() == true,
    )
}

private fun EggDto.isHatched(): Boolean =
    !hatchedAt.isNullOrBlank() || status.equals("hatched", ignoreCase = true)

private fun String.toVisualFamily(): String =
    when (lowercase()) {
        "braise" -> "pyron"
        "lunaire" -> "lunarys"
        "racine" -> "sylvyn"
        else -> lowercase()
    }

internal fun String.toFantasyStateLabel(): String =
    when (lowercase()) {
        "sleeping" -> "Endormi"
        "warm" -> "Tiède"
        "glowing" -> "Lumineux"
        "cracked" -> "Fissuré"
        "hatching" -> "Éclosion"
        "baby" -> "Bébé"
        "young" -> "Jeune"
        "medium" -> "Adulte"
        "large" -> "Grand"
        "legendary" -> "Légendaire"
        else -> replaceFirstChar { first -> first.uppercase() }
    }

private val sampleLoot =
    listOf(
        LootUiItem("chest_common", "Coffre commun", "coffre possédé", 1, NestAssets.chestAsset("common"), "À ouvrir dans la Caverne"),
        LootUiItem("wood_logs", "Rondins de bois", "commun", 3, NestAssets.inventoryItemAsset("wood_logs"), "Refuge"),
        LootUiItem("leaf_sprout", "Pousse de feuille", "commun", 4, NestAssets.inventoryItemAsset("leaf_sprout"), "Œuf ou Refuge"),
        LootUiItem("mushroom", "Champignon", "commun", 2, NestAssets.inventoryItemAsset("mushroom"), "Œuf"),
        LootUiItem("potion", "Potion douce", "peu commun", 1, NestAssets.inventoryItemAsset("potion"), "Dragon ou Œuf"),
        LootUiItem("lantern", "Lanterne", "peu commun", 1, NestAssets.inventoryItemAsset("lantern"), "Refuge"),
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

private val sampleScrolls =
    listOf(
        ScrollUiItem("Choisir le dessert", "TASKO-12-AB34CD", "disponible", "approved"),
    )
