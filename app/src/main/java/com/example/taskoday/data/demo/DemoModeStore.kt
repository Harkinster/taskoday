package com.example.taskoday.data.demo

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.example.taskoday.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Opt-in demo switch for the offline visual pass.
 *
 * The switch is deliberately hard-gated by BuildConfig.DEBUG, so a release build can
 * never read or expose the demo data path.
 */
@Singleton
class DemoModeStore
    private constructor(
        private val preferences: SharedPreferences?,
        initialEnabled: Boolean,
    ) {
        private val _enabled =
            MutableStateFlow(
                BuildConfig.DEBUG && initialEnabled,
            )

        val enabledFlow: StateFlow<Boolean> = _enabled.asStateFlow()
        val isEnabled: Boolean get() = _enabled.value

        fun setEnabled(enabled: Boolean) {
            if (!BuildConfig.DEBUG) return
            preferences?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
            _enabled.value = enabled
        }

        @VisibleForTesting
        fun reset() = setEnabled(false)

        @Inject
        constructor(
            @ApplicationContext context: Context,
        ) : this(
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            initialEnabled = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false),
        )

        /** In-memory disabled instance used by plain JVM ViewModel tests. */
        constructor() : this(preferences = null, initialEnabled = false)

        private companion object {
            const val PREFERENCES_NAME = "taskoday_debug_preferences"
            const val KEY_ENABLED = "demo_mode_enabled"
        }
    }
