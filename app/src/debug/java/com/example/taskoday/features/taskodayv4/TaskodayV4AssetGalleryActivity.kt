package com.example.taskoday.features.taskodayv4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** DEBUG-only screen. It uses fixtures and never initializes repositories or network clients. */
class TaskodayV4AssetGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(33, 22, 47)
        window.navigationBarColor = android.graphics.Color.rgb(33, 22, 47)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF21162F),
                surface = Color(0xFF21162F),
                primary = Color(0xFFE4BB77),
                onBackground = Color(0xFFF1DEB9),
                onSurface = Color(0xFFF1DEB9),
            )) {
                TaskodayV4AssetGallery()
            }
        }
    }
}
