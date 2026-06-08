package com.example.taskoday.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiPathPrefixInterceptorTest {
    private val interceptor =
        ApiPathPrefixInterceptor("https://harkserv.ddns.net/taskoday-api/".toHttpUrl())

    @Test
    fun `adds api prefix after reverse proxy base path`() {
        val requestUrl =
            "https://harkserv.ddns.net/taskoday-api/children/1/progress".toHttpUrl()

        assertEquals(
            "https://harkserv.ddns.net/taskoday-api/api/v1/children/1/progress",
            interceptor.withApiPathPrefix(requestUrl).toString(),
        )
    }

    @Test
    fun `does not duplicate existing api prefix`() {
        val requestUrl =
            "https://harkserv.ddns.net/taskoday-api/api/v1/children/1/crystals".toHttpUrl()

        assertEquals(requestUrl, interceptor.withApiPathPrefix(requestUrl))
    }
}
