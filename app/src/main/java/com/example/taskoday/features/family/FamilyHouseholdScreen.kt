package com.example.taskoday.features.family

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilyMemberRole

@Composable
fun FamilyHouseholdScreen(
    viewModel: FamilyHouseholdViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val groupedMembers = groupFamilyHouseholdMembers(uiState.members)
    val canManageMembers = uiState.isParentAccount && canManageFamilyMembers(uiState.members, uiState.currentUserId)
    val canLeave = uiState.isParentAccount && canLeaveFamily(uiState.members, uiState.currentUserId)
    val canArchive = uiState.isParentAccount && canArchiveFamily(uiState.members, uiState.currentUserId)
    var pendingRemoval by remember { mutableStateOf<FamilyMember?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    pendingRemoval?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Retirer ce membre ?") },
            text = {
                Text(
                    "${member.displayName} ne fera plus partie de ce foyer. Son compte restera disponible. " +
                        "Les autres familles ne seront pas modifiées.",
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.removeMember(member.userId); pendingRemoval = null }, enabled = !uiState.isMembershipBusy) {
                    Text("Retirer")
                }
            },
            dismissButton = { OutlinedButton(onClick = { pendingRemoval = null }) { Text("Annuler") } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Quitter ce foyer ?") },
            text = { Text("Tu quitteras seulement cette famille. Ton compte et tes autres familles resteront disponibles.") },
            confirmButton = {
                Button(onClick = { viewModel.leaveFamily(); confirmLeave = false }, enabled = !uiState.isMembershipBusy) {
                    Text("Quitter")
                }
            },
            dismissButton = { OutlinedButton(onClick = { confirmLeave = false }) { Text("Annuler") } },
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archiver cette famille ?") },
            text = { Text("Archiver cette famille la retirera de vos familles actives. Son historique sera conservé.") },
            confirmButton = {
                Button(onClick = { viewModel.archiveFamily(); confirmArchive = false }, enabled = !uiState.isMembershipBusy) {
                    Text("Archiver")
                }
            },
            dismissButton = { OutlinedButton(onClick = { confirmArchive = false }) { Text("Annuler") } },
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

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
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftGold)
                }
                return@FantasyScreenBackground
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    FamilyHouseholdHeader(onBack = onBack)
                }

                uiState.message?.takeIf { it.isNotBlank() }?.let { message ->
                    item {
                        FamilyHouseholdMessage(
                            message = message,
                            isError = false,
                            onDismiss = viewModel::clearMessages,
                        )
                    }
                }

                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        FamilyHouseholdMessage(
                            message = error,
                            isError = true,
                            onDismiss = viewModel::clearMessages,
                        )
                    }
                }

                if (uiState.families.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = ParchmentLight.copy(alpha = 0.97f)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mes familles", style = MaterialTheme.typography.titleMedium, color = InkBrown)
                                uiState.families.forEach { family ->
                                    FilterChip(
                                        selected = uiState.activeFamilyId == family.id,
                                        onClick = { viewModel.selectFamily(family.id) },
                                        label = { Text(family.name) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isParentAccount) {
                    item {
                        FamilyCreateCard(
                            name = uiState.createFamilyName,
                            isBusy = uiState.isCreateBusy,
                            onNameChange = viewModel::updateCreateFamilyName,
                            onCreate = viewModel::createFamily,
                        )
                    }
                }

                if (uiState.activeFamilyId != null) {
                    item {
                        FamilyMemberGroupCard(
                            title = "Parents",
                            members = groupedMembers.parents,
                            allMembers = uiState.members,
                            emptyMessage = "Aucun parent actif pour le moment.",
                            currentUserId = uiState.currentUserId,
                            canManage = canManageMembers,
                            onRemove = { pendingRemoval = it },
                        )
                    }
                    item {
                        FamilyMemberGroupCard(
                            title = "Enfants",
                            members = groupedMembers.children,
                            allMembers = uiState.members,
                            emptyMessage = "Aucun enfant actif dans ce foyer.",
                            currentUserId = uiState.currentUserId,
                            canManage = canManageMembers,
                            onRemove = { pendingRemoval = it },
                        )
                    }
                    if (groupedMembers.others.isNotEmpty()) {
                        item {
                            FamilyMemberGroupCard(
                                title = "Autres membres",
                                members = groupedMembers.others,
                                allMembers = uiState.members,
                                emptyMessage = "",
                                currentUserId = uiState.currentUserId,
                                canManage = canManageMembers,
                                onRemove = { pendingRemoval = it },
                            )
                        }
                    }
                    if (canManageMembers) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = ParchmentLight.copy(alpha = 0.97f)),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Quitter la famille", style = MaterialTheme.typography.titleMedium, color = InkBrown)
                                    if (!canLeave) {
                                        Text("Le dernier parent doit rester dans ce foyer.", color = InkMuted)
                                    }
                                    OutlinedButton(
                                        onClick = { confirmLeave = true },
                                        enabled = canLeave && !uiState.isMembershipBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Quitter ce foyer") }
                                }
                            }
                        }
                    }
                    if (canManageMembers) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = ParchmentLight.copy(alpha = 0.97f)),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Gestion de la famille", style = MaterialTheme.typography.titleMedium, color = InkBrown)
                                    if (!canArchive) {
                                        Text("Retirez ou faites quitter les autres membres avant d'archiver cette famille.", color = InkMuted)
                                    }
                                    OutlinedButton(
                                        onClick = { confirmArchive = true },
                                        enabled = canArchive && !uiState.isMembershipBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Archiver cette famille") }
                                }
                            }
                        }
                    }
                }

                if (canManageMembers) {
                    item {
                        FamilyInviteCard(
                        invite = uiState.invite,
                        isBusy = uiState.isInviteBusy,
                        onCreateInvite = viewModel::createInvite,
                        onCopy = { invite ->
                            clipboardManager.setText(AnnotatedString(invite.code))
                            viewModel.markInviteCopied()
                        },
                        onShare = { invite ->
                            val shareText =
                                "Rejoins mon foyer Taskoday avec ce code : ${invite.code}\n" +
                                    "Ce code est valable 48 heures et ne peut être utilisé qu'une fois."
                            val intent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                            context.startActivity(Intent.createChooser(intent, "Partager le code"))
                        },
                        )
                    }
                }

                if (uiState.isParentAccount) {
                    item {
                        FamilyJoinCard(
                            code = uiState.joinCode,
                            isBusy = uiState.isJoinBusy,
                            onCodeChange = viewModel::updateJoinCode,
                            onJoin = viewModel::acceptInvite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyCreateCard(
    name: String,
    isBusy: Boolean,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = ParchmentLight.copy(alpha = 0.97f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("+ Créer une famille", style = MaterialTheme.typography.titleMedium, color = InkBrown)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nom de la famille") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onCreate, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (isBusy) "Création..." else "Créer la famille")
            }
        }
    }
}

@Composable
private fun FamilyHouseholdHeader(onBack: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.98f),
                contentColor = InkBrown,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour", tint = WoodBrown)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mon foyer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = "Parents, enfants et partage du foyer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            Icon(Icons.Outlined.Home, contentDescription = null, tint = WoodBrown, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FamilyMemberGroupCard(
    title: String,
    members: List<FamilyMember>,
    allMembers: List<FamilyMember>,
    emptyMessage: String,
    currentUserId: Long?,
    canManage: Boolean,
    onRemove: (FamilyMember) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.97f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            if (members.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            } else {
                members.forEach { member ->
                    FamilyMemberRow(
                        member = member,
                        canRemove = canManage && canRemoveFamilyMember(allMembers, currentUserId, member.userId),
                        onRemove = { onRemove(member) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberRow(member: FamilyMember, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = WoodBrown,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            familyMemberSecondaryLabel(member)?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text =
                when (member.role) {
                    FamilyMemberRole.PARENT -> "Parent"
                    FamilyMemberRole.CHILD -> "Enfant"
                    FamilyMemberRole.OTHER -> "Membre"
                },
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
            if (canRemove) {
                OutlinedButton(onClick = onRemove) { Text("Retirer") }
            }
        }
    }
}

@Composable
private fun FamilyInviteCard(
    invite: FamilyInvite?,
    isBusy: Boolean,
    onCreateInvite: () -> Unit,
    onCopy: (FamilyInvite) -> Unit,
    onShare: (FamilyInvite) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.97f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ajouter un parent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = "Créez un code famille à partager avec un parent.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            Button(
                onClick = onCreateInvite,
                enabled = !isBusy,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isBusy) "Préparation..." else "Créer un code famille")
            }

            invite?.let { currentInvite ->
                Text(
                    text = currentInvite.code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = familyInviteExpirationLabel(currentInvite.expiresAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Text(
                    text = "Ce code est valable 48 heures et ne peut être utilisé qu'une fois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onCopy(currentInvite) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copier")
                    }
                    OutlinedButton(
                        onClick = { onShare(currentInvite) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager")
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyJoinCard(
    code: String,
    isBusy: Boolean,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.97f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Rejoindre une famille",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = "Demandez le code famille à un parent.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("Code famille") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onJoin,
                enabled = !isBusy,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isBusy) "Connexion..." else "Rejoindre ma famille")
            }
        }
    }
}

@Composable
private fun FamilyHouseholdMessage(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.98f),
                contentColor = if (isError) DangerGlow else MossGreen,
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) DangerGlow else MossGreen,
            )
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Fermer")
            }
        }
    }
}
