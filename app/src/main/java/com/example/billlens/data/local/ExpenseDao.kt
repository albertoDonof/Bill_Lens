package com.example.billlens.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.billlens.data.model.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {

//    @Query("SELECT * FROM expenses ORDER BY insertionDate DESC")
//    fun getAllExpenses(): Flow<List<Expense>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertExpense(expense: Expense)
//
//    @Delete
//    fun delete(expense: Expense)
//

    // (Upsert combina Insert e Update)
    @Upsert
    suspend fun upsertExpense(expense: Expense)

    @Upsert
    suspend fun upsertAll(expenses: List<Expense>)

    // Ora lo stream non mostrerà gli elementi marcati come eliminati
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY receiptDate DESC")
    fun getAllVisibleExpenses(): Flow<List<Expense>>

    // Ottiene l'ultima data di modifica per la sincronizzazione (delta sync)
    @Query("SELECT MAX(lastUpdated) FROM expenses")
    suspend fun getLatestUpdateTimestamp(): Date?

    // Ottiene tutti gli elementi non sincronizzati per inviarli al server
    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}