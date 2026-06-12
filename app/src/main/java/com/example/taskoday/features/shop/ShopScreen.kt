package com.example.taskoday.features.shop

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyHeader
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.NestStatCard
import com.example.taskoday.core.ui.component.fantasy.ScrollCard
import com.example.taskoday.core.ui.component.fantasy.WishCard
import com.example.taskoday.core.ui.format.toTaskodayDisplayLabel
import com.example.taskoday.core.ui.component.fantasy.ChestCard
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(
    viewModel: ShopViewModel,
    initialSection: String,
    onOpenProfile: () -> Unit,
    onBackToNest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedSection by rememberSaveable(initialSection) {
        mutableStateOf(if (initialSection == "chests") CaveSection.Chests else CaveSection.Wishes)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        FantasyScreenBackground(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    FantasyStateCard(
                        title = "La Caverne s'éclaire",
                        message = "Les Souhaits arrivent dans un instant.",
                        loading = true,
                        tone = FantasyTone.Ember,
                    )
                }
                return@FantasyScreenBackground
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    FantasyHeader(
                        title = "Caverne",
                        subtitle = "Dépense tes Flammèches en Souhaits ou tes Cristaux en Coffres.",
                        assetResId = NestAssets.interfaceAsset("wish_cave"),
                        assetDescription = "Caverne",
                        onAvatarClick = onOpenProfile,
                        onBackClick = onBackToNest,
                    )
                }

                item {
                    CaveSectionSelector(
                        selected = selectedSection,
                        onSelect = { selectedSection = it },
                    )
                }

                if (selectedSection == CaveSection.Wishes) {
                    item {
                        NestStatCard(
                            label = if (uiState.isParent) "Souhaits parent" else "Solde du Gardien",
                            value = "${uiState.scalesBalance} Flammèches",
                            assetResId = NestAssets.interfaceAsset("flammeche"),
                            tone = FantasyTone.Ember,
                        )
                    }

                    if (!uiState.hasRemoteSession) {
                        item {
                            FantasyCard(tone = FantasyTone.Night) {
                                Text(text = "Mode local", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                                Text(
                                    text = "Connecte un compte pour suivre les demandes et les Parchemins.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted,
                                )
                            }
                        }
                    }

                    if (uiState.isParent && uiState.hasRemoteSession) {
                        item {
                            ParentRewardCreator(
                                isSubmitting = uiState.isSubmitting,
                                onCreate = viewModel::createReward,
                            )
                        }
                    }

                    item { SectionTitle(text = "Souhaits disponibles") }

                    if (uiState.rewards.isEmpty()) {
                        item {
                            EmptyCard(text = "Aucun Souhait pour le moment. La Caverne garde sa magie au chaud.")
                        }
                    } else {
                        items(uiState.rewards, key = { reward -> reward.id }) { reward ->
                            val alreadyRequested =
                                uiState.requests.any { request ->
                                    request.rewardId == reward.id &&
                                        request.status in setOf(RewardRequestStatus.PENDING, RewardRequestStatus.APPROVED)
                                }
                            RewardRow(
                                reward = reward,
                                isParent = uiState.isParent,
                                hasRemoteSession = uiState.hasRemoteSession,
                                scalesBalance = uiState.scalesBalance,
                                alreadyRequested = alreadyRequested,
                                isSubmitting = uiState.isSubmitting,
                                onRequest = { viewModel.requestReward(reward) },
                            )
                        }
                    }

                    item { SectionTitle(text = "Demandes et Parchemins") }

                    if (uiState.requests.isEmpty()) {
                        item {
                            if (uiState.hasRemoteSession) {
                                EmptyCard(text = "Aucun Parchemin pour le moment.")
                            } else {
                                LocalHistory(transactions = uiState.localTransactions)
                            }
                        }
                    } else {
                        items(uiState.requests, key = { request -> request.id }) { request ->
                            RewardRequestRow(
                                request = request,
                                isParent = uiState.isParent,
                                isSubmitting = uiState.isSubmitting,
                                onApprove = { viewModel.approveRequest(request.id) },
                                onRefuse = { viewModel.refuseRequest(request.id) },
                                onUseCoupon = { couponId -> viewModel.useCoupon(couponId) },
                            )
                        }
                    }
                } else {
                    item {
                        NestStatCard(
                            label = "Cristaux disponibles",
                            value = "${uiState.chestCatalog?.crystalsBalance ?: 6} Cristaux",
                            assetResId = NestAssets.interfaceAsset("crystal"),
                            tone = FantasyTone.Violet,
                        )
                    }
                    uiState.lastOpenedChest?.let { result ->
                        item {
                            OpenedChestResultCard(result = result)
                        }
                    }
                    if (!uiState.hasRemoteSession || uiState.chestCatalog == null) {
                        item {
                            FantasyCard(tone = FantasyTone.Night) {
                                Text(text = "Coffres", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                                Text(
                                    text = "Aperçu local. Connecte un compte enfant pour ouvrir les coffres avec tes Cristaux.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                )
                            }
                        }
                    }
                    val displayedChests =
                        uiState.chestCatalog?.chests?.map { chest ->
                            CaveChest(
                                id = chest.id,
                                rarity = chest.rarity,
                                title = "Coffre ${chest.rarity.toTaskodayDisplayLabel().lowercase()}",
                                cost = chest.crystalCost,
                                hint = chest.possibleRewards.joinToString(", ") { reward -> reward.toTaskodayDisplayLabel() },
                            )
                        } ?: caveChests
                    items(displayedChests, key = { chest -> chest.id }) { chest ->
                        val hasRemoteCatalog = uiState.hasRemoteSession && uiState.chestCatalog != null
                        val actionState =
                            chestActionState(
                                crystalsBalance = uiState.chestCatalog?.crystalsBalance,
                                crystalCost = chest.cost,
                                hasRemoteCatalog = hasRemoteCatalog,
                                isSubmitting = uiState.isSubmitting,
                            )
                        ChestCard(
                            points = 0,
                            pointsRequired = 0,
                            unopenedChests = 0,
                            title = chest.title,
                            costLabel = "${actionState.balanceLabel} • ${chest.hint}",
                            actionLabel = actionState.label,
                            actionEnabled = actionState.enabled,
                            onAction = {
                                if (hasRemoteCatalog) {
                                    viewModel.openChest(chest.id)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Connecte un compte enfant pour ouvrir ce coffre.")
                                    }
                                }
                            },
                            assetResId = NestAssets.chestAsset(chest.rarity),
                            contentDescription = chest.title,
                            rarity = chest.rarity,
                        )
                    }
                }
            }
        }
    }
}

private enum class CaveSection(
    val label: String,
) {
    Wishes("Souhaits"),
    Chests("Coffres"),
}

private data class CaveChest(
    val id: String,
    val rarity: String,
    val title: String,
    val cost: Int,
    val hint: String,
)

private val caveChests =
    listOf(
        CaveChest(id = "common", rarity = "common", title = "Coffre commun", cost = 5, hint = "Loot courant"),
        CaveChest(id = "rare", rarity = "rare", title = "Coffre rare", cost = 15, hint = "Chance de loot rare"),
        CaveChest(id = "epic", rarity = "epic", title = "Coffre épique", cost = 40, hint = "Chance de loot épique"),
    )

@Composable
private fun OpenedChestResultCard(result: com.example.taskoday.data.remote.dto.OpenCatalogChestDto) {
    FantasyCard(tone = FantasyTone.Gold) {
        Text(text = "${result.chest.name} ouvert", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
        Text(
            text = "${result.crystalsSpent} Cristaux dépensés • ${result.crystalsBalance} restants",
            style = MaterialTheme.typography.bodyMedium,
            color = MagicViolet,
        )
        result.loot.forEach { loot ->
            Text(
                text = "+${loot.quantity} ${loot.title}${if (loot.isDuplicateCompensation) " (compensation)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
        result.grantedEgg?.let { egg ->
            Text(text = "Nouvel œuf : ${egg.title}", style = MaterialTheme.typography.bodyMedium, color = MossGreen)
        }
    }
}

@Composable
private fun CaveSectionSelector(
    selected: CaveSection,
    onSelect: (CaveSection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        CaveSection.entries.forEach { section ->
            FantasyButton(
                text = section.label,
                onClick = { onSelect(section) },
                style = if (selected == section) FantasyButtonStyle.Filled else FantasyButtonStyle.Outline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = WoodBrownDark,
    )
}

@Composable
private fun ParentRewardCreator(
    isSubmitting: Boolean,
    onCreate: (String, String?, Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var costText by rememberSaveable { mutableStateOf("") }

    FantasyCard(modifier = Modifier.fillMaxWidth(), tone = FantasyTone.Violet) {
        Text(text = "Nouveau Souhait", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre") },
            singleLine = true,
            colors = fantasyTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            colors = fantasyTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = costText,
            onValueChange = { costText = it.filter { c -> c.isDigit() }.take(5) },
            label = { Text("Coût en Flammèches") },
            singleLine = true,
            colors = fantasyTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        FantasyButton(
            text = if (isSubmitting) "Création..." else "Créer le Souhait",
            onClick = {
                onCreate(title, description, costText.toIntOrNull() ?: 0)
                title = ""
                description = ""
                costText = ""
            },
            enabled = !isSubmitting && title.isNotBlank() && (costText.toIntOrNull() ?: 0) > 0,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xSmall),
        )
    }
}

@Composable
private fun RewardRow(
    reward: Reward,
    isParent: Boolean,
    hasRemoteSession: Boolean,
    scalesBalance: Int,
    alreadyRequested: Boolean,
    isSubmitting: Boolean,
    onRequest: () -> Unit,
) {
    val canRequest = hasRemoteSession && !isParent && scalesBalance >= reward.cost && !alreadyRequested
    val supportingText =
        when {
            isParent -> "Ce Souhait est prêt pour les Gardiens."
            alreadyRequested -> "Souhait déjà demandé : il est déjà en chemin."
            !hasRemoteSession -> "Connecte le compte enfant pour faire un souhait."
            scalesBalance < reward.cost -> "Il manque encore ${reward.cost - scalesBalance} Flammèches pour ce Souhait."
            else -> null
        }

    WishCard(
        title = "${reward.emoji} ${reward.title}",
        description = reward.description ?: "Souhait créé par un parent.",
        costLabel = "${reward.cost} Flammèches",
        enabled = canRequest && !isSubmitting,
        supportingText = supportingText,
        contentDescription = "Flammèche",
        onMakeWish = if (isParent) null else onRequest,
    )
}

@Composable
private fun RewardRequestRow(
    request: RewardRedemptionRequest,
    isParent: Boolean,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onRefuse: () -> Unit,
    onUseCoupon: (Long) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    request.coupon?.let { coupon ->
        ScrollCard(
            title = request.rewardTitle,
            code = coupon.code,
            status = request.status.displayLabel(),
            assetResId = request.status.toScrollAssetRes(),
            contentDescription = "Parchemin ${request.status.displayLabel()}",
            onUse =
                if (isParent && request.status == RewardRequestStatus.APPROVED) {
                    { onUseCoupon(coupon.id) }
                } else {
                    null
                },
        )
        return
    }

    FantasyCard(modifier = Modifier.fillMaxWidth(), tone = request.status.toTone()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = request.rewardTitle,
                style = MaterialTheme.typography.titleMedium,
                color = WoodBrownDark,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = request.status.displayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = request.status.toColor(),
            )
        }
        Text(
            text = "${request.costScales} Flammèches",
            style = MaterialTheme.typography.bodyMedium,
            color = MagicViolet,
        )
        if (request.status == RewardRequestStatus.PENDING) {
            Text(
                text = "Parchemin en attente de validation.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }

        if (isParent && request.status == RewardRequestStatus.PENDING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                FantasyButton(
                    text = "Accepter",
                    onClick = onApprove,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                )
                FantasyButton(
                    text = "Refuser",
                    onClick = onRefuse,
                    enabled = !isSubmitting,
                    style = FantasyButtonStyle.Outline,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    FantasyCard(modifier = Modifier.fillMaxWidth(), tone = FantasyTone.Gold) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
    }
}

@Composable
private fun LocalHistory(transactions: List<PointsTransaction>) {
    if (transactions.isEmpty()) {
        EmptyCard(text = "Aucune demande.")
        return
    }

    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        transactions.forEach { transaction ->
            FantasyCard(modifier = Modifier.fillMaxWidth(), tone = FantasyTone.Moss) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = transaction.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WoodBrownDark,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${transaction.amount} Flammèches",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmberOrange,
                    )
                }
            }
        }
    }
}

@Composable
private fun fantasyTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = WoodBrownDark,
        unfocusedTextColor = WoodBrownDark,
        focusedBorderColor = EmberOrange,
        unfocusedBorderColor = SoftGold.copy(alpha = 0.9f),
        focusedLabelColor = EmberOrange,
        unfocusedLabelColor = InkMuted,
        cursorColor = EmberOrange,
    )

private fun RewardRequestStatus.toTone(): FantasyTone =
    when (this) {
        RewardRequestStatus.PENDING -> FantasyTone.Gold
        RewardRequestStatus.APPROVED -> FantasyTone.Moss
        RewardRequestStatus.REFUSED -> FantasyTone.Ember
        RewardRequestStatus.USED -> FantasyTone.Wood
        RewardRequestStatus.EXPIRED -> FantasyTone.Ember
    }

private fun RewardRequestStatus.toColor(): Color =
    when (this) {
        RewardRequestStatus.PENDING -> EmberOrange
        RewardRequestStatus.APPROVED -> MossGreen
        RewardRequestStatus.REFUSED -> DangerGlow
        RewardRequestStatus.USED -> WoodBrownDark
        RewardRequestStatus.EXPIRED -> DangerGlow
    }

private fun RewardRequestStatus.toScrollAssetRes(): Int =
    NestAssets.scrollAsset(name.lowercase())

private fun RewardRequestStatus.displayLabel(): String =
    when (this) {
        RewardRequestStatus.PENDING -> "En attente"
        RewardRequestStatus.APPROVED -> "Approuvée"
        RewardRequestStatus.REFUSED -> "Refusée"
        RewardRequestStatus.USED -> "Utilisée"
        RewardRequestStatus.EXPIRED -> "Expirée"
    }
