package com.example.taskoday.features.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.taskoday.R
import com.example.taskoday.core.ui.component.fantasy.CircularProgressBadge
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.TaskodayBrand
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun SplashScreen(
    headline: String = "Taskoday",
    message: String = "Connexion au royaume...",
    showProgress: Boolean = true,
) {
    val spacing = MaterialTheme.spacing

    FantasyScreenBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(252.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressBadge(
                    progress = 0.82f,
                    modifier = Modifier.fillMaxSize(),
                    size = 252.dp,
                    strokeWidth = 12.dp,
                    centerText = "",
                )
                Image(
                    painter = painterResource(id = R.drawable.taskoday_screenbot_logo_icon),
                    contentDescription = "Screenbot",
                    modifier = Modifier.size(188.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            TaskodayBrand(compact = false)

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
                modifier = Modifier.padding(top = spacing.small),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = spacing.xSmall),
            )
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = spacing.large),
                    color = NeonCyan,
                    trackColor = ArcaneViolet.copy(alpha = 0.18f),
                )
            }
        }
    }
}
