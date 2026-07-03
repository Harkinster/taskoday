package com.example.taskoday.features.activity

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.RewardToast
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun ActivityJournalScreen(
    viewModel: ActivityJournalViewModel,
    isLocalChildMode: Boolean = false,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        title = "Journal",
                        message = "Les dernieres traces se rassemblent.",
                        loading = true,
                        tone = FantasyTone.Gold,
                    )
                }
                return@FantasyScreenBackground
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayTopBar(
                        avatarInitials = "AB",
                        compact = true,
                        showNotification = false,
                        onAvatarClick = onOpenProfile,
                    )
                }

                item {
                    FantasyButton(
                        text = "Retour",
                        onClick = onBack,
                        style = FantasyButtonStyle.Quiet,
                    )
                }

                item {
                    ActivityJournalHeader(uiState, isLocalChildMode = isLocalChildMode)
                }

                uiState.errorMessage?.let { message ->
                    item {
                        RewardToast(message = message, tone = FantasyTone.Ember)
                    }
                }

                if (uiState.events.isEmpty()) {
                    item {
                        FantasyStateCard(
                            title = "Aucune aventure pour le moment",
                            message = "Les actions terminées et les souhaits apparaîtront ici.",
                            assetResId = NestAssets.interfaceAsset("nid"),
                            assetDescription = null,
                            tone = FantasyTone.Moss,
                        )
                    }
                } else {
                    items(uiState.events, key = { item -> item.id }) { item ->
                        ActivityJournalRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityJournalHeader(
    uiState: ActivityJournalUiState,
    isLocalChildMode: Boolean,
) {
    FantasyCard(tone = FantasyTone.Violet) {
        Text(
            text = if (uiState.isParent && !isLocalChildMode) "Journal parent" else "Mes dernières réussites",
            style = MaterialTheme.typography.titleLarge,
            color = WoodBrownDark,
            maxLines = 1,
        )
        Text(
            text = uiState.childLabel ?: "Enfant sélectionné",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Actions, souhaits et parchemins récents.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            maxLines = 2,
        )
    }
}

@Composable
private fun ActivityJournalRow(item: ActivityJournalItem) {
    FantasyCard(
        tone = item.kind.toTone(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(
                assetResId = item.kind.toAssetRes(),
                contentDescription = item.typeLabel,
                size = 42.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.typeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = item.kind.toAccentColor(),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    item.dateLabel?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun ActivityJournalKind.toTone(): FantasyTone =
    when (this) {
        ActivityJournalKind.ACTION -> FantasyTone.Moss
        ActivityJournalKind.WISH_PENDING -> FantasyTone.Gold
        ActivityJournalKind.WISH_APPROVED -> FantasyTone.Violet
        ActivityJournalKind.WISH_REFUSED -> FantasyTone.Ember
        ActivityJournalKind.WISH_USED -> FantasyTone.Wood
    }

private fun ActivityJournalKind.toAccentColor(): Color =
    when (this) {
        ActivityJournalKind.ACTION -> MossGreen
        ActivityJournalKind.WISH_PENDING -> EmberOrange
        ActivityJournalKind.WISH_APPROVED -> SoftGold
        ActivityJournalKind.WISH_REFUSED -> EmberOrange
        ActivityJournalKind.WISH_USED -> WoodBrownDark
    }

private fun ActivityJournalKind.toAssetRes(): Int =
    when (this) {
        ActivityJournalKind.ACTION -> NestAssets.interfaceAsset("nid")
        ActivityJournalKind.WISH_PENDING -> NestAssets.scrollAsset("pending")
        ActivityJournalKind.WISH_APPROVED -> NestAssets.scrollAsset("approved")
        ActivityJournalKind.WISH_REFUSED -> NestAssets.scrollAsset("refused")
        ActivityJournalKind.WISH_USED -> NestAssets.scrollAsset("used")
    }
