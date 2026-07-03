package com.example.taskoday.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.plan.TaskodayPlanFeature
import com.example.taskoday.core.plan.TaskodayPlanPolicy
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.PARENT_PIN_LENGTH
import com.example.taskoday.core.ui.component.fantasy.ParentPinDialog
import com.example.taskoday.core.ui.component.fantasy.ParentPinStatusMessage
import com.example.taskoday.core.ui.component.fantasy.ProfileHeroCard
import com.example.taskoday.core.ui.component.fantasy.RewardsCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.component.fantasy.TaskodayRewardItem
import com.example.taskoday.core.ui.component.fantasy.TaskodayStatItem
import com.example.taskoday.core.ui.component.fantasy.StatsCard
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isLocalChildMode: Boolean = false,
    onOpenParentMode: () -> Unit = {},
    onEnterLocalChildMode: () -> Unit = {},
    onExitLocalChildMode: () -> Unit = {},
    onLogoutConfirmed: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var pairingCodeInput by rememberSaveable { mutableStateOf("") }
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }
    var showReturnParentPinDialog by rememberSaveable { mutableStateOf(false) }
    var returnParentPinError by rememberSaveable { mutableStateOf<String?>(null) }

    val xp = uiState.totalXp
    val level = uiState.level
    val nextLevelXp = uiState.nextLevelXp
    val levelXp = uiState.levelXp
    val levelProgress =
        if (nextLevelXp == 0) {
            0f
        } else {
            levelXp.toFloat() / nextLevelXp.toFloat()
        }

    val stats =
        listOf(
            TaskodayStatItem(label = "Missions", value = uiState.missionsStat),
            TaskodayStatItem(label = "Quêtes", value = uiState.questsStat),
            TaskodayStatItem(label = "Serie", value = uiState.streakStat),
            TaskodayStatItem(label = "Succès", value = uiState.successStat),
        )

    val rewardItems =
        uiState.xpHistoryTokens
            .take(4)
            .mapIndexed { index, token ->
                TaskodayRewardItem(
                    label = "XP ${index + 1}",
                    value = token,
                    emoji = rewardEmojiForIndex(index),
                )
            }.ifEmpty {
                listOf(
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "✨"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "⭐"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "🔥"),
                    TaskodayRewardItem(label = "XP", value = "--", emoji = "🏆"),
                )
            }

    Scaffold(
        containerColor = Color.Transparent,
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
                        title = "Profil",
                        subtitle = "Gérez votre famille et le mode enfant.",
                        avatarInitials = uiState.profileInitials,
                    )
                }

                item {
                    ProfileHeroCard(
                        name = uiState.profileName,
                        subtitle = uiState.profileSubtitle,
                        pointsLabel = "$xp XP",
                        levelLabel = "NIV $level",
                        xpLabel = "$levelXp / $nextLevelXp XP - prochain niveau ${level + 1}",
                        progress = levelProgress,
                        avatarInitials = uiState.profileInitials,
                    )
                }

                if (isLocalChildMode) {
                    item {
                        LocalChildModeSettingsCard(
                            onReturnParent = {
                                returnParentPinError = null
                                showReturnParentPinDialog = true
                            },
                        )
                    }
                }

                if (!uiState.isParentUser && !isLocalChildMode) {
                    item {
                        LogoutActionCard(onClick = { showLogoutConfirmation = true })
                    }
                }

                if (uiState.isParentUser && !isLocalChildMode) {
                    item {
                        ActiveChildCard(
                            uiState = uiState,
                            onSelectChild = viewModel::selectActiveChild,
                            onCreateChild = viewModel::createChild,
                            onRenameChild = viewModel::renameChild,
                            onClearMessages = viewModel::clearChildManagementMessages,
                            onEnterLocalChildMode = onEnterLocalChildMode,
                            hasParentPin = uiState.hasParentPin,
                            onOpenPremium = onOpenPremium,
                        )
                    }

                    item {
                        ParentPinSettingsCard(
                            hasParentPin = uiState.hasParentPin,
                            successMessage = uiState.parentPinSuccessMessage,
                            errorMessage = uiState.parentPinErrorMessage,
                            onSavePin = viewModel::saveParentPin,
                            onClearMessages = viewModel::clearParentPinMessages,
                        )
                    }
                }

                if (!uiState.profileErrorMessage.isNullOrBlank()) {
                    item {
                        NeonCard(tone = NeonTone.Warning) {
                            Text(
                                text = uiState.profileErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                if (!isLocalChildMode) {
                    item {
                        NeonCard(tone = NeonTone.Blue) {
                            ProfileActionRow(
                                icon = Icons.Outlined.Edit,
                                title = "Modifier le profil",
                                subtitle = "Nom, pseudo et biographie.",
                            )
                            ProfileActionRow(
                                icon = Icons.Outlined.Image,
                                title = "Modifier la photo",
                                subtitle = "Choisis un nouvel avatar.",
                            )
                        }
                    }
                }

                if (uiState.isParentUser && !isLocalChildMode) {
                    item {
                        LogoutActionCard(onClick = { showLogoutConfirmation = true })
                    }
                }

                if (!isLocalChildMode) {
                    item {
                        NeonCard(tone = NeonTone.Violet) {
                            Text(
                                text = "Préférences",
                                style = MaterialTheme.typography.titleLarge,
                                color = StarWhite,
                            )
                            SettingSwitchRow(
                                icon = Icons.Outlined.Notifications,
                                title = "Notifications",
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = viewModel::setNotificationsEnabled,
                            )
                            SettingSwitchRow(
                                icon = Icons.Outlined.Palette,
                                title = "Couleurs dynamiques",
                                checked = uiState.useDynamicColors,
                                onCheckedChange = viewModel::setDynamicColors,
                            )
                        }
                    }
                }

                if (uiState.isParentUser && !isLocalChildMode) {
                    item {
                        NeonCard(tone = NeonTone.Cyan) {
                            Text(
                                text = "Premium Taskoday",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Voir les limites gratuites et ce que Premium préparera.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            NeonButton(
                                text = "Voir Premium",
                                onClick = onOpenPremium,
                                modifier = Modifier.fillMaxWidth(),
                                style = NeonButtonStyle.Outline,
                            )
                        }
                    }
                }

                item {
                    StatsCard(
                        title = "Statistiques",
                        stats = stats,
                    )
                }

                item {
                    RewardsCard(
                        title = "Souhaits validés",
                        rewards = rewardItems,
                    )
                }

                item {
                    NeonCard(tone = NeonTone.Blue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QueryStats,
                                contentDescription = null,
                                tint = NeonCyan,
                            )
                            Column {
                                Text(
                                    text = "Version de l’application",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StarWhite,
                                )
                                Text(
                                    text = uiState.appVersionLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                        }
                    }
                }

                if (uiState.isParentUser && !isLocalChildMode) {
                    item {
                        ParentPairingCard(
                            uiState = uiState,
                            pairingCodeInput = pairingCodeInput,
                            onPairingCodeInputChange = {
                                pairingCodeInput = it.uppercase().trim()
                                viewModel.clearPairingMessages()
                            },
                            onSelectFamily = {
                                viewModel.selectFamily(it)
                                viewModel.clearPairingMessages()
                            },
                            onAttachChild = { viewModel.attachChildWithCode(pairingCodeInput) },
                        )
                    }
                } else if (!isLocalChildMode) {
                    item {
                        ChildPairingCard(
                            uiState = uiState,
                            onFetchCode = viewModel::fetchOrGeneratePairingCode,
                        )
                    }
                }

                if (uiState.isParentUser && !isLocalChildMode) {
                    item {
                        NeonCard(tone = NeonTone.Cyan) {
                            Text(
                                text = "Mode parent",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Accède à la gestion des routines, missions et quêtes d'un enfant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            NeonButton(
                                text = "Ouvrir la gestion parent",
                                onClick = onOpenParentMode,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirmation) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutConfirmation = false },
            onConfirm = {
                showLogoutConfirmation = false
                onLogoutConfirmed()
            },
        )
    }

    if (showReturnParentPinDialog) {
        ParentPinDialog(
            title = "Retour parent",
            message = "Saisis le PIN parent pour revenir au tableau de bord.",
            errorMessage = returnParentPinError,
            onDismiss = {
                showReturnParentPinDialog = false
                returnParentPinError = null
            },
            onConfirm = { pin ->
                if (viewModel.verifyParentPin(pin)) {
                    showReturnParentPinDialog = false
                    returnParentPinError = null
                    onExitLocalChildMode()
                } else {
                    returnParentPinError = "PIN incorrect."
                }
            },
        )
    }
}

@Composable
private fun LogoutActionCard(onClick: () -> Unit) {
    NeonCard(tone = NeonTone.Danger) {
        ProfileActionRow(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Déconnexion",
            subtitle = "Quitter ce compte ou changer de compte.",
            modifier = Modifier.testTag(TaskodayTestTags.ProfileLogoutAction),
            onClick = onClick,
        )
    }
}

@Composable
private fun LocalChildModeSettingsCard(onReturnParent: () -> Unit) {
    NeonCard(tone = NeonTone.Cyan) {
        Text(
            text = "Mode enfant local",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        Text(
            text = "Les options parent sont masquees sur cet appareil.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        NeonButton(
            text = "Retour parent",
            onClick = onReturnParent,
            style = NeonButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParentPinSettingsCard(
    hasParentPin: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onSavePin: (String) -> Unit,
    onClearMessages: () -> Unit,
) {
    var showPinDialog by rememberSaveable { mutableStateOf(false) }

    NeonCard(tone = if (hasParentPin) NeonTone.Cyan else NeonTone.Warning) {
        Text(
            text = "PIN parent local",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        ParentPinStatusMessage(hasParentPin = hasParentPin)
        Text(
            text = "Il protège le bouton Retour parent quand l'app est en mode enfant.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        NeonButton(
            text = if (hasParentPin) "Modifier le PIN" else "Définir le PIN parent",
            onClick = {
                onClearMessages()
                showPinDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            style = if (hasParentPin) NeonButtonStyle.Outline else NeonButtonStyle.Filled,
        )
        if (!successMessage.isNullOrBlank()) {
            Text(
                text = successMessage,
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showPinDialog) {
        SetParentPinDialog(
            hasParentPin = hasParentPin,
            onDismiss = { showPinDialog = false },
            onSavePin = { pin ->
                onSavePin(pin)
                showPinDialog = false
            },
        )
    }
}

@Composable
private fun SetParentPinDialog(
    hasParentPin: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val pinsMatch = pin.length == PARENT_PIN_LENGTH && pin == confirmation

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            tone = NeonTone.Cyan,
        ) {
            Text(
                text = if (hasParentPin) "Modifier le PIN" else "Définir le PIN parent",
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = "Choisis un code à 4 chiffres pour protéger le retour parent.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            ParentPinTextField(
                value = pin,
                onValueChange = { pin = it },
                label = "Nouveau PIN",
            )
            ParentPinTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirmer",
            )
            if (confirmation.isNotEmpty() && pin != confirmation) {
                Text(
                    text = "Les deux PIN ne correspondent pas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeonButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = "Enregistrer",
                    onClick = { onSavePin(pin) },
                    enabled = pinsMatch,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ParentPinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(PARENT_PIN_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActiveChildCard(
    uiState: SettingsUiState,
    onSelectChild: (Long) -> Unit,
    onCreateChild: (String, String?, String?) -> Unit,
    onRenameChild: (Long, String) -> Unit,
    onClearMessages: () -> Unit,
    onEnterLocalChildMode: () -> Unit,
    hasParentPin: Boolean,
    onOpenPremium: () -> Unit,
) {
    val activeChild = uiState.pairedChildren.firstOrNull { child -> child.id == uiState.activeChildId }
    val childLimitReached = !TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Child, uiState.pairedChildren.size)
    var showCreateChildDialog by rememberSaveable { mutableStateOf(false) }
    var editingChildId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingChildName by rememberSaveable { mutableStateOf("") }
    val editingChild = uiState.pairedChildren.firstOrNull { child -> child.id == editingChildId }

    NeonCard(tone = if (uiState.pairedChildren.isEmpty()) NeonTone.Warning else NeonTone.Cyan) {
        if (uiState.pairedChildren.isEmpty()) {
            Text(
                text = "Aucun enfant pour le moment",
                style = MaterialTheme.typography.titleMedium,
                color = StarWhite,
            )
            Text(
                text = "Créez votre premier enfant pour commencer à préparer ses routines.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            Text(
                text = "Enfant actif",
                style = MaterialTheme.typography.titleMedium,
                color = StarWhite,
            )
            Text(
            text = activeChild?.displayName ?: "Sélectionne un enfant",
                style = MaterialTheme.typography.bodyMedium,
                color = NeonCyan,
            )
            Text(
                text = "Les actions, souhaits et le Nid se rechargent avec cet enfant.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.pairedChildren.forEach { child ->
                    ChildManagementRow(
                        displayName = child.displayName,
                        email = child.email,
                        isActive = child.id == uiState.activeChildId,
                        isBusy = uiState.isChildManagementBusy,
                        onSelect = { onSelectChild(child.id) },
                        onEdit = {
                            editingChildId = child.id
                            editingChildName = child.displayName
                            onClearMessages()
                        },
                    )
                }
            }
        }

        Text(
            text = TaskodayPlanPolicy.usageLabel(TaskodayPlanFeature.Child, uiState.pairedChildren.size),
            style = MaterialTheme.typography.bodySmall,
            color = if (childLimitReached) MaterialTheme.colorScheme.error else TextMuted,
        )
        if (childLimitReached) {
            Text(
                text = TaskodayPlanPolicy.limitReachedMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            NeonButton(
                text = "Voir Premium",
                onClick = onOpenPremium,
                modifier = Modifier.fillMaxWidth(),
                style = NeonButtonStyle.Outline,
            )
        }

        NeonButton(
            text =
                if (uiState.isChildManagementBusy) {
                    "Ajout..."
                } else if (uiState.pairedChildren.isEmpty()) {
                    "Créer mon premier enfant"
                } else {
                    "Ajouter un enfant"
                },
            onClick = {
                onClearMessages()
                showCreateChildDialog = true
            },
            enabled = !uiState.isChildManagementBusy && !childLimitReached,
            modifier = Modifier.fillMaxWidth(),
        )

        NeonButton(
            text = "Passer en mode enfant",
            onClick = onEnterLocalChildMode,
            enabled = !uiState.isChildManagementBusy && activeChild != null && hasParentPin,
            style = NeonButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!hasParentPin) {
            Text(
                text = "Définissez un PIN parent pour activer le mode enfant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (!uiState.childManagementSuccessMessage.isNullOrBlank()) {
            Text(
                text = uiState.childManagementSuccessMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!uiState.childManagementErrorMessage.isNullOrBlank()) {
            Text(
                text = uiState.childManagementErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showCreateChildDialog) {
        CreateChildDialog(
            isBusy = uiState.isChildManagementBusy,
            onDismiss = {
                if (!uiState.isChildManagementBusy) {
                    showCreateChildDialog = false
                }
            },
            onConfirm = { displayName, email, birthDate ->
                onCreateChild(displayName, email, birthDate)
                showCreateChildDialog = false
            },
            onClearMessages = onClearMessages,
        )
    }

    editingChild?.let { child ->
        Dialog(
            onDismissRequest = {
                if (!uiState.isChildManagementBusy) {
                    editingChildId = null
                    editingChildName = ""
                }
            },
        ) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                tone = NeonTone.Cyan,
            ) {
                Text(
                    text = "Modifier l’enfant",
                    style = MaterialTheme.typography.titleLarge,
                    color = StarWhite,
                )
                Text(
                    text = child.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                OutlinedTextField(
                    value = editingChildName,
                    onValueChange = {
                        editingChildName = it
                        onClearMessages()
                    },
                    label = { Text("Nom affiché") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeonButton(
                        text = "Annuler",
                        onClick = {
                            editingChildId = null
                            editingChildName = ""
                        },
                        enabled = !uiState.isChildManagementBusy,
                        modifier = Modifier.weight(1f),
                        style = NeonButtonStyle.Outline,
                    )
                    NeonButton(
                        text = if (uiState.isChildManagementBusy) "Enregistrement…" else "Enregistrer",
                        onClick = {
                            onRenameChild(child.id, editingChildName)
                            editingChildId = null
                            editingChildName = ""
                        },
                        enabled = !uiState.isChildManagementBusy && editingChildName.trim().isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateChildDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit,
    onClearMessages: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            tone = NeonTone.Cyan,
        ) {
            Text(
                text = "Ajouter un enfant",
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = "Le compte sera lié directement à ce parent.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    onClearMessages()
                },
                label = { Text("Prénom / nom affiché") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onClearMessages()
                },
                label = { Text("Email optionnel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = birthDate,
                onValueChange = {
                    birthDate = it
                    onClearMessages()
                },
                label = { Text("Date de naissance optionnelle") },
                placeholder = { Text("AAAA-MM-JJ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeonButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = if (isBusy) "Création…" else "Créer",
                    onClick = {
                        onConfirm(
                            displayName,
                            email.trim().takeIf { it.isNotBlank() },
                            birthDate.trim().takeIf { it.isNotBlank() },
                        )
                    },
                    enabled = !isBusy && displayName.trim().isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChildManagementRow(
    displayName: String,
    email: String,
    isActive: Boolean,
    isBusy: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(ArcaneViolet.copy(alpha = if (isActive) 0.28f else 0.14f), RoundedCornerShape(12.dp))
                .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                color = if (isActive) NeonCyan else StarWhite,
                maxLines = 1,
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
            )
        }
        NeonButton(
            text = if (isActive) "Actif" else "Choisir",
            onClick = onSelect,
            enabled = !isActive && !isBusy,
            modifier = Modifier.weight(0.82f),
            style = if (isActive) NeonButtonStyle.Filled else NeonButtonStyle.Outline,
        )
        NeonButton(
            text = "Modifier",
            onClick = onEdit,
            enabled = !isBusy,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.Outline,
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            tone = NeonTone.Danger,
        ) {
            Text(
                text = "Déconnexion",
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = "Se déconnecter de ce compte ?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeonButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = "Se déconnecter",
                    onClick = onConfirm,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag(TaskodayTestTags.ProfileLogoutConfirm),
                )
            }
        }
    }
}

@Composable
private fun ParentPairingCard(
    uiState: SettingsUiState,
    pairingCodeInput: String,
    onPairingCodeInputChange: (String) -> Unit,
    onSelectFamily: (Long) -> Unit,
    onAttachChild: () -> Unit,
) {
    NeonCard(tone = NeonTone.Blue) {
        Text(
            text = "Associer par code",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        Text(
            text = "Option secondaire : saisis le code temporaire fourni par un compte enfant existant.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        if (uiState.familyIds.isNotEmpty()) {
            Text(
                text = "Famille",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.familyIds.forEach { familyId ->
                    FilterChip(
                        selected = uiState.selectedFamilyId == familyId,
                        onClick = { onSelectFamily(familyId) },
                        label = { Text("Famille #$familyId") },
                    )
                }
            }
        }

        OutlinedTextField(
            value = pairingCodeInput,
            onValueChange = onPairingCodeInputChange,
            label = { Text("Code enfant") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        NeonButton(
            text = if (uiState.isPairingBusy) "Association..." else "Associer",
            onClick = onAttachChild,
            enabled = !uiState.isPairingBusy,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.pairedChildren.isNotEmpty()) {
            Text(
                text = "Enfants associés: ${uiState.pairedChildren.size}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        if (!uiState.pairingSuccessMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingSuccessMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingErrorMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ChildPairingCard(
    uiState: SettingsUiState,
    onFetchCode: () -> Unit,
) {
    NeonCard(tone = NeonTone.Violet) {
        Text(
            text = "Code parent",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        Text(
            text = "Ce code est temporaire. Donne-le à ton parent pour associer ton compte.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        NeonButton(
            text = if (uiState.isPairingBusy) "Chargement..." else "Afficher mon code parent",
            onClick = onFetchCode,
            enabled = !uiState.isPairingBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.pairingCode?.let { code ->
            Text(
                text = code,
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingCodeExpiresAt.isNullOrBlank()) {
            Text(
                text = "Expire le : ${uiState.pairingCodeExpiresAt}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        if (!uiState.pairingSuccessMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingSuccessMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
            )
        }
        if (!uiState.pairingErrorMessage.isNullOrBlank()) {
            Text(
                text = uiState.pairingErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(ArcaneViolet.copy(alpha = 0.22f), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = StarWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = StarWhite,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun rewardEmojiForIndex(index: Int): String =
    when (index % 5) {
        0 -> "✨"
        1 -> "💎"
        2 -> "🎁"
        3 -> "🏅"
        else -> "👑"
    }
