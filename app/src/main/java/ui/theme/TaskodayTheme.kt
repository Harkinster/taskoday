package com.taskoday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TaskodayDarkScheme = darkColorScheme(
    primary = TaskodayColors.Cyan,
    secondary = TaskodayColors.NeonPurple,
    tertiary = TaskodayColors.Magenta,
    background = TaskodayColors.Night,
    surface = TaskodayColors.Panel,
    surfaceVariant = TaskodayColors.PanelAlt,
    onPrimary = TaskodayColors.Night,
    onSecondary = TaskodayColors.TextPrimary,
    onBackground = TaskodayColors.TextPrimary,
    onSurface = TaskodayColors.TextPrimary,
    onSurfaceVariant = TaskodayColors.TextSecondary
)

@Composable
fun TaskodayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TaskodayDarkScheme,
        typography = TaskodayTypography,
        content = content
    )
}
