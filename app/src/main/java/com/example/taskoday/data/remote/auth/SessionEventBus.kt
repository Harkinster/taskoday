package com.example.taskoday.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SessionEventBus
    @Inject
    constructor() {
        private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

        fun notifyUnauthorized() {
            _unauthorizedEvents.tryEmit(Unit)
        }
    }
