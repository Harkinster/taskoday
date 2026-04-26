package com.example.taskoday.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.Task

@Composable
fun TaskCard(
    task: Task,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                            .padding(horizontal = spacing.small, vertical = spacing.xSmall),
                ) {
                    Text(text = task.emoji, style = MaterialTheme.typography.titleMedium)
                }
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            }

            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                AssistChip(onClick = {}, label = { Text(text = task.status.label()) })
                AssistChip(onClick = {}, label = { Text(text = "${task.dayPart.emoji()} ${task.dayPart.label()}") })
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = if (task.isDaily) "Routine" else "Mission: ${DateTimeUtils.formatDate(task.scheduledDate)}")
                    },
                )
            }
        }
    }
}
