package com.example.taskoday.data.repository

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

fun Throwable.toRemoteUserMessage(fallback: String = "Erreur réseau."): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requête expirée."
        is HttpException ->
            when (code()) {
                400 -> "Cette action n'est pas possible pour le moment."
                401 -> "Session expiree."
                403 -> "Action non autorisée."
                404 -> "Element introuvable."
                422 -> "Certaines informations sont invalides."
                else -> "Erreur API (${code()})."
            }

        is IOException -> fallback
        else -> message ?: fallback
    }
