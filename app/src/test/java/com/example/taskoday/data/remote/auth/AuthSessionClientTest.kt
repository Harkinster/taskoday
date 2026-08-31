package com.example.taskoday.data.remote.auth

import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.Timeout
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthSessionClientTest {
    @Test
    fun `logout does not close Retrofit converted raw response`() {
        val callbackFailure = AtomicReference<Throwable?>()
        val callbackFinished = CountDownLatch(1)
        val rawResponse =
            okhttp3.Response
                .Builder()
                .request(Request.Builder().url("https://example.test/auth/logout").build())
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body(RawBodyThatMustNotBeClosed())
                .build()
        val logoutCall =
            AsyncFakeCall(
                Response.success<Unit>(null, rawResponse),
                callbackFailure,
                callbackFinished,
            )
        val client = RetrofitAuthSessionClient(FakeAuthSessionApi(logoutCall))

        client.logout("refresh-token")

        assertTrue(callbackFinished.await(2, TimeUnit.SECONDS))
        assertNull(callbackFailure.get())
    }
}

private class FakeAuthSessionApi(
    private val logoutCall: Call<Unit>,
) : AuthSessionApi {
    override fun refresh(payload: RefreshTokenRequestDto): Call<TokenResponseDto> = error("Not used")

    override fun logout(payload: RefreshTokenRequestDto): Call<Unit> = logoutCall
}

private class AsyncFakeCall<T>(
    private val response: Response<T>,
    private val callbackFailure: AtomicReference<Throwable?>,
    private val callbackFinished: CountDownLatch,
) : Call<T> {
    override fun enqueue(callback: Callback<T>) {
        Thread {
            try {
                callback.onResponse(this, response)
            } catch (throwable: Throwable) {
                callbackFailure.set(throwable)
            } finally {
                callbackFinished.countDown()
            }
        }.start()
    }

    override fun execute(): Response<T> = response

    override fun clone(): Call<T> = AsyncFakeCall(response, callbackFailure, callbackFinished)

    override fun isExecuted(): Boolean = false

    override fun cancel() = Unit

    override fun isCanceled(): Boolean = false

    override fun request(): Request = response.raw().request

    override fun timeout(): Timeout = Timeout.NONE
}

private class RawBodyThatMustNotBeClosed : ResponseBody() {
    override fun contentType(): MediaType? = null

    override fun contentLength(): Long = 0L

    override fun source(): BufferedSource = error("Converted raw response body must not be read or closed")
}
