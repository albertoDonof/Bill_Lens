package com.example.billlens.data.repository

import com.example.billlens.data.model.UserData
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import kotlinx.coroutines.flow.Flow

/**
 * Interfaccia per il Repository che gestisce i dati dell'utente.
 * Astrae la sorgente dati dell'utente (Firebase, Google, cache locale) dal resto dell'app.
 */
interface UserRepository {

    /**
     * Espone uno stream di dati dell'utente corrente.
     * Il valore sarà null se l'utente non è autenticato.
     */
    val userData: Flow<UserData?>

    fun signOut()

    // AGGIUNGI QUESTA RIGA
    suspend fun getIdToken(forceRefresh: Boolean = false): String?

    suspend fun clearLocalUser()

    // Definisce che il repository deve essere in grado di fornire una richiesta di autorizzazione.
    fun getAuthorizationRequest(): AuthorizationRequest
}