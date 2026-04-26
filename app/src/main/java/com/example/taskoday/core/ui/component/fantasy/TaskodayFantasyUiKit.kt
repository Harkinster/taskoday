package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.taskoday.R
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.GlowHalo
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonBorderEnd
import com.example.taskoday.core.ui.theme.NeonBorderStart
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.NeonCyanSoft
import com.example.taskoday.core.ui.theme.NeonPurple
import com.example.taskoday.core.ui.theme.NightBlue850
import com.example.taskoday.core.ui.theme.NightBlue900
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.SuccessGlow
import com.example.taskoday.core.ui.theme.SurfaceGlass
import com.example.taskoday.core.ui.theme.SurfacePanel
import com.example.taskoday.core.ui.theme.TextDimmed
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.fantasyMetrics
import com.example.taskoday.core.ui.theme.taskodayNeonBorderBrush
import com.example.taskoday.core.ui.theme.taskodayNeonProgressBrush
import com.example.taskoday.navigation.TaskodayDestination

enum class NeonTone {
    Cyan,
    Violet,
    Blue,
    Success,
    Warning,
    Danger,
}

enum class NeonButtonStyle {
    Filled,
    Outline,
}

data class TaskodayStatItem(
    val label: String,
    val value: String,
    val accent: Color = NeonCyan,
)

data class TaskodayRewardItem(
    val label: String,
    val value: String,
    val emoji: String,
)

@Composable
fun TaskodayTopBar(
    avatarInitials: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showNotification: Boolean = true,
    onNotificationClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val metrics = MaterialTheme.fantasyMetrics
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskodayBrand(compact = compact)
        Spacer(modifier = Modifier.weight(1f))

        if (showNotification) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfacePanel.copy(alpha = 0.74f))
                        .border(
                            width = metrics.cardStroke,
                            brush = taskodayNeonBorderBrush(),
                            shape = CircleShape,
                        ),
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = StarWhite,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        UserAvatarBadge(
            initials = avatarInitials,
            size = if (compact) 40.dp else 44.dp,
            onClick = onAvatarClick,
        )
        trailingContent()
    }
}

@Composable
fun TaskodayHeader(
    title: String,
    subtitle: String,
    avatarInitials: String,
    modifier: Modifier = Modifier,
    showNotification: Boolean = true,
    onNotificationClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
) {
    val metrics = MaterialTheme.fantasyMetrics
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
    ) {
        TaskodayTopBar(
            avatarInitials = avatarInitials,
            showNotification = showNotification,
            onNotificationClick = onNotificationClick,
            onAvatarClick = onAvatarClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = StarWhite,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    tone: NeonTone = NeonTone.Cyan,
    shape: RoundedCornerShape = RoundedCornerShape(MaterialTheme.fantasyMetrics.cardCorner),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderTone = toneColor(tone)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(SurfacePanel.copy(alpha = 0.92f), SurfaceGlass),
                        ),
                )
                .border(
                    width = MaterialTheme.fantasyMetrics.cardStroke,
                    brush =
                        Brush.linearGradient(
                            listOf(borderTone.copy(alpha = 0.9f), NeonBorderEnd.copy(alpha = 0.86f)),
                        ),
                    shape = shape,
                ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = GlowHalo.copy(alpha = 0.25f),
                radius = size.minDimension * 0.60f,
                center = Offset(size.width * 0.95f, size.height * 0.05f),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun XpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = TextDimmed.copy(alpha = 0.45f),
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .height(height)
                .clip(RoundedCornerShape(100.dp))
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(taskodayNeonProgressBrush()),
        )
    }
}

@Composable
fun CircularProgressBadge(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 8.dp,
    centerText: String = "",
    completed: Boolean = false,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val color = if (completed) SuccessGlow else NeonCyan
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = TextDimmed.copy(alpha = 0.4f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(color, NeonPurple, color)),
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
            val inner = this.size.minDimension * 0.35f
            drawCircle(
                color = NightBlue900.copy(alpha = 0.94f),
                radius = inner,
                center = center,
            )
        }
        if (centerText.isNotBlank()) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleSmall,
                color = StarWhite,
                textAlign = TextAlign.Center,
            )
        } else if (completed) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = SuccessGlow,
                modifier = Modifier.size(size * 0.28f),
            )
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NeonButtonStyle = NeonButtonStyle.Filled,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(MaterialTheme.fantasyMetrics.buttonCorner)
    if (style == NeonButtonStyle.Outline) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.defaultMinSize(minHeight = 42.dp),
            shape = shape,
            border =
                androidx.compose.foundation.BorderStroke(
                    width = MaterialTheme.fantasyMetrics.cardStroke,
                    brush = taskodayNeonBorderBrush(),
                ),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = StarWhite,
                    disabledContentColor = TextMuted,
                ),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
        return
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 42.dp),
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = NeonBlue,
                contentColor = StarWhite,
                disabledContainerColor = NightBlue850,
                disabledContentColor = TextMuted,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProgressHeroCard(
    title: String,
    completed: Int,
    total: Int,
    progress: Float,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: NeonTone = NeonTone.Cyan,
    badgeLabel: String? = null,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = accent,
        shape = RoundedCornerShape(MaterialTheme.fantasyMetrics.cardCornerLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressBadge(
                progress = progress,
                size = 92.dp,
                centerText = if (total <= 0) "0%" else "${(progress * 100f).toInt()}%",
                completed = completed == total && total > 0,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = StarWhite,
                )
                Text(
                    text = "$completed/$total completes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonCyan,
                )
                XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            if (!badgeLabel.isNullOrBlank()) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyanSoft,
                )
            }
        }
    }
}

@Composable
fun RoutineSectionCard(
    title: String,
    progressLabel: String,
    modifier: Modifier = Modifier,
    tone: NeonTone = NeonTone.Blue,
    content: @Composable ColumnScope.() -> Unit,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = tone,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan,
            )
        }
        content()
    }
}

@Composable
fun RoutineItemRow(
    title: String,
    emoji: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onToggleDone: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (done) {
                        NeonBlue.copy(alpha = 0.14f)
                    } else {
                        Color.Transparent
                    },
                )
                .border(
                    width = 1.dp,
                    color = if (done) NeonCyan.copy(alpha = 0.74f) else NeonBorderStart.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = StarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (done) SuccessGlow else ArcaneViolet,
                        shape = CircleShape,
                    )
                    .clickable(onClick = onToggleDone),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = SuccessGlow,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun MissionCard(
    title: String,
    description: String?,
    emoji: String,
    dueLabel: String?,
    statusLabel: String,
    progress: Float,
    completionLabel: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    tone: NeonTone = if (done) NeonTone.Success else NeonTone.Blue,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = tone,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonBlue.copy(alpha = 0.22f))
                        .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StarWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!dueLabel.isNullOrBlank()) {
                        Text(
                            text = dueLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyanSoft,
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = toneColor(tone),
                    )
                }
                XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = completionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (done) SuccessGlow else ArcaneViolet,
                                shape = CircleShape,
                            )
                            .clickable(onClick = onToggleDone),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = SuccessGlow,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (onEdit != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editer",
                            tint = StarWhite,
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestCard(
    title: String,
    description: String?,
    emoji: String,
    xpLabel: String,
    progress: Float,
    actionLabel: String,
    dayPartLabel: String,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    canManage: Boolean = false,
    onAction: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = if (done) NeonTone.Success else NeonTone.Violet,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArcaneViolet.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StarWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = dayPartLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = xpLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan,
                )
                NeonButton(
                    text = actionLabel,
                    onClick = onAction,
                    style = if (done) NeonButtonStyle.Outline else NeonButtonStyle.Filled,
                    modifier = Modifier.widthIn(min = 104.dp),
                )
            }
        }
        if (canManage && (onEdit != null || onDelete != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editer",
                            tint = StarWhite,
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeroCard(
    name: String,
    subtitle: String,
    pointsLabel: String,
    levelLabel: String,
    xpLabel: String,
    progress: Float,
    avatarInitials: String,
    modifier: Modifier = Modifier,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = NeonTone.Violet,
        shape = RoundedCornerShape(MaterialTheme.fantasyMetrics.cardCornerLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatarBadge(initials = avatarInitials, size = 86.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = StarWhite,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pointsLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                    )
                    Text(
                        text = levelLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyanSoft,
                    )
                }
            }
        }
        XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        Text(
            text = xpLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    stats: List<TaskodayStatItem>,
    modifier: Modifier = Modifier,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = NeonTone.Blue,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = StarWhite,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            stats.forEach { stat ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.titleLarge,
                        color = stat.accent,
                    )
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun RewardsCard(
    title: String,
    rewards: List<TaskodayRewardItem>,
    modifier: Modifier = Modifier,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        tone = NeonTone.Violet,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = StarWhite,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rewards.forEach { reward ->
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NightBlue900.copy(alpha = 0.6f))
                            .border(
                                width = 1.dp,
                                color = NeonBorderEnd.copy(alpha = 0.54f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = reward.emoji,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = reward.value,
                        style = MaterialTheme.typography.labelLarge,
                        color = StarWhite,
                    )
                    Text(
                        text = reward.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun FantasyBottomNavigation(
    destinations: List<TaskodayDestination>,
    currentDestination: NavDestination?,
    onNavigate: (TaskodayDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.verticalGradient(
                            listOf(SurfaceGlass.copy(alpha = 0.98f), NightBlue900.copy(alpha = 0.98f)),
                        ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
                .border(
                    width = 1.dp,
                    brush = taskodayNeonBorderBrush(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            destinations.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                val itemColor = if (selected) NeonCyan else TextMuted

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    NeonBlue.copy(alpha = 0.20f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .border(
                                width = 1.dp,
                                color =
                                    if (selected) {
                                        NeonCyan.copy(alpha = 0.7f)
                                    } else {
                                        Color.Transparent
                                    },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onNavigate(destination) }
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    destination.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = destination.label,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = itemColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun QuestLevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(ArcaneViolet.copy(alpha = 0.22f))
                .border(
                    width = 1.dp,
                    color = NeonBorderEnd.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "NIV",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyanSoft,
            )
            Text(
                text = level.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = StarWhite,
            )
        }
    }
}

@Composable
fun TaskodayDragonWatermark(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.taskoday_screenbot_logo_icon),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alpha = 0.12f,
    )
}

@Composable
fun RewardSummaryChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(100.dp))
                .background(NeonBlue.copy(alpha = 0.14f))
                .border(
                    width = 1.dp,
                    color = NeonBorderStart.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(100.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = StarWhite,
        )
    }
}

private fun toneColor(tone: NeonTone): Color =
    when (tone) {
        NeonTone.Cyan -> NeonCyan
        NeonTone.Violet -> NeonPurple
        NeonTone.Blue -> NeonBlue
        NeonTone.Success -> SuccessGlow
        NeonTone.Warning -> WarningGlow
        NeonTone.Danger -> DangerGlow
    }
