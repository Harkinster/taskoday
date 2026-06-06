package com.example.taskoday.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.component.fantasy.TaskodayWorldBackground
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun SplashScreen(
    headline: String = "Taskoday",
    message: String = "Connexion au royaume...",
    showProgress: Boolean = true,
) {
    val spacing = MaterialTheme.spacing
    val optionalHeadline = headline.takeIf { it.isNotBlank() && it != "Taskoday" }

    TaskodayWorldBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xDD2B1738),
                                    Color(0xEE170B26),
                                ),
                            ),
                        )
                        .border(1.4.dp, SoftGold.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if (optionalHeadline != null) {
                    Text(
                        text = optionalHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftGold,
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentLight,
                )
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = spacing.xSmall),
                        color = NeonCyan,
                        trackColor = ArcaneViolet.copy(alpha = 0.28f),
                    )
                }
            }
        }
    }
}
