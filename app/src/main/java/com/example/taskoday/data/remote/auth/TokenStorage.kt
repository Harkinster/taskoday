package com.example.taskoday.data.remote.auth

interface TokenStorage {
    fun getAccessToken(): String?

    fun saveAccessToken(token: String)

    fun getActiveChildId(): Long?

    fun saveActiveChildId(childId: Long)

    fun clearActiveChildId()

    fun clear()
}
