package com.example.taskoday.features.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(onBack: () -> Unit) {
    val spacing = MaterialTheme.spacing
    val days = DateTimeUtils.weekDayLabels()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Semaine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            item {
                Text(
                    text = "Vue calendrier (placeholder)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Les blocs de planification sont prêts à être reliés aux données réelles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xSmall),
                )
            }
            items(days) { day ->
                Card {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(text = day, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Aucun événement lié pour le moment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.xSmall),
                        )
                    }
                }
            }
        }
    }
}
