package com.example.taskoday.data.remote.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEventBusTest {
    @Test
    fun `worker suppression prevents navigation event then restores normal events`() =
        runTest {
            val eventBus = SessionEventBus()
            var eventCount = 0
            val collection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    eventBus.unauthorizedEvents.collect { eventCount += 1 }
                }

            eventBus.suppressUnauthorizedEvents {
                eventBus.notifyUnauthorized()
            }
            runCurrent()
            assertEquals(0, eventCount)

            eventBus.notifyUnauthorized()
            runCurrent()
            assertEquals(1, eventCount)
            collection.cancel()
        }
}
