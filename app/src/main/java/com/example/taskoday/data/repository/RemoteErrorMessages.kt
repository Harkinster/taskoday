package com.example.taskoday.data.repository

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

internal fun Throwable.toRemoteUserMessage(fallback: String = "Erreur reseau."): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requete expiree."
        is HttpException ->
            when (code()) {
                401 -> "Session expiree."
                403 -> "Action non autorisee."
                else -> "Erreur API (${code()})."
            }

        is IOException -> fallback
        else -> message ?: fallback
    }
