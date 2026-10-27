package com.example.taskoday.core.ui.component.taskodayv3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.theme.TaskodayV3UtilityTheme
import com.example.taskoday.core.ui.theme.taskodayV3Palette

private val CardShape = RoundedCornerShape(18.dp)
private val ControlShape = RoundedCornerShape(14.dp)

@Composable
fun TaskodayV3PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.taskodayV3Palette.brandPrimary,
            contentColor = MaterialTheme.taskodayV3Palette.textOnPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TaskodayV3SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ControlShape,
        border = BorderStroke(1.dp, MaterialTheme.taskodayV3Palette.brandPrimary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.taskodayV3Palette.brandDeep,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TaskodayV3TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.taskodayV3Palette.brandPrimary),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TaskodayV3TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supportingText?.let { message -> ({ Text(message) }) },
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = ControlShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.taskodayV3Palette.brandPrimary,
            focusedLabelColor = MaterialTheme.taskodayV3Palette.brandPrimary,
            cursorColor = MaterialTheme.taskodayV3Palette.brandPrimary,
            unfocusedBorderColor = MaterialTheme.taskodayV3Palette.border,
        ),
    )
}

@Composable
fun TaskodayV3SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        label = { Text(label) },
        shape = CircleShape,
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.taskodayV3Palette.border,
            selectedBorderColor = MaterialTheme.taskodayV3Palette.brandPrimary,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.taskodayV3Palette.surface,
            labelColor = MaterialTheme.taskodayV3Palette.textPrimary,
            selectedContainerColor = MaterialTheme.taskodayV3Palette.lavender,
            selectedLabelColor = MaterialTheme.taskodayV3Palette.brandDeep,
        ),
    )
}

enum class TaskodayV3ActionKind { ROUTINE, MISSION, QUEST }

@Composable
fun TaskodayV3ActionTypeBadge(
    label: String,
    kind: TaskodayV3ActionKind,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.taskodayV3Palette
    val (foreground, background) = when (kind) {
        TaskodayV3ActionKind.ROUTINE -> palette.routineInk to palette.routineSoft
        TaskodayV3ActionKind.MISSION -> palette.missionInk to palette.missionSoft
        TaskodayV3ActionKind.QUEST -> palette.questInk to palette.questSoft
    }
    TaskodayV3Badge(label, foreground, background, modifier)
}

enum class TaskodayV3TaskStatus { TODO, IN_PROGRESS, PENDING_VALIDATION, COMPLETED, OVERDUE, ERROR }

@Composable
fun TaskodayV3StatusBadge(
    label: String,
    status: TaskodayV3TaskStatus,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.taskodayV3Palette
    val (foreground, background) = when (status) {
        TaskodayV3TaskStatus.TODO -> palette.textSecondary to palette.surfaceSecondary
        TaskodayV3TaskStatus.IN_PROGRESS -> palette.infoInk to palette.infoSoft
        TaskodayV3TaskStatus.PENDING_VALIDATION -> palette.brandDeep to palette.lavender
        TaskodayV3TaskStatus.COMPLETED -> palette.successInk to palette.successSoft
        TaskodayV3TaskStatus.OVERDUE, TaskodayV3TaskStatus.ERROR -> palette.errorInk to palette.errorSoft
    }
    TaskodayV3Badge(label, foreground, background, modifier)
}

@Composable
private fun TaskodayV3Badge(
    label: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = background,
        contentColor = foreground,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TaskodayV3TaskCard(
    title: String,
    typeLabel: String,
    kind: TaskodayV3ActionKind,
    statusLabel: String,
    status: TaskodayV3TaskStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    val accent = when (kind) {
        TaskodayV3ActionKind.ROUTINE -> MaterialTheme.taskodayV3Palette.routine
        TaskodayV3ActionKind.MISSION -> MaterialTheme.taskodayV3Palette.mission
        TaskodayV3ActionKind.QUEST -> MaterialTheme.taskodayV3Palette.quest
    }
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.taskodayV3Palette.border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.taskodayV3Palette.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = accent,
                        size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    )
                }
                .padding(
                    start = if (compact) 13.dp else 17.dp,
                    end = if (compact) 11.dp else 14.dp,
                    top = if (compact) 9.dp else 14.dp,
                    bottom = if (compact) 9.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 9.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.taskodayV3Palette.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TaskodayV3ActionTypeBadge(typeLabel, kind)
                TaskodayV3StatusBadge(statusLabel, status)
            }
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.taskodayV3Palette.textSecondary)
            }
            extraContent()
        }
    }
}

@Composable
fun TaskodayV3FamilyAvatar(
    initials: String,
    contentDescription: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.taskodayV3Palette.textPrimary)
    }
}

@Composable
fun TaskodayV3InfoBlock(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.taskodayV3Palette.lavenderMist,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tone,
        contentColor = MaterialTheme.taskodayV3Palette.textPrimary,
        shape = CardShape,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.taskodayV3Palette.textSecondary)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TaskodayV3ComponentGalleryPreview() {
    TaskodayV3UtilityTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.taskodayV3Palette.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Composants Taskoday", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskodayV3PrimaryButton("Créer une action", {})
                TaskodayV3SecondaryButton("Annuler", {})
            }
            TaskodayV3SelectableChip("Aujourd'hui", selected = true, onClick = {})
            TaskodayV3TaskCard(
                title = "Préparer les affaires",
                typeLabel = "Mission",
                kind = TaskodayV3ActionKind.MISSION,
                statusLabel = "À valider",
                status = TaskodayV3TaskStatus.PENDING_VALIDATION,
                supportingText = "Prévu pour : Léa",
            )
            TaskodayV3InfoBlock("À retenir", "Les choix sont fictifs dans cette prévisualisation.")
        }
    }
}
