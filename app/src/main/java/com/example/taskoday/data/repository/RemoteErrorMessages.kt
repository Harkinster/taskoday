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
                401 -> "Session expirée."
                403 -> "Action non autorisée."
                404 -> "Élément introuvable."
                409 -> "Cette action a changé. Actualise l’écran et réessaie."
                422 -> "Certaines informations sont invalides."
                in 500..599 -> "Le service rencontre un problème. Réessaie plus tard."
                else -> fallback
            }

        is IOException -> fallback
        else -> message ?: fallback
    }
