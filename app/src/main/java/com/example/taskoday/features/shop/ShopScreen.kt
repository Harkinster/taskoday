package com.example.taskoday.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: ShopViewModel,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Caverne aux Souhaits") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profil",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
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
            item {
                BalanceCard(
                    scalesBalance = uiState.scalesBalance,
                    isParent = uiState.isParent,
                    hasRemoteSession = uiState.hasRemoteSession,
                )
            }

            if (uiState.isParent && uiState.hasRemoteSession) {
                item {
                    ParentRewardCreator(
                        isSubmitting = uiState.isSubmitting,
                        onCreate = viewModel::createReward,
                    )
                }
            }

            item {
                Text(text = "Souhaits", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.rewards.isEmpty()) {
                item {
                    EmptyCard(text = "Aucun Souhait disponible.")
                }
            } else {
                items(uiState.rewards, key = { reward -> reward.id }) { reward ->
                    RewardRow(
                        reward = reward,
                        isParent = uiState.isParent,
                        canRequest = uiState.hasRemoteSession && !uiState.isParent && uiState.scalesBalance >= reward.cost,
                        isSubmitting = uiState.isSubmitting,
                        onRequest = { viewModel.requestReward(reward) },
                    )
                }
            }

            item {
                Text(text = "Demandes et Parchemins", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.requests.isEmpty()) {
                item {
                    if (uiState.hasRemoteSession) {
                        EmptyCard(text = "Aucune demande.")
                    } else {
                        LocalHistory(transactions = uiState.localTransactions)
                    }
                }
            } else {
                items(uiState.requests, key = { request -> request.id }) { request ->
                    RewardRequestRow(
                        request = request,
                        isParent = uiState.isParent,
                        isSubmitting = uiState.isSubmitting,
                        onApprove = { viewModel.approveRequest(request.id) },
                        onRefuse = { viewModel.refuseRequest(request.id) },
                        onUseCoupon = { couponId -> viewModel.useCoupon(couponId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    scalesBalance: Int,
    isParent: Boolean,
    hasRemoteSession: Boolean,
) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = if (isParent) "Souhaits parent" else "Solde", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (hasRemoteSession) "Synchronise" else "Mode local",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xSmall), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Star, contentDescription = null)
                Text(text = "$scalesBalance Flammèches", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ParentRewardCreator(
    isSubmitting: Boolean,
    onCreate: (String, String?, Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var costText by rememberSaveable { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(text = "Nouveau Souhait", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Coût en Flammèches") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onCreate(title, description, costText.toIntOrNull() ?: 0)
                    title = ""
                    description = ""
                    costText = ""
                },
                enabled = !isSubmitting && title.isNotBlank() && (costText.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Creer")
            }
        }
    }
}

@Composable
private fun RewardRow(
    reward: Reward,
    isParent: Boolean,
    canRequest: Boolean,
    isSubmitting: Boolean,
    onRequest: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
            ) {
                Text(text = "${reward.emoji} ${reward.title}", style = MaterialTheme.typography.titleMedium)
                reward.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = "Coût : ${reward.cost} Flammèches", style = MaterialTheme.typography.labelLarge)
            }

            if (!isParent) {
                Button(onClick = onRequest, enabled = canRequest && !isSubmitting) {
                    Text(text = "Demander")
                }
            }
        }
    }
}

@Composable
private fun RewardRequestRow(
    request: RewardRedemptionRequest,
    isParent: Boolean,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onRefuse: () -> Unit,
    onUseCoupon: (Long) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = request.rewardTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = request.status.label, style = MaterialTheme.typography.labelLarge)
            }
            Text(text = "${request.costScales} Flammèches", style = MaterialTheme.typography.bodyMedium)
            request.coupon?.let { coupon ->
                Text(text = "Parchemin: ${coupon.code}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Statut: ${coupon.status}", style = MaterialTheme.typography.bodySmall)
            }

            when {
                isParent && request.status == RewardRequestStatus.PENDING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        Button(onClick = onApprove, enabled = !isSubmitting) {
                            Text("Accepter")
                        }
                        TextButton(onClick = onRefuse, enabled = !isSubmitting) {
                            Text("Refuser")
                        }
                    }
                }

                isParent && request.status == RewardRequestStatus.APPROVED && request.coupon != null -> {
                    Button(onClick = { onUseCoupon(request.coupon.id) }, enabled = !isSubmitting) {
                        Text("Marquer utilise")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocalHistory(transactions: List<PointsTransaction>) {
    if (transactions.isEmpty()) {
        EmptyCard(text = "Aucune demande.")
        return
    }

    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        transactions.forEach { transaction ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = transaction.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "${transaction.amount} Flammèches")
                }
            }
        }
    }
}
