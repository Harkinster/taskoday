package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.taskoday.core.ui.theme.NestNightBlue
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.ParchmentShadow
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.SoftGoldPale
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing

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

@Composable
fun FantasyCard(
    modifier: Modifier = Modifier,
    tone: FantasyTone = FantasyTone.Gold,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 72.dp)
                .clip(shape)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(ParchmentLight.copy(alpha = 0.98f), ParchmentCream.copy(alpha = 0.96f)),
                        ),
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(tone.accent.copy(alpha = 0.72f), ParchmentShadow)),
                    shape = shape,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(tone.soft, Color.Transparent),
                                radius = 460f,
                            ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(9.dp),
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
    val shape = RoundedCornerShape(14.dp)
    val buttonModifier = modifier.defaultMinSize(minHeight = 48.dp)
    val textContent: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    when (style) {
        FantasyButtonStyle.Filled ->
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                shape = shape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = ParchmentLight,
                        disabledContainerColor = ParchmentShadow,
                        disabledContentColor = InkMuted,
                    ),
            ) {
                textContent()
            }

        FantasyButtonStyle.Outline ->
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                shape = shape,
                border = BorderStroke(1.2.dp, SoftGold),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = ParchmentLight.copy(alpha = 0.68f),
                        contentColor = WoodBrownDark,
                        disabledContentColor = InkMuted,
                    ),
            ) {
                textContent()
            }

        FantasyButtonStyle.Quiet ->
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                shape = shape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MossGreen.copy(alpha = 0.18f),
                        contentColor = MossGreen,
                        disabledContainerColor = ParchmentShadow,
                        disabledContentColor = InkMuted,
                    ),
            ) {
                textContent()
            }
    }
}

@Composable
fun FantasyHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    avatarInitials: String = "AB",
    assetResId: Int = NestAssets.NestBackground.resId,
    assetDescription: String = "Le Nid",
    onAvatarClick: () -> Unit = {},
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = assetDescription, size = 64.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = 3,
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
                    .background(Brush.horizontalGradient(listOf(EmberOrange, SoftGold, MossGreen))),
        )
    }
}

@Composable
fun NestStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.Flameche.resId,
    tone: FantasyTone = FantasyTone.Gold,
) {
    FantasyCard(modifier = modifier, tone = tone, contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = label, size = 42.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
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
    assetResId: Int = NestAssets.EggSolarSleeping.resId,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Gold) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = title, size = 58.dp)
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
    }
}

@Composable
fun DragonCard(
    title: String,
    stage: String,
    nextStep: String,
    modifier: Modifier = Modifier,
    assetResId: Int = NestAssets.DragonEmberBaby.resId,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Ember) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = title, size = 64.dp)
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
            }
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
    assetResId: Int = NestAssets.Flameche.resId,
    onMakeWish: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Violet) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = "Flammèche", size = 48.dp)
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
    assetResId: Int = NestAssets.ScrollApproved.resId,
    onUse: (() -> Unit)? = null,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Wood) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = "Parchemin", size = 54.dp)
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
    assetResId: Int = NestAssets.ChestCommon.resId,
) {
    val progress = if (pointsRequired <= 0) 0f else points.toFloat() / pointsRequired.toFloat()
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = FantasyTone.Wood) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = "Coffre", size = 58.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = "Coffre du Gardien", style = MaterialTheme.typography.titleMedium, color = WoodBrownDark)
                Text(text = "$points/$pointsRequired points coffre", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                FantasyProgressBar(progress = progress)
                Text(text = "$unopenedChests coffre en attente", style = MaterialTheme.typography.bodySmall, color = MossGreen)
            }
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
    assetResId: Int = NestAssets.NestBackground.resId,
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
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(SoftGoldPale, ParchmentCream)))
                .border(1.dp, SoftGold.copy(alpha = 0.9f), CircleShape)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = assetResId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth(),
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
    assetResId: Int = NestAssets.Crystal.resId,
) {
    FantasyCard(modifier = modifier.fillMaxWidth(), tone = if (rarity == "rare") FantasyTone.Violet else FantasyTone.Moss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FantasyAssetBubble(assetResId = assetResId, contentDescription = title, size = 50.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WoodBrownDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = rarity, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            Text(text = "x$quantity", style = MaterialTheme.typography.titleLarge, color = WoodBrownDark)
        }
    }
}
