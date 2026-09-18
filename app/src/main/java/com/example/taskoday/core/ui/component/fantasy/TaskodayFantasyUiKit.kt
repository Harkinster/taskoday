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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.taskoday.R
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.CarvedWoodDark
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.GlowHalo
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.MagicVioletSoft
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonBorderEnd
import com.example.taskoday.core.ui.theme.NeonBorderStart
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.NeonCyanSoft
import com.example.taskoday.core.ui.theme.NeonPurple
import com.example.taskoday.core.ui.theme.NightBlue850
import com.example.taskoday.core.ui.theme.NightBlue900
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.RoyalPurpleDark
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.SuccessGlow
import com.example.taskoday.core.ui.theme.SurfaceGlass
import com.example.taskoday.core.ui.theme.SurfacePanel
import com.example.taskoday.core.ui.theme.SurfaceSuccess
import com.example.taskoday.core.ui.theme.SurfaceSuccessSoft
import com.example.taskoday.core.ui.theme.SurfaceWarning
import com.example.taskoday.core.ui.theme.TextDimmed
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.fantasyMetrics
import com.example.taskoday.core.ui.theme.taskodayGoldBrush
import com.example.taskoday.core.ui.theme.taskodayNeonBorderBrush
import com.example.taskoday.core.ui.theme.taskodayNeonProgressBrush
import com.example.taskoday.core.ui.theme.taskodayParchmentBrush
import com.example.taskoday.core.ui.theme.taskodayWoodPanelBrush
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
                        .background(Brush.verticalGradient(listOf(MagicViolet, NightBlue900, WoodBrownDark)))
                        .border(
                            width = metrics.cardStroke,
                            color = SoftGold.copy(alpha = 0.86f),
                            shape = CircleShape,
                        ),
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = SoftGold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        UserAvatarBadge(
            initials = avatarInitials,
            size = if (compact) 32.dp else 40.dp,
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
            compact = true,
            showNotification = showNotification,
            onNotificationClick = onNotificationClick,
            onAvatarClick = onAvatarClick,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(metrics.cardCornerLarge))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, ParchmentLight.copy(alpha = 0.12f), RoundedCornerShape(metrics.cardCornerLarge))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MagicViolet.copy(alpha = 0.22f))
                            .border(1.dp, SoftGold.copy(alpha = 0.42f), CircleShape)
                            .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.taskoday_screenbot_logo_icon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ParchmentLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentCream.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    tone: NeonTone = NeonTone.Cyan,
    shape: RoundedCornerShape = RoundedCornerShape(MaterialTheme.fantasyMetrics.cardCorner),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderTone = toneColor(tone)
    Box(
        modifier =
            modifier
                .shadow(elevation = 3.dp, shape = shape, clip = false)
                .clip(shape)
                .background(ParchmentLight.copy(alpha = 0.98f))
                .border(width = 1.dp, color = borderTone.copy(alpha = 0.24f), shape = shape),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun XpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = InkMuted.copy(alpha = 0.18f),
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
                color = ParchmentLight.copy(alpha = 0.96f),
                radius = inner,
                center = center,
            )
        }
        if (centerText.isNotBlank()) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleSmall,
                color = ParchmentLight,
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
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 38.dp)
                .shadow(elevation = if (enabled) 2.dp else 0.dp, shape = shape, clip = false)
                .clip(shape)
                .background(taskodayReadableButtonBrush(style, enabled))
                .clickable(enabled = enabled, onClick = onClick)
                .border(
                    width = 1.dp,
                    color = if (style == NeonButtonStyle.Filled) MagicViolet.copy(alpha = 0.62f) else MagicViolet.copy(alpha = 0.34f),
                    shape = shape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLine(
                color = ParchmentLight.copy(alpha = if (enabled) 0.48f else 0.18f),
                start = Offset(size.width * 0.12f, 4f),
                end = Offset(size.width * 0.88f, 4f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> TextMuted
                style == NeonButtonStyle.Filled -> ParchmentLight
                else -> MagicViolet
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressBadge(
                progress = progress,
                size = 60.dp,
                strokeWidth = 7.dp,
                centerText = if (total <= 0) "0%" else "${(progress * 100f).toInt()}%",
                completed = completed == total && total > 0,
            )
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
                Text(
                    text = "$completed/$total terminées",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!badgeLabel.isNullOrBlank()) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyanSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
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
    statusLabel: String? = null,
    isOverdue: Boolean = false,
    rewardLabel: String? = null,
    actionEnabled: Boolean = true,
    isSubmitting: Boolean = false,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onToggleDone: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (done) {
                        Brush.horizontalGradient(listOf(ParchmentLight, SurfaceSuccessSoft))
                    } else if (isOverdue) {
                        Brush.horizontalGradient(listOf(ParchmentLight, SurfaceWarning))
                    } else {
                        Brush.horizontalGradient(listOf(ParchmentLight, ParchmentCream))
                    },
                )
                .border(
                    width = 1.dp,
                    color = when {
                        done -> SuccessGlow.copy(alpha = 0.52f)
                        isOverdue -> EmberOrange.copy(alpha = 0.48f)
                        else -> MagicViolet.copy(alpha = 0.18f)
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(horizontal = 9.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MagicViolet.copy(alpha = 0.10f))
                    .border(1.dp, MagicViolet.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MagicViolet.copy(alpha = 0.82f),
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WoodBrownDark,
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
            if (!rewardLabel.isNullOrBlank()) {
                Text(
                    text = rewardLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmberOrange,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!statusLabel.isNullOrBlank()) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (done) SuccessGlow else EmberOrange,
                maxLines = 1,
            )
        }
        if (onEdit != null || onDelete != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                onEdit?.let { editAction ->
                    IconButton(
                        onClick = editAction,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Modifier",
                            tint = ArcaneViolet,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                onDelete?.let { deleteAction ->
                    IconButton(
                        onClick = deleteAction,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = DangerGlow,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
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
                    .clickable(enabled = actionEnabled, onClick = onToggleDone),
            contentAlignment = Alignment.Center,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MagicViolet,
                    strokeWidth = 2.dp,
                )
            } else if (done) {
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
    isOverdue: Boolean = false,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(
                    when {
                        done -> SurfaceSuccess
                        isOverdue -> SurfaceWarning
                        else -> ParchmentLight
                    },
                )
                .border(
                    width = 1.dp,
                    color =
                        when {
                            done -> SuccessGlow.copy(alpha = 0.35f)
                            isOverdue -> EmberOrange.copy(alpha = 0.45f)
                            else -> MagicViolet.copy(alpha = 0.16f)
                        },
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(toneColor(tone).copy(alpha = 0.10f))
                        .border(1.dp, toneColor(tone).copy(alpha = 0.26f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = toneColor(tone).copy(alpha = 0.82f),
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
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
                            color = MagicViolet,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOverdue) EmberOrange else toneColor(tone),
                    )
                }
                XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (done) SuccessGlow.copy(alpha = 0.20f) else ParchmentCream.copy(alpha = 0.88f))
                        .border(
                            width = 1.2.dp,
                            color = if (done) SuccessGlow else MagicViolet,
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
                        modifier = Modifier.size(17.dp),
                    )
                } else {
                    Text(
                        text = completionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MagicViolet,
                    )
                }
            }
            val editAction = onEdit
            val deleteAction = onDelete
            if (editAction != null || deleteAction != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editAction != null) {
                        CompactIconAction(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Éditer",
                            tint = WoodBrownDark.copy(alpha = 0.78f),
                            onClick = editAction,
                        )
                    }
                    if (deleteAction != null) {
                        CompactIconAction(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = WoodBrownDark.copy(alpha = 0.58f),
                            onClick = deleteAction,
                        )
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
    ReferenceQuestCard(
        title = title,
        description = description,
        xpLabel = xpLabel,
        progress = progress,
        actionLabel = actionLabel,
        dayPartLabel = dayPartLabel,
        modifier = modifier,
        done = done,
        canManage = canManage,
        onAction = onAction,
        onEdit = onEdit,
        onDelete = onDelete,
    )
    return

    val shape = RoundedCornerShape(MaterialTheme.fantasyMetrics.cardCorner)
    Box(
            modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 7.dp, shape = shape, clip = false)
                .clip(shape)
                .background(taskodayQuestCardBrush())
                .border(
                    width = MaterialTheme.fantasyMetrics.cardStrokeStrong,
                    brush = Brush.linearGradient(listOf(SoftGold, NeonBorderEnd, SoftGold.copy(alpha = 0.72f))),
                    shape = shape,
                ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = GlowHalo.copy(alpha = 0.18f),
                radius = size.minDimension * 0.74f,
                center = Offset(size.width * 0.02f, size.height * 0.02f),
            )
            drawLine(
                color = SoftGold.copy(alpha = 0.42f),
                start = Offset(size.width * 0.07f, 5f),
                end = Offset(size.width * 0.93f, 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.4.dp, SoftGold.copy(alpha = 0.92f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                FantasySkinSurface(
                    asset = FantasySkinAssets.iconFrameGold,
                    fallbackBrush = taskodayWoodPanelBrush(),
                    modifier = Modifier.matchParentSize(),
                ) {}
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentCream,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = dayPartLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentCream.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                XpProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier =
                    Modifier
                        .widthIn(min = 88.dp, max = 104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MagicViolet.copy(alpha = 0.34f))
                        .border(1.dp, SoftGold.copy(alpha = 0.66f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "Récompenses",
                    style = MaterialTheme.typography.labelMedium,
                    color = ParchmentCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = xpLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                NeonButton(
                    text = actionLabel,
                    onClick = onAction,
                    style = if (done) NeonButtonStyle.Outline else NeonButtonStyle.Filled,
                    modifier = Modifier.fillMaxWidth(),
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
                            contentDescription = "Éditer",
                            tint = ParchmentLight,
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = ParchmentCream,
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ReferenceQuestCard(
    title: String,
    description: String?,
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
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(ParchmentLight.copy(alpha = 0.98f))
                .border(width = 1.dp, color = MagicViolet.copy(alpha = 0.18f), shape = shape)
                .clickable(onClick = onAction),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = MagicViolet.copy(alpha = 0.045f),
                radius = size.minDimension * 0.54f,
                center = Offset(size.width * 0.02f, size.height * 0.05f),
            )
            drawCircle(
                color = SoftGold.copy(alpha = 0.025f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.86f, size.height * 0.18f),
            )
            drawLine(
                color = MagicViolet.copy(alpha = 0.10f),
                start = Offset(size.width * 0.07f, 5f),
                end = Offset(size.width * 0.93f, 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MagicViolet.copy(alpha = 0.10f))
                        .border(1.dp, MagicViolet.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MagicViolet,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WoodBrownDark,
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
                FantasyActionMedallion(onClick = onAction, dark = true)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FantasyInfoChip(text = xpLabel, accent = MagicViolet, modifier = Modifier.weight(1f))
                FantasyInfoChip(
                    text = if (done) "Terminée" else "En cours",
                    accent = if (done) SuccessGlow else MagicViolet,
                    modifier = Modifier.weight(1f),
                )
                FantasyCompactButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.widthIn(min = 82.dp, max = 104.dp),
                )
            }
            if (canManage && (onEdit != null || onDelete != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onEdit != null) {
                        CompactIconAction(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Éditer",
                            tint = ParchmentLight,
                            onClick = onEdit,
                        )
                    }
                    if (onDelete != null) {
                        CompactIconAction(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            tint = ParchmentCream,
                            onClick = onDelete,
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
                            .background(MagicViolet.copy(alpha = 0.07f))
                            .border(
                                width = 1.dp,
                                color = MagicViolet.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val rewardAsset =
                        when {
                            reward.label.contains("flamm", ignoreCase = true) -> R.drawable.v2_resource_flammeche
                            reward.label.contains("crist", ignoreCase = true) -> R.drawable.v2_resource_crystal
                            reward.label.contains("xp", ignoreCase = true) -> R.drawable.v2_resource_xp
                            else -> null
                        }
                    if (rewardAsset != null) {
                        Image(
                            painter = painterResource(id = rewardAsset),
                            contentDescription = reward.label,
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(text = reward.emoji, style = MaterialTheme.typography.titleLarge)
                    }
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
    attentionDestinationRoutes: Set<String> = emptySet(),
) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Box(
            modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 14.dp, shape = shape, clip = false)
                .clip(shape)
                .background(taskodayBottomNavBrush())
                .border(
                    width = 1.8.dp,
                    brush = taskodayNeonBorderBrush(),
                    shape = shape,
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLine(
                color = SoftGold.copy(alpha = 0.34f),
                start = Offset(size.width * 0.06f, 4f),
                end = Offset(size.width * 0.94f, 4f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            destinations.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                val itemColor = if (selected) SoftGold else ParchmentCream.copy(alpha = 0.86f)
                val itemShape = RoundedCornerShape(13.dp)

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .background(
                                if (selected) taskodayBottomNavActiveBrush() else taskodayBottomNavInactiveBrush(),
                            )
                            .border(
                                width = if (selected) 1.4.dp else 0.8.dp,
                                color =
                                    if (selected) {
                                        SoftGold.copy(alpha = 0.9f)
                                    } else {
                                        SoftGold.copy(alpha = 0.18f)
                                    },
                                shape = itemShape,
                            )
                            .clickable { onNavigate(destination) }
                            .padding(vertical = if (selected) 5.dp else 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    destination.icon?.let { icon ->
                        Box(
                            modifier =
                                Modifier
                                    .size(if (selected) 30.dp else 26.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (selected) {
                                            taskodayGoldBrush()
                                        } else {
                                            Brush.verticalGradient(listOf(MagicViolet.copy(alpha = 0.24f), WoodBrownDark.copy(alpha = 0.24f)))
                                        },
                                    )
                                    .border(
                                        width = if (selected) 1.dp else 0.8.dp,
                                        color = if (selected) SoftGold.copy(alpha = 0.82f) else SoftGold.copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(9.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = destination.label,
                                tint = if (selected) WoodBrownDark else itemColor,
                                modifier = Modifier.size(if (selected) 19.dp else 17.dp),
                            )
                            if (destination.route in attentionDestinationRoutes) {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(WarningGlow)
                                            .border(1.dp, WoodBrownDark, RoundedCornerShape(100.dp)),
                                )
                            }
                        }
                    }
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = itemColor,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                    if (selected) {
                        Box(
                            modifier =
                                Modifier
                                    .width(22.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(SoftGold.copy(alpha = 0.92f)),
                        )
                    }
                }
            }
        }
    }
}

/** The V2 navigation keeps the chrome quiet so the day's work stays primary. */
@Composable
fun TaskodayV2BottomNavigation(
    destinations: List<TaskodayDestination>,
    currentDestination: NavDestination?,
    onNavigate: (TaskodayDestination) -> Unit,
    modifier: Modifier = Modifier,
    attentionDestinationRoutes: Set<String> = emptySet(),
    notificationBadges: Map<String, Int> = emptyMap(),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF151020).copy(alpha = 0.98f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(destination) }
                        .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ParchmentLight.copy(alpha = 0.10f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    destination.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = destination.label,
                            tint = if (selected) ParchmentLight else ParchmentCream.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (destination.route in attentionDestinationRoutes || destination.route in notificationBadges) {
                        val badge = notificationBadges[destination.route]
                        if (badge != null && badge > 0) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(SoftGold)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = badge.coerceAtMost(99).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WoodBrownDark,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).size(7.dp).clip(CircleShape).background(SoftGold),
                            )
                        }
                    }
                }
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) ParchmentLight else ParchmentCream.copy(alpha = 0.58f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

private fun taskodayV2NavIcon(route: String): Int? =
    when (route) {
        TaskodayDestination.FamilyHome.route -> R.drawable.v2_nav_family_parent
        TaskodayDestination.Home.route -> R.drawable.v2_nav_routine
        TaskodayDestination.Tasks.route -> R.drawable.v2_nav_mission
        TaskodayDestination.Quests.route -> R.drawable.v2_nav_quest
        TaskodayDestination.Nest.route -> R.drawable.v2_nav_home
        else -> null
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

@Composable
private fun FantasyActionMedallion(
    onClick: () -> Unit,
    dark: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .shadow(elevation = 2.dp, shape = shape, clip = false)
                .clip(shape)
                .background(if (dark) MagicViolet.copy(alpha = 0.88f) else MagicViolet.copy(alpha = 0.10f))
                .border(1.dp, if (dark) MagicViolet else MagicViolet.copy(alpha = 0.28f), shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = if (dark) ParchmentLight else MagicViolet,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun FantasyInfoChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (dark) MagicViolet.copy(alpha = 0.32f) else ParchmentLight.copy(alpha = 0.76f))
                .border(1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(100.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (dark) ParchmentCream else WoodBrownDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp),
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

private fun taskodayReadableHeaderBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(WoodBrownDark, MagicViolet, NightBlue900),
    )

private fun taskodayReadablePanelBrush(tone: NeonTone): Brush =
    Brush.verticalGradient(
        colors =
            when (tone) {
                NeonTone.Violet,
                NeonTone.Danger,
                -> listOf(ParchmentLight, ParchmentCream, Color(0xFFF3E7FF), Color(0xFFE9D8F7))
                NeonTone.Warning -> listOf(ParchmentLight, ParchmentCream, Color(0xFFFFE9B8), Color(0xFFFFD58A))
                NeonTone.Success -> listOf(ParchmentLight, ParchmentCream, Color(0xFFEAF4D6), Color(0xFFD9E9BA))
                NeonTone.Blue,
                NeonTone.Cyan,
                -> listOf(ParchmentLight, ParchmentCream, Color(0xFFFFE9B8), Color(0xFFEBD7FF))
            },
    )

private fun taskodayReadableButtonBrush(style: NeonButtonStyle, enabled: Boolean): Brush =
    if (!enabled) {
        Brush.verticalGradient(listOf(ParchmentCream.copy(alpha = 0.88f), TextDimmed.copy(alpha = 0.28f)))
    } else {
        when (style) {
            NeonButtonStyle.Filled -> Brush.verticalGradient(listOf(MagicViolet, RoyalPurpleDark))
            NeonButtonStyle.Outline -> Brush.verticalGradient(listOf(ParchmentLight, ParchmentCream))
        }
    }

private fun taskodayQuestCardBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(WoodBrownDark, MagicViolet.copy(alpha = 0.98f), NightBlue900),
    )

private fun taskodayBottomNavBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(Color(0xFF1B0B2A), Color(0xFF3A1D3B), CarvedWoodDark, Color(0xFF140817)),
    )

private fun taskodayBottomNavActiveBrush(): Brush =
    Brush.verticalGradient(
        colors =
            listOf(
                Color(0xFF6C3BA4),
                MagicViolet.copy(alpha = 0.98f),
                Color(0xFF421957),
                WoodBrownDark.copy(alpha = 0.98f),
            ),
    )

private fun taskodayBottomNavInactiveBrush(): Brush =
    Brush.verticalGradient(
        colors =
            listOf(
                Color(0xFF321A40).copy(alpha = 0.74f),
                Color(0xFF1C0D29).copy(alpha = 0.92f),
                Color(0xFF120716).copy(alpha = 0.96f),
            ),
    )
