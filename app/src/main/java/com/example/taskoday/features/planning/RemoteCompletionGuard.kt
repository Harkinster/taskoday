package com.example.taskoday.features.planning

internal fun shouldApplyLocalCompletionAfterRemoteAttempt(
    remoteCompletionRequired: Boolean,
    remoteCompletionSucceeded: Boolean,
): Boolean = !remoteCompletionRequired || remoteCompletionSucceeded
