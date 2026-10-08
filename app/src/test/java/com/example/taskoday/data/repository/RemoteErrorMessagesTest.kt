package com.example.taskoday.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class RemoteErrorMessagesTest {
    @Test
    fun `http failures are translated without exposing status codes`() {
        assertEquals("Cette action a changé. Actualise l’écran et réessaie.", httpError(409).toRemoteUserMessage())
        assertEquals("Le service rencontre un problème. Réessaie plus tard.", httpError(500).toRemoteUserMessage())
        assertEquals("Erreur locale.", httpError(418).toRemoteUserMessage("Erreur locale."))
        assertEquals("Session expirée.", httpError(401).toRemoteUserMessage())
    }

    private fun httpError(code: Int) =
        HttpException(Response.error<Unit>(code, "{}".toResponseBody("application/json".toMediaType())))
}
