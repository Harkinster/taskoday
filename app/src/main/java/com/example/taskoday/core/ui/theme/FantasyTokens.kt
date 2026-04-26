package com.example.taskoday.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FantasyMetrics(
    val cardCorner: Dp = 18.dp,
    val cardCornerLarge: Dp = 22.dp,
    val chipCorner: Dp = 12.dp,
    val buttonCorner: Dp = 14.dp,
    val avatarRing: Dp = 2.dp,
    val cardStroke: Dp = 1.1.dp,
    val cardStrokeStrong: Dp = 1.4.dp,
    val sectionGap: Dp = 12.dp,
)

val LocalFantasyMetrics = staticCompositionLocalOf { FantasyMetrics() }

val MaterialTheme.fantasyMetrics: FantasyMetrics
    @Composable get() = LocalFantasyMetrics.current

fun taskodayBackgroundBrush(): Brush = Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom))

fun taskodayNeonBorderBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(NeonBorderStart, NeonBorderEnd),
    )

fun taskodayNeonProgressBrush(): Brush =
    Brush.horizontalGradient(
        colors = listOf(NeonPurple, CrystalBlue, NeonCyan),
    )
