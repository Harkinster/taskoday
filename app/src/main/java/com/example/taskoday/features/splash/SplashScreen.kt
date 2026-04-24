package com.example.taskoday.features.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.taskoday.core.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val spacing = MaterialTheme.spacing
    LaunchedEffect(Unit) {
        delay(700L)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Taskoday", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Construisez votre journée avec clarté",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.small),
        )
        CircularProgressIndicator(modifier = Modifier.padding(top = spacing.large))
    }
}
