package com.example.billlens.data.repository

import com.example.billlens.data.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Interfaccia per il Repository che gestisce l'accesso ai dati delle spese.
 * Definisce il contratto per l'accesso ai dati, astraendo le sorgenti dati
 * (locale, rete) dal resto dell'applicazione (es. ViewModels).
 */
interface ExpenseRepository {

    /**
     * Espone uno stream di tutte le spese visibili (non eliminate)
     * provenienti dalla sorgente di verità (il database locale).
     */
    val allExpenses: Flow<List<Expense>>

    /**
     * Salva una spesa (creazione o modifica). L'operazione avviene prima in locale
     * e viene marcata come da sincronizzare.
     * @param expense La spesa da salvare.
     */
    suspend fun saveExpense(expense: Expense)

    /**
     * Marca una spesa come eliminata (soft delete). L'operazione avviene in locale
     * e viene marcata come da sincronizzare.
     * @param expense La spesa da eliminare.
     */
    suspend fun deleteExpense(expense: Expense)

    suspend fun clearLocalData()

    /**
     * Orchestra la sincronizzazione dei dati con il server.
     * Invia le modifiche locali (upsert/delete) e riceve gli aggiornamenti
     * dal server (delta sync).
     */
    suspend fun syncWithServer()
}