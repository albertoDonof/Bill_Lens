package com.example.billlens.data.repository

import com.example.billlens.data.model.UserData
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
}