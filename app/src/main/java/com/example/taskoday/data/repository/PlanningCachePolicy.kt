package com.example.taskoday.data.repository

/** Explicit demo repositories bypass this policy; authenticated accounts never read local samples. */
internal fun planningCacheEntryVisible(hasRemoteSession: Boolean, isRemoteEntry: Boolean): Boolean =
    if (hasRemoteSession) isRemoteEntry else !isRemoteEntry
