package com.example.taskoday.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.small),
        )
        if (actionLabel != null && onActionClick != null) {
            Button(
                onClick = onActionClick,
                modifier = Modifier.padding(top = spacing.medium),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}
