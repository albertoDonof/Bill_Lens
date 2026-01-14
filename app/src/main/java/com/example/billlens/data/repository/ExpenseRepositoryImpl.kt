package com.example.billlens.data.repository

import android.util.Log
import com.example.billlens.data.local.LocalDataSource
import com.example.billlens.data.model.Expense
import com.example.billlens.data.network.NetworkDataSource
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione concreta di [ExpenseRepository].
 * Orchestra le operazioni tra la sorgente dati locale (Room) e quella remota (Retrofit).
 * Segue un'architettura offline-first.
 *
 * @param localDataSource La sorgente dati locale (Single Source of Truth).
 * @param networkDataSource La sorgente dati remota per la sincronizzazione.
 */
@Singleton // Hilt gestirà questa classe come un singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val networkDataSource: NetworkDataSource,
    private val userRepository: UserRepository
) : ExpenseRepository {

    // La UI osserva SOLO i dati locali visibili
    override val allExpenses: Flow<List<Expense>> = localDataSource.allVisibleExpenses

    // L'azione dell'utente (creazione/modifica) è PRIMA locale
    override suspend fun saveExpense(expense: Expense) {
        val now = Date()
        val expenseToSave = expense.copy(
            lastUpdated = now,
            isSynced = false // Marcalo come non sincronizzato
        )
        localDataSource.upsertExpense(expenseToSave)

        syncWithServer()
    }

    // La cancellazione è un soft-delete locale
    override suspend fun deleteExpense(expense: Expense) {
        val expenseToDelete = expense.copy(
            isDeleted = true,
            lastUpdated = Date(),
            isSynced = false // Marcalo come da sincronizzare
        )
        localDataSource.upsertExpense(expenseToDelete)
    }

    override suspend fun clearLocalData() {
        localDataSource.deleteAllExpenses()
    }

    // Il cuore della sincronizzazione
    override suspend fun syncWithServer() {
        try {
            val idToken = userRepository.getIdToken()
            if (idToken == null) {
                Log.e("Sync", "Sincronizzazione fallita: Token non disponibile")
                return
            }
            // FASE A: Invia le modifiche locali al server
            val unsyncedExpenses = localDataSource.getUnsyncedExpenses()
            unsyncedExpenses.forEach { localExpense ->
                val response = networkDataSource.upsertExpense(idToken,localExpense)
                if (response.isSuccessful) {
                    // Se il server ha accettato, aggiorna l'elemento locale come "sincronizzato"
                    val syncedExpense = localExpense.copy(isSynced = true)
                    localDataSource.upsertExpense(syncedExpense)
                }
            }

            // FASE B: Ricevi le modifiche dal server (Delta Sync)
            val lastTimestamp = localDataSource.getLatestUpdateTimestamp()
            // Formattatore per lo standard ISO 8601 richiesto dal server
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val since = lastTimestamp?.let { formatter.format(it) }

            val response = networkDataSource.syncExpenses(idToken,since)
            if (response.isSuccessful) {
                response.body()?.let { remoteExpenses ->
                    // Marca tutti gli elementi ricevuti come sincronizzati e salvali
                    val syncedRemoteExpenses = remoteExpenses.map { it.copy(isSynced = true) }
                    localDataSource.upsertAll(syncedRemoteExpenses)
                }
            }
        } catch (e: Exception) {
            // Gestisci l'errore di rete (es. log)
            // L'app continuerà a funzionare con i dati locali
            println("Sync failed: ${e.message}")
        }
    }
}