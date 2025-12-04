package com.example.billlens.data.model

/**
 * Rappresenta i dati essenziali di un utente autenticato.
 *
 * @param id L'identificativo unico dell'utente (es. da Firebase Auth).
 * @param displayName Il nome visualizzato dall'utente.
 * @param email L'email dell'utente.
 * @param profilePictureUrl L'URL dell'immagine del profilo.
 * @param isAuthenticated Un flag per indicare se l'utente è attualmente autenticato.
 */
data class UserData(
    val id: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUrl: String?,
    val isAuthenticated: Boolean = false
)