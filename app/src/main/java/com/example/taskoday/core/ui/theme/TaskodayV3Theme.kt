package com.example.taskoday.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Palette for the light, utility-focused Taskoday V3 surfaces. */
@Immutable
data class TaskodayV3Palette(
    val brandPrimary: Color = Color(0xFF6F5AE8),
    val brandDeep: Color = Color(0xFF3E2F73),
    val lavender: Color = Color(0xFFEEE9FF),
    val lavenderMist: Color = Color(0xFFF6F2FF),
    val background: Color = Color(0xFFF7F4EF),
    val surface: Color = Color(0xFFFFFDFC),
    val surfaceSecondary: Color = Color(0xFFF1EEE8),
    val border: Color = Color(0xFFD9D3E6),
    val textPrimary: Color = Color(0xFF1F2430),
    val textSecondary: Color = Color(0xFF5F6673),
    val textOnPrimary: Color = Color(0xFFFFFFFF),
    val success: Color = Color(0xFF4FA36C),
    val successInk: Color = Color(0xFF28643E),
    val successSoft: Color = Color(0xFFDDF0E3),
    val info: Color = Color(0xFF7CB7D8),
    val infoInk: Color = Color(0xFF35627A),
    val infoSoft: Color = Color(0xFFE4F3FB),
    val mission: Color = Color(0xFFE07A5F),
    val missionInk: Color = Color(0xFF8D3A28),
    val missionSoft: Color = Color(0xFFFBE6DF),
    val quest: Color = Color(0xFFD8A63F),
    val questInk: Color = Color(0xFF74520B),
    val questSoft: Color = Color(0xFFF8EDC8),
    val routine: Color = Color(0xFF5FA8A3),
    val routineInk: Color = Color(0xFF225854),
    val routineSoft: Color = Color(0xFFDDF1EF),
    val error: Color = Color(0xFFC85C5C),
    val errorInk: Color = Color(0xFF8D3636),
    val errorSoft: Color = Color(0xFFF9E2E2),
)

val TaskodayV3Colors = TaskodayV3Palette()

private val TaskodayV3Typography = TaskodayTypography.copy(
    headlineLarge = TaskodayTypography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TaskodayTypography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TaskodayTypography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TaskodayTypography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TaskodayTypography.titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TaskodayTypography.titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TaskodayTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TaskodayTypography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TaskodayTypography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TaskodayTypography.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TaskodayTypography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TaskodayTypography.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
)

val LocalTaskodayV3Palette = staticCompositionLocalOf { TaskodayV3Colors }

val MaterialTheme.taskodayV3Palette: TaskodayV3Palette
    @Composable get() = LocalTaskodayV3Palette.current

private val TaskodayV3ColorScheme: ColorScheme
    get() = lightColorScheme(
        primary = TaskodayV3Colors.brandPrimary,
        onPrimary = TaskodayV3Colors.textOnPrimary,
        primaryContainer = TaskodayV3Colors.lavender,
        onPrimaryContainer = TaskodayV3Colors.brandDeep,
        secondary = TaskodayV3Colors.brandDeep,
        onSecondary = TaskodayV3Colors.textOnPrimary,
        secondaryContainer = TaskodayV3Colors.lavenderMist,
        onSecondaryContainer = TaskodayV3Colors.brandDeep,
        tertiary = TaskodayV3Colors.routine,
        onTertiary = TaskodayV3Colors.textOnPrimary,
        background = TaskodayV3Colors.background,
        onBackground = TaskodayV3Colors.textPrimary,
        surface = TaskodayV3Colors.surface,
        onSurface = TaskodayV3Colors.textPrimary,
        surfaceVariant = TaskodayV3Colors.surfaceSecondary,
        onSurfaceVariant = TaskodayV3Colors.textSecondary,
        outline = TaskodayV3Colors.border,
        error = TaskodayV3Colors.error,
        onError = TaskodayV3Colors.textOnPrimary,
    )

/**
 * Opt-in theme for Taskoday utility screens. It deliberately does not alter
 * [TaskodayTheme], window system bars, or Chronodria's fantasy composition locals.
 */
@Composable
fun TaskodayV3UtilityTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as? Activity)?.window
    val insetsController = remember(window, view) {
        window?.let { WindowCompat.getInsetsController(it, view) }
    }

    DisposableEffect(window, insetsController) {
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val previousNavigationBarContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window?.isNavigationBarContrastEnforced else null
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        val previousLightNavigationBars = insetsController?.isAppearanceLightNavigationBars
        onDispose {
            if (window != null) {
                previousStatusBarColor?.let { window.statusBarColor = it }
                previousNavigationBarColor?.let { window.navigationBarColor = it }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    previousNavigationBarContrast?.let { window.isNavigationBarContrastEnforced = it }
                }
            }
            previousLightStatusBars?.let { insetsController?.isAppearanceLightStatusBars = it }
            previousLightNavigationBars?.let { insetsController?.isAppearanceLightNavigationBars = it }
        }
    }
    SideEffect {
        if (window != null && !view.isInEditMode) {
            val systemBarColor = TaskodayV3Colors.background.toArgb()
            window.statusBarColor = systemBarColor
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                window.navigationBarColor = systemBarColor
            } else {
                window.navigationBarColor = systemBarColor
                window.isNavigationBarContrastEnforced = false
            }
            insetsController?.isAppearanceLightStatusBars = true
            insetsController?.isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(LocalTaskodayV3Palette provides TaskodayV3Colors) {
        MaterialTheme(
            colorScheme = TaskodayV3ColorScheme,
            typography = TaskodayV3Typography,
            content = content,
        )
    }
}
