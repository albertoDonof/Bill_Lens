package com.example.billlens.data.user

import android.content.Context
import android.util.Log
import com.example.billlens.R
import com.example.billlens.data.local.UserLocalDataSource
import com.example.billlens.data.model.UserData
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.android.gms.common.api.Scope




/**
 * Implementazione reale di [UserDataSource] che utilizza Firebase Authentication.
 */
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val local: UserLocalDataSource,
    @ApplicationContext private val context: Context
) {

    private fun firebaseUserToModel(u: FirebaseUser?) = u?.let {
        UserData(
            id = it.uid,
            displayName = it.displayName,
            email = it.email,
            profilePictureUrl = it.photoUrl?.toString(),
            isAuthenticated = true
        )
    }

    fun getCurrentUser(): UserData? =
        firebaseUserToModel(auth.currentUser)



    /**
     * Prepara una richiesta di autorizzazione per gli scope di Google Sheets e Drive.
     * Questo metodo crea l'oggetto richiesta che la UI userà per lanciare la schermata di consenso.
     */
    fun getAuthorizationRequest(): AuthorizationRequest {
        val serverClientId = context.getString(R.string.default_web_client_id)
        // Instead of a list of Strings, create a list of Scope objects.
        val scopes = listOf(
            Scope("https://www.googleapis.com/auth/spreadsheets"),
            Scope("https://www.googleapis.com/auth/drive.file")
        )

        return AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .build()
    }


    /**
     * Recupera il Token ID di Firebase per l'utente corrente.
     * Utile per autenticare le chiamate verso il server Flask.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return try {
            val user = auth.currentUser
            // user.getIdToken restituisce un Task, usiamo .await() (richiede kotlinx-coroutines-play-services)
            val result = user?.getIdToken(forceRefresh)?.await()
            result?.token
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error in retrieving the token", e)
            null
        }
    }

    fun signOut() = auth.signOut()

    fun observeUser(): Flow<UserData?> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO)
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val model = firebaseUserToModel(firebaseAuth.currentUser)
            trySend(model).isSuccess
            scope.launch {
                if (model != null) local.save(model) else local.clear()
            }
        }
        auth.addAuthStateListener(listener)
        trySend(firebaseUserToModel(auth.currentUser)).isSuccess
        auth.currentUser?.let { scope.launch { local.save(firebaseUserToModel(it)!!) } }

        awaitClose {
            scope.cancel()
            auth.removeAuthStateListener(listener)
        }
    }
}