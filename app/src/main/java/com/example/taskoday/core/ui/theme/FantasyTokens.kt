package com.example.taskoday.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FantasyMetrics(
    val cardCorner: Dp = 16.dp,
    val cardCornerLarge: Dp = 20.dp,
    val chipCorner: Dp = 10.dp,
    val buttonCorner: Dp = 12.dp,
    val avatarRing: Dp = 2.dp,
    val cardStroke: Dp = 1.2.dp,
    val cardStrokeStrong: Dp = 1.8.dp,
    val sectionGap: Dp = 12.dp,
)

val LocalFantasyMetrics = staticCompositionLocalOf { FantasyMetrics() }

val MaterialTheme.fantasyMetrics: FantasyMetrics
    @Composable get() = LocalFantasyMetrics.current

fun taskodayBackgroundBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(SkyGold, ParchmentLight, SkyBlueSoft, ValleyMist),
    )

fun taskodayWorldBackgroundBrush(): Brush =
    Brush.verticalGradient(
        colors =
            listOf(
                BackgroundTop,
                Color(0xFF21143A),
                Color(0xFF171025),
                BackgroundBottom,
            ),
    )

fun taskodayParchmentBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(ParchmentLight, ParchmentCream),
    )

fun taskodayWoodPanelBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(Color(0xFF4A3158), Color(0xFF281A3B)),
    )

fun taskodayCrestBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(RoyalPurpleDark, RoyalPurple, ShieldPurple),
    )

fun taskodayGoldBrush(): Brush =
    Brush.verticalGradient(
        colors = listOf(LogoGoldLight, LogoGold),
    )

fun taskodayLeafAccentBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(MossGreenSoft, MossGreen, SoftGold),
    )

fun taskodayCrystalBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(CrystalBlueLight, CrystalBlue, MagicVioletSoft),
    )

fun taskodayNeonBorderBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(LogoGoldLight, SoftGold, MagicVioletSoft),
    )

fun taskodayNeonProgressBrush(): Brush =
    Brush.horizontalGradient(
        colors = listOf(EmberOrange, SoftGold, MossGreen),
    )
