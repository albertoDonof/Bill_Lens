package com.example.billlens.data.network

import com.example.billlens.data.model.Expense
import javax.inject.Inject

// Questa classe wrappa le chiamate API
class NetworkDataSource @Inject constructor(
    private val apiService: BillLensApiService
) {
    suspend fun syncExpenses(token: String,lastTimestamp: String?) = apiService.syncExpenses("Bearer $token",lastTimestamp)
    suspend fun upsertExpense(token: String, expense: Expense) = apiService.upsertExpense("Bearer $token",expense)

    suspend fun exportToSheets(firebaseIdToken: String, googleAccessToken: String) =
        apiService.exportToSheets(
            token = "Bearer $firebaseIdToken",
            body = ExportSheetRequest(accessToken = googleAccessToken)
        )
}