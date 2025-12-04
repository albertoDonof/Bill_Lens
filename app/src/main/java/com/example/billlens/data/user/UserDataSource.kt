package com.example.billlens.data.user

import com.example.billlens.data.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sorgente dati per le informazioni dell'utente.
 *
 * ATTENZIONE: Questa è un'implementazione FAKE in-memory per scopi di sviluppo.
 * In futuro, questa classe interagirà con un vero provider di autenticazione
 * come Firebase Authentication o le API di Google Sign-In.
 */
interface UserDataSource {
    /**
     * Espone uno stream con i dati dell'utente corrente.
     * Emette null se nessun utente è loggato.
     */
    val userData: Flow<UserData?>
}

@Singleton
class FakeUserDataSource @Inject constructor() : UserDataSource {

    // Un StateFlow privato che contiene i dati dell'utente (o null).
    // Inizializziamo con un utente "fake" per facilitare lo sviluppo della UI.
    private val _userData = MutableStateFlow<UserData?>(
        UserData(
            id = "fake-user-id-123",
            displayName = "Mario Rossi",
            email = "mario.rossi@example.com",
            profilePictureUrl = null, // Inserisci un URL qui per testare l'immagine
            isAuthenticated = true
        )
    )

    override val userData: Flow<UserData?> = _userData.asStateFlow()

    // In futuro, avrai metodi come:
    // suspend fun signInWithGoogle(token: String): UserData
    // suspend fun signOut() { _userData.value = null }
}


// da usare questa data source
@Singleton
class AuthDataSource @Inject constructor() : UserDataSource{
    override val userData: Flow<UserData?> = MutableStateFlow<UserData?>(
        value = TODO()
    ).asStateFlow()
}