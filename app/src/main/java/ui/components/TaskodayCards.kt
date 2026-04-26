package com.taskoday.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.taskoday.ui.theme.TaskodayColors

@Composable
fun ProgressHeroCard(
    title: String,
    current: Int,
    total: Int,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    NeonCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .neonGlow(TaskodayColors.Cyan.copy(alpha = 0.55f), 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = TaskodayColors.TextPrimary, style = MaterialTheme.typography.displayLarge)
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$current", color = TaskodayColors.Cyan, style = MaterialTheme.typography.headlineLarge)
                    Text(" / $total complétées", style = MaterialTheme.typography.titleMedium)
                }
                XpProgressBar(progress = current.toFloat() / total.toFloat(), modifier = Modifier.padding(vertical = 10.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.neonGlow(TaskodayColors.Cyan.copy(alpha = 0.35f), 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TaskodayColors.PanelAlt,
            contentColor = TaskodayColors.TextPrimary
        ),
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(listOf(TaskodayColors.Cyan, TaskodayColors.NeonPurple))
        )
    ) {
        Text(text)
    }
}
