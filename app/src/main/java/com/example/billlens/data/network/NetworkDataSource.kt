package com.example.billlens.data.network

import com.example.billlens.data.model.Expense
import javax.inject.Inject

// Questa classe wrappa le chiamate API
class NetworkDataSource @Inject constructor(
    private val apiService: BillLensApiService
) {
    suspend fun syncExpenses(lastTimestamp: String?) = apiService.syncExpenses(lastTimestamp)
    suspend fun upsertExpense(expense: Expense) = apiService.upsertExpense(expense)
}