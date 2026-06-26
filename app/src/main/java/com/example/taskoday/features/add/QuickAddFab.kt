package com.example.taskoday.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taskoday.core.ui.component.fantasy.FantasySkinAssets
import com.example.taskoday.core.ui.component.fantasy.FantasySkinSurface
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.ui.theme.taskodayGoldBrush

@Composable
fun QuickAddFab(
    uiState: QuickAddUiState,
    onRefresh: () -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateMission: () -> Unit,
    onCreateQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .size(46.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(taskodayGoldBrush())
                .clickable {
                    onRefresh()
                    showDialog = true
                }
                .border(1.5.dp, MagicViolet.copy(alpha = 0.78f), CircleShape)
                .testTag(TaskodayTestTags.TasksAddFab),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(MagicViolet, WoodBrownDark)))
                    .border(1.dp, SoftGold.copy(alpha = 0.86f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Ajouter",
                modifier = Modifier.size(23.dp),
                tint = SoftGold,
            )
        }
    }

    if (showDialog) {
        QuickAddSelectionDialog(
            uiState = uiState,
            onDismiss = { showDialog = false },
            onCreateRoutine = {
                showDialog = false
                onCreateRoutine()
            },
            onCreateMission = {
                showDialog = false
                onCreateMission()
            },
            onCreateQuest = {
                showDialog = false
                onCreateQuest()
            },
        )
    }
}

@Composable
private fun QuickAddSelectionDialog(
    uiState: QuickAddUiState,
    onDismiss: () -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateMission: () -> Unit,
    onCreateQuest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 14.dp, shape = shape, clip = false)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A0D24),
                                MagicViolet,
                                WoodBrownDark,
                            ),
                        ),
                    )
                    .border(1.6.dp, SoftGold.copy(alpha = 0.88f), shape)
                    .padding(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Text(
                    text = "Ajouter une action",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Choisis le type d'action a creer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentCream.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                QuickAddOption(
                    label = "Routine",
                    description = "Habitude reguliere a suivre.",
                    icon = Icons.Outlined.Repeat,
                    enabled = uiState.canCreateRoutine,
                    onClick = onCreateRoutine,
                )
                QuickAddOption(
                    label = "Mission",
                    description = "Objectif planifie par le parent.",
                    icon = Icons.Outlined.Flag,
                    enabled = uiState.canCreateMission,
                    onClick = onCreateMission,
                )
                QuickAddOption(
                    label = "Quête",
                    description = "Defi facultatif pour progresser.",
                    icon = Icons.Outlined.AutoAwesome,
                    enabled = uiState.canCreateQuest,
                    onClick = onCreateQuest,
                )
                if (uiState.hasRemoteSession && !uiState.isParent) {
                    Text(
                        text = "Missions et quêtes sont réservées au parent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentCream.copy(alpha = 0.82f),
                    )
                }
                QuickAddCloseButton(onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun QuickAddOption(
    label: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ParchmentLight.copy(alpha = if (enabled) 1f else 0.84f),
                            ParchmentCream.copy(alpha = if (enabled) 1f else 0.78f),
                        ),
                    ),
                )
                .clickable(enabled = enabled, onClick = onClick)
                .border(1.2.dp, if (enabled) SoftGold.copy(alpha = 0.90f) else MagicViolet.copy(alpha = 0.34f), shape)
                .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickAddIconFrame(icon = icon, enabled = enabled)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) WoodBrownDark else InkMuted,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) WoodBrownDark.copy(alpha = 0.78f) else InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickAddIconFrame(
    icon: ImageVector,
    enabled: Boolean,
) {
    val shape = RoundedCornerShape(12.dp)
    FantasySkinSurface(
        asset = FantasySkinAssets.iconFrameGold,
        fallbackBrush = taskodayGoldBrush(),
        modifier =
            Modifier
                .size(42.dp)
                .clip(shape)
                .border(1.dp, MagicViolet.copy(alpha = if (enabled) 0.64f else 0.30f), shape)
                .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(22.dp),
            tint = if (enabled) MagicViolet else InkMuted,
        )
    }
}

@Composable
private fun QuickAddCloseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        val shape = RoundedCornerShape(14.dp)
        FantasySkinSurface(
            asset = FantasySkinAssets.buttonPurple,
            fallbackBrush = Brush.verticalGradient(listOf(MagicViolet, WoodBrownDark)),
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 96.dp, minHeight = 38.dp)
                    .clip(shape)
                    .clickable(onClick = onClick)
                    .border(1.dp, SoftGold.copy(alpha = 0.76f), shape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Fermer",
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentCream,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
