package com.example.taskoday.features.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.PlaceholderScreen
import com.example.taskoday.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    Scaffold(topBar = { TopAppBar(title = { Text(text = "Projets") }) }) { innerPadding ->
        if (uiState.projects.isEmpty() && !uiState.isLoading) {
            PlaceholderScreen(
                title = "Aucun projet",
                description = "La base projet est prête. Vous pouvez ajouter la création ensuite.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            items(uiState.projects, key = { project -> project.id }) { project ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Couleur : ${project.color} | Icône : ${project.icon}",
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
