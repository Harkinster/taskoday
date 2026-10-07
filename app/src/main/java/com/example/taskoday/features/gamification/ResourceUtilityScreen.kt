package com.example.taskoday.features.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResourceUtilityScreen(
    viewModel: ResourceUtilityViewModel,
    initialSection: String,
    onBackToNest: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var title by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("1") }
    var editingOfferId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.message, state.error) {
        val message = state.message ?: state.error
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onBackToNest) { Text("‹ Le Nid") }
                    OutlinedButton(onClick = onOpenProfile) { Text("Compte") }
                }
                Text(if (initialSection == "chests") "Coffres Chronodria" else "Caverne des souhaits", style = MaterialTheme.typography.headlineSmall)
                Text("Tes ressources restent personnelles.", style = MaterialTheme.typography.bodyMedium)
                val b = state.balance
                if (b != null) Text("${b.flames} Flammèches  ·  ${b.crystals} Cristaux", style = MaterialTheme.typography.titleMedium)
            }
            if (state.loading) item { Text("Chargement…", Modifier.padding(16.dp)) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

            if (initialSection != "chests") {
                if (state.isParent) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (editingOfferId == null) "Ajouter un souhait" else "Modifier le souhait", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(title, { title = it }, label = { Text("Souhait") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(cost, { cost = it.filter(Char::isDigit) }, label = { Text("Coût en Flammèches") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    val editId = editingOfferId
                                    if (editId == null) viewModel.createOffer(title, cost) else viewModel.updateOffer(editId, title, cost)
                                    title = ""; cost = "1"; editingOfferId = null
                                }, enabled = !state.submitting && title.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0) {
                                    Text(if (editingOfferId == null) "Créer l’offre" else "Enregistrer")
                                }
                            }
                        }
                    }
                }
                items(state.offers, key = { "offer-${it.id}" }) { offer ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(offer.title, style = MaterialTheme.typography.titleMedium)
                            offer.description?.takeIf(String::isNotBlank)?.let { Text(it) }
                            Text("${offer.flameCost} Flammèches")
                            if (state.isParent) Button(onClick = { viewModel.obtain(offer.id) }, enabled = !state.submitting && (state.balance?.flames ?: 0) >= offer.flameCost) { Text("Obtenir") }
                            else Button(onClick = { viewModel.requestWish(offer.id) }, enabled = !state.submitting) { Text("Demander") }
                            if (state.isParent) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editingOfferId = offer.id; title = offer.title; cost = offer.flameCost.toString() }, enabled = !state.submitting) { Text("Modifier") }
                                OutlinedButton(onClick = { viewModel.deactivateOffer(offer.id) }, enabled = !state.submitting) { Text("Désactiver") }
                            }
                        }
                    }
                }
                item { Text(if (state.isParent) "Demandes de la famille" else "Mes demandes", style = MaterialTheme.typography.titleLarge) }
                items(state.requests, key = { "request-${it.id}" }) { request ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(request.title, style = MaterialTheme.typography.titleMedium)
                            if (state.isParent) Text("Demandé par ${request.requesterName}")
                            Text("${request.flameCost} Flammèches · ${request.status}")
                            if (state.isParent && request.status == "PENDING") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.approve(request.id) }, enabled = !state.submitting && (state.balance?.flames ?: Int.MAX_VALUE) >= 0) { Text("Accepter") }
                                OutlinedButton(onClick = { viewModel.reject(request.id) }, enabled = !state.submitting) { Text("Refuser") }
                            }
                            if (!state.isParent && request.status == "PENDING") OutlinedButton(onClick = { viewModel.cancel(request.id) }, enabled = !state.submitting) { Text("Annuler la demande") }
                        }
                    }
                }
            } else {
                items(state.chests?.chests.orEmpty(), key = { it.chestType }) { chest ->
                    val enough = canOpenChest(state.balance?.crystals ?: 0, chest.crystalCost)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(chest.title, style = MaterialTheme.typography.titleLarge)
                            Text("${chest.crystalCost} Cristaux · ${chest.dropCount} découverte(s)")
                            Button(onClick = { viewModel.openChest(chest.chestType) }, enabled = !state.submitting && enough) {
                                Text(if (enough) "Ouvrir" else "Cristaux insuffisants")
                            }
                        }
                    }
                }
                if (state.chestOpens.isNotEmpty()) item { Text("Coffres ouverts récemment", style = MaterialTheme.typography.titleLarge) }
                items(state.chestOpens, key = { "open-${it.id}" }) { opened ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${opened.chestType.lowercase().replaceFirstChar(Char::uppercase)} · ${opened.crystalCost} Cristaux")
                            Text(chestRevealText(opened.drops))
                            OutlinedButton(onClick = { viewModel.showChestReveal(opened) }) { Text("Voir la découverte") }
                        }
                    }
                }
                item { Text("Ma collection", style = MaterialTheme.typography.titleLarge) }
                items(state.collection?.items.orEmpty(), key = { it.collectibleKey }) { item ->
                    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.title); Text("×${item.quantity}")
                    } }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 20.dp)) }
        }
    }
    state.chestReveal?.let { opened ->
        AlertDialog(
            onDismissRequest = viewModel::dismissChestReveal,
            title = { Text("Découverte du coffre ${opened.chestType.lowercase().replaceFirstChar(Char::uppercase)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    opened.drops.forEach { drop ->
                        val totalQuantity = state.collection?.items?.firstOrNull { it.title == drop.title }?.quantity
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(drop.title, style = MaterialTheme.typography.titleMedium)
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text("×${drop.quantity}", style = MaterialTheme.typography.titleMedium)
                                    totalQuantity?.let { Text("Collection : ×$it", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                    Text("Ta collection a été mise à jour.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissChestReveal) { Text("Continuer") }
            },
        )
    }
}
