package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.EmberOrangeSoft
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.MagicVioletSoft
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.MossGreenSoft
import com.example.taskoday.core.ui.theme.NestNightBlue
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.ParchmentShadow
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.SoftGoldPale
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.ui.theme.taskodayGoldBrush
import com.example.taskoday.core.ui.theme.taskodayNeonProgressBrush
import com.example.taskoday.core.ui.theme.taskodayParchmentBrush

enum class FantasyTone(
    val accent: Color,
    val soft: Color,
) {
    Ember(EmberOrange, EmberOrangeSoft.copy(alpha = 0.22f)),
    Gold(SoftGold, SoftGoldPale.copy(alpha = 0.55f)),
    Violet(MagicViolet, MagicVioletSoft.copy(alpha = 0.18f)),
    Night(NestNightBlue, NestNightBlue.copy(alpha = 0.10f)),
    Wood(WoodBrown, WoodBrown.copy(alpha = 0.14f)),
    Moss(MossGreen, MossGreen.copy(alpha = 0.18f)),
}

enum class FantasyButtonStyle {
    Filled,
    Outline,
    Quiet,
}

private fun fantasyCardBrush(tone: FantasyTone): Brush =
    Brush.verticalGradient(
        colors =
            when (tone) {
                FantasyTone.Night,
                FantasyTone.Violet,
                -> listOf(ParchmentLight, ParchmentCream, Color(0xFFE9D8F7))
                FantasyTone.Wood -> listOf(ParchmentLight, ParchmentCream, Color(0xFFEED4AE))
                FantasyTone.Ember -> listOf(ParchmentLight, ParchmentCream, Color(0xFFFFD0A3))
                FantasyTone.Moss -> listOf(ParchmentLight, ParchmentCream, Color(0xFFE6F0D0))
                FantasyTone.Gold -> listOf(ParchmentLight, ParchmentCream, Color(0xFFFFE7B1))
            },
    )

private fun fantasyButtonBrush(style: FantasyButtonStyle, enabled: Boolean): Brush =
    if (!enabled) {
        Brush.verticalGradient(listOf(ParchmentShadow.copy(alpha = 0.74f), WoodBrown.copy(alpha = 0.34f)))
    } else {
        when (style) {
            FantasyButtonStyle.Filled -> taskodayGoldBrush()
            FantasyButtonStyle.Outline -> Brush.verticalGradient(listOf(MagicViolet, WoodBrownDark))
            FantasyButtonStyle.Quiet -> Brush.verticalGradient(listOf(ParchmentLight, ParchmentCream))
        }
    }

private fun fantasyButtonTextColor(style: FantasyButtonStyle, enabled: Boolean): Color =
    when {
        !enabled -> InkMuted
        style == FantasyButtonStyle.Filled -> WoodBrownDark
        style == FantasyButtonStyle.Quiet -> WoodBrownDark
        else -> ParchmentLight
    }

private fun fantasyBadgeBrush(tone: FantasyTone): Brush =
    Brush.horizontalGradient(
        colors =
            if (tone == FantasyTone.Night) {
                listOf(WoodBrownDark, MagicViolet)
            } else {
                listOf(ParchmentLight, tone.soft.copy(alpha = 0.78f))
            },
    )

private fun fantasyHeaderBrush(): Brush =
    Brush.verticalGradient(
        listOf(
            WoodBrownDark,
            MagicViolet.copy(alpha = 0.96f),
            NestNightBlue,
        ),
    )

@Composable
fun FantasyCard(
    modifier: Modifier = Modifier,
    tone: FantasyTone = FantasyTone.Gold,
    contentPadding: PaddingValues = PaddingValues(horizontal = 13.dp, vertical = 11.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 58.dp)
                .shadow(elevation = 5.dp, shape = shape, clip = false)
                .clip(shape)
                .background(fantasyCardBrush(tone))
                .border(
                    width = 1.8.dp,
                    brush =
                        Brush.linearGradient(
                            listOf(
                                SoftGold.copy(alpha = 0.95f),
                                tone.accent.copy(alpha = 0.78f),
                                ParchmentShadow.copy(alpha = 0.88f),
                            ),
                    ),
                    shape = shape,
                ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLine(
                color = SoftGold.copy(alpha = 0.32f),
                start = Offset(size.width * 0.08f, 5f),
                end = Offset(size.width * 0.92f, 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = WoodBrown.copy(alpha = 0.14f),
                start = Offset(size.width * 0.08f, size.height - 5f),
                end = Offset(size.width * 0.92f, size.height - 5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(tone.soft.copy(alpha = 0.30f), Color.Transparent),
                                radius = 420f,
                            ),
                    ),
        )
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
fun FantasyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FantasyButtonStyle = FantasyButtonStyle.Filled,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(13.dp)
    val buttonModifier =
            modifier
                .defaultMinSize(minHeight = 38.dp)
                .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = shape, clip = false)
                .clip(shape)
                .background(fantasyButtonBrush(style, enabled))
                .clickable(enabled = enabled, onClick = onClick)
    val textContent: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fantasyButtonTextColor(style, enabled),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Box(
        modifier =
            buttonModifier
                .border(
                    width = if (style == FantasyButtonStyle.Filled) 1.dp else 1.2.dp,
                    color = if (style == FantasyButtonStyle.Filled) MagicViolet.copy(alpha = 0.48f) else SoftGold.copy(alpha = 0.70f),
                    shape = shape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            textContent()
        }
    }
}

@Composable
fun FantasyCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .widthIn(min = 72.dp, max = 108.dp)
                .defaultMinSize(minHeight = 34.dp),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, if (enabled) SoftGold.copy(alpha = 0.88f) else MagicViolet.copy(alpha = 0.36f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = ParchmentLight.copy(alpha = 0.84f),
                contentColor = WoodBrownDark,
                disabledContainerColor = ParchmentShadow.copy(alpha = 0.48f),
                disabledContentColor = InkMuted,
            ),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
fun FantasyBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: FantasyTone = FantasyTone.Gold,
) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier =
            modifier
                .widthIn(max = 128.dp)
                .clip(shape)
                .background(fantasyBadgeBrush(tone))
                .border(1.dp, tone.accent.copy(alpha = 0.70f), shape)
                .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (tone == FantasyTone.Night) ParchmentLight else WoodBrownDark,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
fun FantasyHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    avatarInitials: String = "AB",
    assetResId: Int = NestAssets.interfaceAsset("nid"),
    assetDescription: String? = "Le Nid",
    onAvatarClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        TaskodayTopBar(
            avatarInitials = avatarInitials,
            compact = true,
            showNotification = false,
            onAvatarClick = onAvatarClick,
        )
        if (onBackClick != null) {
            FantasyCompactButton(
                text = "Retour au Nid",
                onClick = onBackClick,
            )
        }
        FantasyScreenHeader(
            title = title,
            subtitle = subtitle,
            assetResId = assetResId,
            assetDescription = assetDescription,
        )
    }
}

@Composable
fun FantasyScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.interfaceAsset("nid"),
    assetDescription: String? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = shape, clip = false)
                .clip(shape)
                .background(fantasyHeaderBrush())
                .border(1.8.dp, SoftGold.copy(alpha = 0.86f), shape),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = SoftGold.copy(alpha = 0.14f),
                radius = size.minDimension * 0.70f,
                center = Offset(size.width * 0.04f, 0f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = assetDescription, size = 46.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftGoldPale,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentCream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun FantasyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .height(height)
                .clip(RoundedCornerShape(100.dp))
                .background(ParchmentShadow.copy(alpha = 0.7f)),
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
fun NestStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.interfaceAsset("flammeche"),
    contentDescription: String = label,
    tone: FantasyTone = FantasyTone.Gold,
) {
    FantasyCard(modifier = modifier, tone = tone, contentPadding = PaddingValues(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 40.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun EggProgressCard(
    title: String,
    status: String,
    requirements: String,
    progress: Float,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.eggAsset("pyron", "sleeping"),
    contentDescription: String = title,
    locked: Boolean = false,
    materialLabel: String? = null,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = if (locked) FantasyTone.Night else FantasyTone.Gold) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 66.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = status, style = MaterialTheme.typography.bodyMedium, color = MossGreen)
                FantasyProgressBar(progress = progress)
                Text(
                    text = requirements,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!materialLabel.isNullOrBlank()) {
            Text(text = materialLabel, style = MaterialTheme.typography.labelLarge, color = WoodBrownDark)
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            FantasyButton(
                text = actionLabel,
                onClick = onAction,
                style = FantasyButtonStyle.Quiet,
                modifier = Modifier.fillMaxWidth(),
                enabled = !locked && actionEnabled,
            )
        }
    }
}

@Composable
fun DragonCard(
    title: String,
    stage: String,
    nextStep: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.dragonAsset("pyron", "baby"),
    contentDescription: String = title,
    locked: Boolean = false,
    badgeLabel: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = if (locked) FantasyTone.Night else FantasyTone.Ember) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 72.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = stage, style = MaterialTheme.typography.bodyMedium, color = EmberOrange)
                Text(
                    text = nextStep,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!badgeLabel.isNullOrBlank()) {
                    Text(text = badgeLabel, style = MaterialTheme.typography.labelLarge, color = MossGreen)
                }
            }
        }
        if (!primaryActionLabel.isNullOrBlank() && onPrimaryAction != null) {
            FantasyButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
                style = FantasyButtonStyle.Filled,
                modifier = Modifier.fillMaxWidth(),
                enabled = !locked,
            )
        }
        if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
            FantasyButton(
                text = secondaryActionLabel,
                onClick = onSecondaryAction,
                style = FantasyButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth(),
                enabled = !locked,
            )
        }
    }
}

@Composable
fun WishCard(
    title: String,
    description: String,
    costLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    assetResId: Int = NestAssets.interfaceAsset("flammeche"),
    contentDescription: String = "Flammèche",
    onMakeWish: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Violet) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 56.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = costLabel, style = MaterialTheme.typography.labelLarge, color = MagicViolet)
            }
        }
        if (!supportingText.isNullOrBlank()) {
            Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        if (onMakeWish != null) {
            FantasyButton(
                text = "Faire un souhait",
                onClick = onMakeWish,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ScrollCard(
    title: String,
    code: String,
    status: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.scrollAsset("approved"),
    contentDescription: String = "Parchemin",
    onUse: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Wood) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 60.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = status, style = MaterialTheme.typography.labelLarge, color = MossGreen)
            }
        }
        if (onUse != null) {
            FantasyButton(text = "Marquer utilisé", onClick = onUse, style = FantasyButtonStyle.Outline)
        }
    }
}

@Composable
fun ChestCard(
    points: Int,
    pointsRequired: Int,
    unopenedChests: Int,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.chestAsset("common"),
    contentDescription: String = "Coffre",
    rarity: String = "common",
    title: String = "Coffre du Gardien",
    costLabel: String? = null,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    val progress = if (pointsRequired <= 0) 0f else points.toFloat() / pointsRequired.toFloat()
    val tone =
        when (rarity) {
            "rare" -> FantasyTone.Violet
            "epic" -> FantasyTone.Gold
            else -> FantasyTone.Wood
        }
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = tone) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 68.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                if (pointsRequired > 0) {
                    Text(text = "$points/$pointsRequired points coffre", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    FantasyProgressBar(progress = progress)
                }
                if (!costLabel.isNullOrBlank()) {
                    Text(text = costLabel, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
                if (unopenedChests > 0) {
                    val waitingLabel = if (unopenedChests > 1) "coffres en attente" else "coffre en attente"
                    Text(text = "$unopenedChests $waitingLabel", style = MaterialTheme.typography.bodySmall, color = MossGreen)
                }
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            FantasyButton(
                text = actionLabel,
                onClick = onAction,
                style = FantasyButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth(),
                enabled = actionEnabled,
            )
        }
    }
}

@Composable
fun RewardToast(
    message: String,
    modifier: Modifier = Modifier,
    tone: FantasyTone = FantasyTone.Gold,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = tone, contentPadding = PaddingValues(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = MossGreen)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = WoodBrownDark)
        }
    }
}

@Composable
fun FantasyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: FantasyTone = FantasyTone.Gold,
    assetResId: Int = NestAssets.interfaceAsset("nid"),
    assetDescription: String? = null,
    loading: Boolean = false,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = tone) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tone.accent, strokeWidth = 3.dp)
                }
            } else {
                FantasyAssetBubble(assetResId = assetResId, contentDescription = assetDescription, size = 52.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        }
    }
}

@Composable
fun FantasyAssetBubble(
    assetResId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .border(1.3.dp, MagicViolet.copy(alpha = 0.72f), CircleShape)
                .padding(if (size >= 70.dp) 7.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        FantasySkinSurface(
            asset = FantasySkinAssets.iconFrameGold,
            fallbackBrush = taskodayGoldBrush(),
            modifier = Modifier.matchParentSize(),
        ) {}
        Image(
            painter = painterResource(id = assetResId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun InventoryLootCard(
    title: String,
    rarity: String,
    quantity: Int,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.interfaceAsset("crystal"),
    contentDescription: String = title,
    usageLabel: String? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = if (rarity == "rare") FantasyTone.Violet else FantasyTone.Moss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = contentDescription, size = 50.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = rarity, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                if (!usageLabel.isNullOrBlank()) {
                    Text(text = usageLabel, style = MaterialTheme.typography.bodySmall, color = MossGreen)
                }
            }
            Text(text = "x$quantity", style = MaterialTheme.typography.titleLarge, color = WoodBrownDark)
        }
    }
}
