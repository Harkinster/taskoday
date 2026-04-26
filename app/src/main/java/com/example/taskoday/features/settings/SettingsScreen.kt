package com.example.taskoday.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProfileHeroCard
import com.example.taskoday.core.ui.component.fantasy.RewardsCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.component.fantasy.TaskodayRewardItem
import com.example.taskoday.core.ui.component.fantasy.TaskodayStatItem
import com.example.taskoday.core.ui.component.fantasy.StatsCard
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenParentMode: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var pairingCodeInput by rememberSaveable { mutableStateOf("") }

    val points = uiState.totalXp
    val level = uiState.level
    val nextLevelXp = uiState.nextLevelXp
    val levelXp = uiState.levelXp
    val levelProgress =
        if (nextLevelXp == 0) {
            0f
        } else {
            levelXp.toFloat() / nextLevelXp.toFloat()
        }

    val stats =
        listOf(
            TaskodayStatItem(label = "Missions", value = uiState.missionsStat),
            TaskodayStatItem(label = "Quetes", value = uiState.questsStat),
            TaskodayStatItem(label = "Serie", value = uiState.streakStat),
            TaskodayStatItem(label = "Succes", value = uiState.successStat),
        )

    val rewardItems =
        uiState.xpHistoryTokens
            .take(4)
            .mapIndexed { index, token ->
                TaskodayRewardItem(
                    label = "XP ${index + 1}",
                    value = token,
                    emoji = rewardEmojiForIndex(index),
                )
            }.ifEmpty {
                listOf(
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "✨"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "⭐"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "🔥"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "🏆"),
                )
            }

    Scaffold(
        containerColor = Color.Transparent,
    ) { innerPadding ->
        FantasyScreenBackground(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayHeader(
                        title = "Profil",
                        subtitle = "Gere ton profil et suis ta progression.",
                        avatarInitials = uiState.profileInitials,
                    )
                }

                item {
                    ProfileHeroCard(
                        name = uiState.profileName,
                        subtitle = uiState.profileSubtitle,
                        pointsLabel = "$points points",
                        levelLabel = "NIV $level",
                        xpLabel = "$levelXp / $nextLevelXp XP - prochain niveau ${level + 1}",
                        progress = levelProgress,
                        avatarInitials = uiState.profileInitials,
                    )
                }

                if (!uiState.profileErrorMessage.isNullOrBlank()) {
                    item {
                        NeonCard(tone = NeonTone.Warning) {
                            Text(
                                text = uiState.profileErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item {
                    NeonCard(tone = NeonTone.Blue) {
                        ProfileActionRow(
                            icon = Icons.Outlined.Edit,
                            title = "Modifier le profil",
                            subtitle = "Nom, pseudo et biographie.",
                        )
                        ProfileActionRow(
                            icon = Icons.Outlined.Image,
                            title = "Modifier la photo",
                            subtitle = "Choisis un nouvel avatar.",
                        )
                    }
                }

                item {
                    NeonCard(tone = NeonTone.Violet) {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            color = StarWhite,
                        )
                        SettingSwitchRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::setNotificationsEnabled,
                        )
                        SettingSwitchRow(
                            icon = Icons.Outlined.Palette,
                            title = "Couleurs dynamiques",
                            checked = uiState.useDynamicColors,
                            onCheckedChange = viewModel::setDynamicColors,
                        )
                    }
                }

                item {
                    StatsCard(
                        title = "Statistiques",
                        stats = stats,
                    )
                }

                item {
                    RewardsCard(
                        title = "Recompenses obtenues",
                        rewards = rewardItems,
                    )
                }

                item {
                    NeonCard(tone = NeonTone.Blue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QueryStats,
                                contentDescription = null,
                                tint = NeonCyan,
                            )
                            Column {
                                Text(
                                    text = "Version de l application",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StarWhite,
                                )
                                Text(
                                    text = uiState.appVersionLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                        }
                    }
                }

                if (uiState.isParentUser) {
                    item {
                        ParentPairingCard(
                            uiState = uiState,
                            pairingCodeInput = pairingCodeInput,
                            onPairingCodeInputChange = {
                                pairingCodeInput = it.uppercase().trim()
                                viewModel.clearPairingMessages()
                            },
                            onSelectFamily = {
                                viewModel.selectFamily(it)
                                viewModel.clearPairingMessages()
                            },
                            onAttachChild = { viewModel.attachChildWithCode(pairingCodeInput) },
                        )
                    }
                } else {
                    item {
                        ChildPairingCard(
                            uiState = uiState,
                            onFetchCode = viewModel::fetchOrGeneratePairingCode,
                        )
                    }
                }

                if (uiState.isParentUser) {
                    item {
                        NeonCard(tone = NeonTone.Cyan) {
                            Text(
                                text = "Mode parent",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Accede a la gestion des routines, missions et quetes d un enfant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            NeonButton(
                                text = "Ouvrir la gestion parent",
                                onClick = onOpenParentMode,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentPairingCard(
    uiState: SettingsUiState,
    pairingCodeInput: String,
    onPairingCodeInputChange: (String) -> Unit,
    onSelectFamily: (Long) -> Unit,
    onAttachChild: () -> Unit,
) {
    NeonCard(tone = NeonTone.Blue) {
        Text(
            text = "Associer un enfant",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        Text(
            text = "Saisis le code temporaire fourni par le compte enfant.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        if (uiState.familyIds.isNotEmpty()) {
            Text(
                text = "Famille",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.familyIds.forEach { familyId ->
                    FilterChip(
                        selected = uiState.selectedFamilyId == familyId,
                        onClick = { onSelectFamily(familyId) },
                        label = { Text("Famille #$familyId") },
                    )
                }
            }
        }

        OutlinedTextField(
            value = pairingCodeInput,
            onValueChange = onPairingCodeInputChange,
            label = { Text("Code enfant") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        NeonButton(
            text = if (uiState.isPairingBusy) "Association..." else "Associer",
            onClick = onAttachChild,
            enabled = !uiState.isPairingBusy,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.pairedChildren.isNotEmpty()) {
            Text(
                text = "Enfants associes: ${uiState.pairedChildren.size}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        if (!uiState.pairingSuccessMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingSuccessMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingErrorMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ChildPairingCard(
    uiState: SettingsUiState,
    onFetchCode: () -> Unit,
) {
    NeonCard(tone = NeonTone.Violet) {
        Text(
            text = "Code parent",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        Text(
            text = "Ce code est temporaire. Donne-le a ton parent pour associer ton compte.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        NeonButton(
            text = if (uiState.isPairingBusy) "Chargement..." else "Afficher mon code parent",
            onClick = onFetchCode,
            enabled = !uiState.isPairingBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.pairingCode?.let { code ->
            Text(
                text = code,
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingCodeExpiresAt.isNullOrBlank()) {
            Text(
                text = "Expire le: ${uiState.pairingCodeExpiresAt}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        if (!uiState.pairingSuccessMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingSuccessMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingErrorMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(ArcaneViolet.copy(alpha = 0.22f), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = StarWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = StarWhite,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun rewardEmojiForIndex(index: Int): String =
    when (index % 5) {
        0 -> "✨"
        1 -> "💎"
        2 -> "🎁"
        3 -> "🏅"
        else -> "👑"
    }
