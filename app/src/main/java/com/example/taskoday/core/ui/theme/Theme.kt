package com.example.taskoday.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat

private val TaskodayFantasyDarkColors: ColorScheme =
    darkColorScheme(
        primary = NeonCyan,
        onPrimary = NightBlue950,
        primaryContainer = NightBlue800,
        onPrimaryContainer = StarWhite,
        secondary = NebulaViolet,
        onSecondary = StarWhite,
        secondaryContainer = NightBlue850,
        onSecondaryContainer = StarWhite,
        tertiary = NeonBlue,
        onTertiary = StarWhite,
        tertiaryContainer = NightBlue900,
        onTertiaryContainer = StarWhite,
        background = NightBlue950,
        onBackground = StarWhite,
        surface = SurfaceGlass,
        onSurface = StarWhite,
        surfaceVariant = SurfacePanelAlt,
        onSurfaceVariant = TextMuted,
        outline = OutlineGlow,
        error = DangerGlow,
        onError = NightBlue950,
    )

private val TaskodayFantasyLightColors: ColorScheme = TaskodayFantasyDarkColors

@Composable
fun TaskodayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> TaskodayFantasyDarkColors
            else -> TaskodayFantasyLightColors
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = "#00000000".toColorInt()
            window.navigationBarColor = "#060C2B".toColorInt()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalTaskodaySpacing provides TaskodaySpacing(),
        LocalFantasyMetrics provides FantasyMetrics(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TaskodayTypography,
            content = content,
        )
    }
}
