package com.example.taskoday.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat

private val TaskodayFantasyColors: ColorScheme =
    lightColorScheme(
        primary = EmberOrange,
        onPrimary = ParchmentLight,
        primaryContainer = LogoGoldLight,
        onPrimaryContainer = WoodBrownDark,
        secondary = MagicViolet,
        onSecondary = ParchmentLight,
        secondaryContainer = RoyalPurpleSoft.copy(alpha = 0.28f),
        onSecondaryContainer = WoodBrownDark,
        tertiary = MossGreen,
        onTertiary = ParchmentLight,
        tertiaryContainer = MossGreenSoft.copy(alpha = 0.34f),
        onTertiaryContainer = WoodBrownDark,
        background = ParchmentLight,
        onBackground = InkBrown,
        surface = SurfacePanel,
        onSurface = InkBrown,
        surfaceVariant = ParchmentCream,
        onSurfaceVariant = InkMuted,
        outline = SoftGold,
        error = DangerGlow,
        onError = ParchmentLight,
    )

private val TaskodayFantasyDarkColors: ColorScheme = TaskodayFantasyColors
private val TaskodayFantasyLightColors: ColorScheme = TaskodayFantasyColors

@Composable
@Suppress("DEPRECATION")
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
            window.navigationBarColor = "#3B2417".toColorInt()
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
