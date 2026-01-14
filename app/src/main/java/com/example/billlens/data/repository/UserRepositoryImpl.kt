package com.example.billlens.data.repository

import com.example.billlens.data.local.UserLocalDataSource
import com.example.billlens.data.model.UserData
import com.example.billlens.data.user.FirebaseAuthDataSource
import com.example.billlens.data.user.UserDataSource
import com.google.android.gms.auth.api.identity.AuthorizationRequest

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione concreta di [UserRepository].
 * Per ora, si limita a passare i dati dalla UserDataSource.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val userLocalDataSource: UserLocalDataSource
) : UserRepository {

    override val userData: Flow<UserData?> = firebaseAuthDataSource.observeUser()


    override fun signOut() = firebaseAuthDataSource.signOut()

    // AGGIUNGI QUESTA IMPLEMENTAZIONE
    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        return firebaseAuthDataSource.getIdToken(forceRefresh)
    }

    override suspend fun clearLocalUser() {
        userLocalDataSource.clear()

    }

    override fun getAuthorizationRequest(): AuthorizationRequest {
        // Delega la creazione della richiesta al data source che sa come farlo.
        return firebaseAuthDataSource.getAuthorizationRequest()
    }


}