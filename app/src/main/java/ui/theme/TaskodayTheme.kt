package com.taskoday.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.taskoday.core.ui.theme.FantasyMetrics
import com.example.taskoday.core.ui.theme.LocalFantasyMetrics
import com.example.taskoday.core.ui.theme.LocalTaskodaySpacing
import com.example.taskoday.core.ui.theme.TaskodaySpacing

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
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalTaskodaySpacing provides TaskodaySpacing(),
        LocalFantasyMetrics provides FantasyMetrics(),
    ) {
        MaterialTheme(
            colorScheme = TaskodayDarkScheme,
            typography = TaskodayTypography,
            content = content,
        )
    }
}
