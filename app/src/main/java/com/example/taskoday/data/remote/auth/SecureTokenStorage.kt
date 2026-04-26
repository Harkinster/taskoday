package com.example.taskoday.data.remote.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TokenStorage {
        private val preferences: SharedPreferences by lazy { createPreferences(context) }

        override fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

        override fun saveAccessToken(token: String) {
            preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        }

        override fun getActiveChildId(): Long? {
            if (!preferences.contains(KEY_ACTIVE_CHILD_ID)) return null
            return preferences.getLong(KEY_ACTIVE_CHILD_ID, -1L).takeIf { it > 0L }
        }

        override fun saveActiveChildId(childId: Long) {
            preferences.edit().putLong(KEY_ACTIVE_CHILD_ID, childId).apply()
        }

        override fun clearActiveChildId() {
            preferences.edit().remove(KEY_ACTIVE_CHILD_ID).apply()
        }

        override fun clear() {
            preferences
                .edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_ACTIVE_CHILD_ID)
                .apply()
        }

        private fun createPreferences(context: Context): SharedPreferences =
            runCatching {
                val masterKey =
                    MasterKey
                        .Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrElse {
                // Fallback keeps app usable on environments where secure storage init fails.
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

        private companion object {
            const val PREFS_NAME = "taskoday_secure_auth"
            const val KEY_ACCESS_TOKEN = "access_token"
            const val KEY_ACTIVE_CHILD_ID = "active_child_id"
        }
    }
