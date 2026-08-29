package com.example.taskoday.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SessionEventBus
    @Inject
    constructor() {
        private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()
        private val unauthorizedEventSuppressions = AtomicInteger(0)

        fun notifyUnauthorized() {
            if (unauthorizedEventSuppressions.get() == 0) {
                _unauthorizedEvents.tryEmit(Unit)
            }
        }

        suspend fun <T> suppressUnauthorizedEvents(block: suspend () -> T): T {
            unauthorizedEventSuppressions.incrementAndGet()
            return try {
                block()
            } finally {
                unauthorizedEventSuppressions.decrementAndGet()
            }
        }
    }
