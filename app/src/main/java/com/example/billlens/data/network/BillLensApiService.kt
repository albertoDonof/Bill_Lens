package com.example.billlens.data.network

import com.example.billlens.data.model.Expense
import retrofit2.Response
import retrofit2.http.*

interface BillLensApiService {
    @POST("expenses")
    suspend fun upsertExpense(@Body expense: Expense): Response<Expense>

    @GET("expenses")
    suspend fun syncExpenses(@Query("since") timestamp: String?): Response<List<Expense>>

    // Non lo useremo direttamente nel repo, ma è bene averlo
    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>
}