package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun FantasyConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            tone = NeonTone.Danger,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NeonButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = confirmEnabled,
                )
            }
        }
    }
}
