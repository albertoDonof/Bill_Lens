package com.example.billlens.data.repository

import com.example.billlens.data.model.UserData
import com.example.billlens.data.user.UserDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione concreta di [UserRepository].
 * Per ora, si limita a passare i dati dalla UserDataSource.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDataSource: UserDataSource
) : UserRepository {

    override val userData: Flow<UserData?> = userDataSource.userData

    // In futuro, qui avrai la logica per il sign-in, sign-out, ecc.
    // Esempio:
    // suspend fun signInWithGoogle(token: String) {
    //     val user = userDataSource.signInWithGoogle(token)
    //     // Puoi salvare i dati utente in un database locale qui se necessario
    // }
}