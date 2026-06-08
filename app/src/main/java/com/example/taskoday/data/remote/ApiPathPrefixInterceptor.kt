package com.example.taskoday.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class ApiPathPrefixInterceptor(
    private val baseUrl: HttpUrl,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val apiUrl = withApiPathPrefix(request.url)

        return chain.proceed(request.newBuilder().url(apiUrl).build())
    }

    internal fun withApiPathPrefix(originalUrl: HttpUrl): HttpUrl {
        val basePath = baseUrl.encodedPath
        val apiPath = "${basePath}api/v1/"

        if (!originalUrl.encodedPath.startsWith(basePath) || originalUrl.encodedPath.startsWith(apiPath)) {
            return originalUrl
        }

        val relativePath = originalUrl.encodedPath.removePrefix(basePath)
        return originalUrl
            .newBuilder()
            .encodedPath("$apiPath$relativePath")
            .build()
    }
}
