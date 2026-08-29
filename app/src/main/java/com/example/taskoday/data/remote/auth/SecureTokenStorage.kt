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
        private val preferences: SharedPreferences? by lazy { createPreferences(context) }

        override fun getSessionTokens(): SessionTokens? {
            val preferences = preferences ?: return null
            val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
            return SessionTokens(
                accessToken = accessToken,
                refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() },
                accessExpiresAtEpochSeconds = preferences.readNullableLong(KEY_ACCESS_EXPIRES_AT),
                refreshExpiresAtEpochSeconds = preferences.readNullableLong(KEY_REFRESH_EXPIRES_AT),
            )
        }

        override fun saveSessionTokens(
            accessToken: String,
            refreshToken: String?,
            accessExpiresInSeconds: Int?,
            refreshExpiresInSeconds: Int?,
        ) {
            require(accessToken.isNotBlank()) { "Access token must not be blank." }
            val preferences = requireNotNull(preferences) { "Secure token storage is unavailable." }
            val nowEpochSeconds = System.currentTimeMillis() / 1_000L
            preferences
                .edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putNullableString(KEY_REFRESH_TOKEN, refreshToken?.takeIf { it.isNotBlank() })
                .putNullableLong(KEY_ACCESS_EXPIRES_AT, accessExpiresInSeconds?.let { nowEpochSeconds + it })
                .putNullableLong(KEY_REFRESH_EXPIRES_AT, refreshExpiresInSeconds?.let { nowEpochSeconds + it })
                .apply()
        }

        override fun getActiveChildId(): Long? {
            val preferences = preferences ?: return null
            if (!preferences.contains(KEY_ACTIVE_CHILD_ID)) return null
            return preferences.getLong(KEY_ACTIVE_CHILD_ID, -1L).takeIf { it > 0L }
        }

        override fun saveActiveChildId(childId: Long) {
            preferences?.edit()?.putLong(KEY_ACTIVE_CHILD_ID, childId)?.apply()
        }

        override fun clearActiveChildId() {
            preferences?.edit()?.remove(KEY_ACTIVE_CHILD_ID)?.apply()
        }

        override fun hasParentPin(): Boolean = !preferences?.getString(KEY_PARENT_PIN, null).isNullOrBlank()

        override fun saveParentPin(pin: String) {
            requireNotNull(preferences) { "Secure preference storage is unavailable." }
                .edit()
                .putString(KEY_PARENT_PIN, pin)
                .apply()
        }

        override fun verifyParentPin(pin: String): Boolean = preferences?.getString(KEY_PARENT_PIN, null) == pin

        override fun clear() {
            val preferences = preferences ?: return
            preferences
                .edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRES_AT)
                .remove(KEY_REFRESH_EXPIRES_AT)
                .remove(KEY_ACTIVE_CHILD_ID)
                .apply()
        }

        private fun createPreferences(context: Context): SharedPreferences? =
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
            }.getOrNull()

        private companion object {
            const val PREFS_NAME = "taskoday_secure_auth"
            const val KEY_ACCESS_TOKEN = "access_token"
            const val KEY_REFRESH_TOKEN = "refresh_token"
            const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
            const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
            const val KEY_ACTIVE_CHILD_ID = "active_child_id"
            const val KEY_PARENT_PIN = "parent_pin"
        }
    }

private fun SharedPreferences.readNullableLong(key: String): Long? =
    if (contains(key)) getLong(key, 0L).takeIf { it > 0L } else null

private fun SharedPreferences.Editor.putNullableString(
    key: String,
    value: String?,
): SharedPreferences.Editor = if (value == null) remove(key) else putString(key, value)

private fun SharedPreferences.Editor.putNullableLong(
    key: String,
    value: Long?,
): SharedPreferences.Editor = if (value == null) remove(key) else putLong(key, value)
