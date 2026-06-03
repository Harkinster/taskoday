package com.example.taskoday.features.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing

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

    FloatingActionButton(
        onClick = {
            onRefresh()
            showDialog = true
        },
        modifier = modifier.testTag(TaskodayTestTags.TasksAddFab),
        containerColor = EmberOrange,
        contentColor = ParchmentLight,
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = "Ajouter")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Ajouter") },
            containerColor = ParchmentLight,
            titleContentColor = WoodBrownDark,
            textContentColor = InkMuted,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    QuickAddOption(
                        label = "Routine",
                        icon = Icons.Outlined.Repeat,
                        enabled = uiState.canCreateRoutine,
                        onClick = {
                            showDialog = false
                            onCreateRoutine()
                        },
                    )
                    QuickAddOption(
                        label = "Mission",
                        icon = Icons.Outlined.Flag,
                        enabled = uiState.canCreateMission,
                        onClick = {
                            showDialog = false
                            onCreateMission()
                        },
                    )
                    QuickAddOption(
                        label = "Quête",
                        icon = Icons.Outlined.AutoAwesome,
                        enabled = uiState.canCreateQuest,
                        onClick = {
                            showDialog = false
                            onCreateQuest()
                        },
                    )
                    if (uiState.hasRemoteSession && !uiState.isParent) {
                        Text(
                            text = "Missions et quêtes sont réservées au parent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmberOrange),
                ) {
                    Text(text = "Fermer")
                }
            },
        )
    }
}

@Composable
private fun QuickAddOption(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) SoftGold.copy(alpha = 0.9f) else MagicViolet.copy(alpha = 0.35f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = WoodBrownDark,
                disabledContentColor = InkMuted,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = label)
        }
    }
}
