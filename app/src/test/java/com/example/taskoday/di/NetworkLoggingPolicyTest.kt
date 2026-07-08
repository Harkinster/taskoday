package com.example.taskoday.di

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLoggingPolicyTest {
    @Test
    fun `debug logging never logs request or response bodies`() {
        assertEquals(HttpLoggingInterceptor.Level.BASIC, taskodayHttpLogLevel(isDebug = true))
    }

    @Test
    fun `non debug logging is disabled`() {
        assertEquals(HttpLoggingInterceptor.Level.NONE, taskodayHttpLogLevel(isDebug = false))
    }

    @Test
    fun `authorization header is marked sensitive`() {
        assertTrue(sensitiveHttpHeaders.any { it.equals("Authorization", ignoreCase = true) })
    }
}
