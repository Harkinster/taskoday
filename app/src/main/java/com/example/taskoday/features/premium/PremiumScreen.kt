package com.example.taskoday.features.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val spacing = MaterialTheme.spacing

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        FantasyScreenBackground(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayHeader(
                        title = "Premium Taskoday",
                        subtitle = "Plus de place pour organiser toute la famille.",
                        avatarInitials = "P",
                    )
                }

                item {
                    NeonCard(tone = NeonTone.Violet) {
                        Text(
                            text = "Le Premium sera disponible bientôt.",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                        )
                        Text(
                            text = "En attendant, la version gratuite reste utilisable avec des limites simples.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                }

                item {
                    NeonCard(tone = NeonTone.Cyan) {
                        Text(
                            text = "Ce que Premium préparera",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarWhite,
                        )
                        premiumBenefits.forEach { benefit ->
                            Text(
                                text = "• $benefit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }

                item {
                    NeonCard(tone = NeonTone.Blue) {
                        Text(
                            text = "Limites gratuites actuelles",
                            style = MaterialTheme.typography.titleMedium,
                            color = StarWhite,
                        )
                        freeLimits.forEach { limit ->
                            Text(
                                text = "• $limit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }

                item {
                    FantasyButton(
                        text = "J’ai compris",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        style = FantasyButtonStyle.Filled,
                    )
                }
            }
        }
    }
}

private val premiumBenefits =
    listOf(
        "plusieurs enfants",
        "plus de routines, missions et quêtes",
        "plus de souhaits",
        "historique avancé",
        "comptes enfants séparés plus tard",
        "synchronisation multi-appareils plus tard",
    )

private val freeLimits =
    listOf(
        "1 enfant",
        "5 routines actives",
        "3 missions actives",
        "1 quête active",
        "3 souhaits actifs",
    )
