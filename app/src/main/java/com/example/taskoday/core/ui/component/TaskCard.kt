package com.example.taskoday.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xSmall),
                )
            }
            Row(
                modifier = Modifier.padding(top = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                AssistChip(onClick = {}, label = { Text(text = task.status.label()) })
                AssistChip(onClick = {}, label = { Text(text = task.priority.label()) })
                AssistChip(onClick = {}, label = { Text(text = DateTimeUtils.formatDate(task.dueDate)) })
            }
        }
    }
}
