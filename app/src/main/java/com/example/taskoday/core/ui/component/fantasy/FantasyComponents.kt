package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taskoday.R
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.NebulaViolet
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.NeonCyanSoft
import com.example.taskoday.core.ui.theme.NightBlue850
import com.example.taskoday.core.ui.theme.NightBlue900
import com.example.taskoday.core.ui.theme.NightBlue950
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.SuccessGlow
import com.example.taskoday.core.ui.theme.SurfacePanel
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow

@Composable
fun FantasyScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(NightBlue900, NightBlue950),
                        ),
                ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                color = ArcaneViolet.copy(alpha = 0.28f),
                radius = width * 0.54f,
                center = Offset(width * 0.85f, height * 0.16f),
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.12f),
                radius = width * 0.46f,
                center = Offset(width * 0.12f, height * 0.70f),
            )
            drawCircle(
                color = NeonBlue.copy(alpha = 0.18f),
                radius = width * 0.40f,
                center = Offset(width * 0.88f, height * 0.90f),
            )

            for (i in 0..54) {
                val x = (((i * 67) % 100) / 100f) * width
                val y = (((i * 37 + 19) % 100) / 100f) * height
                val radius = if (i % 4 == 0) 2.2f else 1.2f
                drawCircle(
                    color = StarWhite.copy(alpha = 0.14f),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
        content()
    }
}

@Composable
fun TaskodayBrand(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val iconSize = if (compact) 30.dp else 38.dp
    val wordmarkWidth = if (compact) 112.dp else 150.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.taskoday_screenbot_logo_icon),
            contentDescription = "Screenbot logo",
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(id = R.drawable.taskoday_screenbot_wordmark),
            contentDescription = "Taskoday",
            modifier = Modifier.width(wordmarkWidth),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
fun UserAvatarBadge(
    modifier: Modifier = Modifier,
    initials: String = "A",
    size: Dp = 42.dp,
    onClick: (() -> Unit)? = null,
) {
    val avatarModifier =
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush =
                    Brush.radialGradient(
                        colors = listOf(NeonBlue.copy(alpha = 0.80f), NightBlue850),
                    ),
            )
            .border(
                width = 1.2.dp,
                color = NeonCyanSoft.copy(alpha = 0.9f),
                shape = CircleShape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = avatarModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = StarWhite,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun FantasyHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    avatarInitials: String = "A",
    showNotification: Boolean = true,
    onNotificationClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TaskodayBrand()
            Spacer(modifier = Modifier.weight(1f))

            if (showNotification) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfacePanel.copy(alpha = 0.7f))
                            .border(
                                width = 1.dp,
                                color = NeonCyan.copy(alpha = 0.48f),
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
                Spacer(modifier = Modifier.size(8.dp))
            }

            UserAvatarBadge(initials = avatarInitials)
        }

        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = StarWhite,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = SurfacePanel.copy(alpha = 0.74f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    brush =
                        Brush.linearGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.78f), ArcaneViolet.copy(alpha = 0.74f)),
                        ),
                    shape = shape,
                ),
    ) {
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

enum class NeonBadgeTone {
    Default,
    Success,
    Warning,
    Danger,
}

@Composable
fun NeonBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: NeonBadgeTone = NeonBadgeTone.Default,
) {
    val toneColor =
        when (tone) {
            NeonBadgeTone.Default -> NeonCyan
            NeonBadgeTone.Success -> SuccessGlow
            NeonBadgeTone.Warning -> WarningGlow
            NeonBadgeTone.Danger -> DangerGlow
        }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(100.dp))
                .background(toneColor.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = toneColor.copy(alpha = 0.76f),
                    shape = RoundedCornerShape(100.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = toneColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = TextMuted.copy(alpha = 0.25f),
    height: Dp = 8.dp,
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
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(NebulaViolet, NeonCyan),
                            ),
                    ),
        )
    }
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
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
fun NeonOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border =
            BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.8f), ArcaneViolet.copy(alpha = 0.8f))),
            ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = StarWhite,
                disabledContentColor = TextMuted,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FantasySectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        if (!badge.isNullOrBlank()) {
            NeonBadge(text = badge)
        }
    }
}
