package com.example.billlens.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date
import java.util.UUID


@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val totalAmount: BigDecimal,
    val receiptDate: Date,
    val category: String,
    val notes: String?,
    val storeLocation: String?,
    // Verrà impostato un valore di default al momento della creazione dell'oggetto
    val insertionDate: Date,
    // Campi per la sincronizzazione
    var lastUpdated: Date,     // Quando è stato modificato l'ultima volta (locale o server)
    var isDeleted: Boolean = false, // Per i soft delete
    var isSynced: Boolean = false   // true se è allineato con il server
)