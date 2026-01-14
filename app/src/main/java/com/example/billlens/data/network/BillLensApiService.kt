package com.example.billlens.data.network

import com.example.billlens.data.model.Expense
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// New DTO for the request body
data class ExportSheetRequest(
    @SerializedName("accessToken")
    val accessToken: String
)


data class ExportResponse(
    val status: String,
    val sheetUrl: String?, // Deve coincidere con la chiave in Flask
    val sheetTitle: String?,
    val sharedWith: String?,
    val message: String? = null
)

interface BillLensApiService {
    @POST("expenses")
    suspend fun upsertExpense(@Header("Authorization") token: String,@Body expense: Expense): Response<Expense>

    @GET("expenses")
    suspend fun syncExpenses(@Header("Authorization") token: String,@Query("since") timestamp: String?): Response<List<Expense>>

    // Non lo useremo direttamente nel repo, ma è bene averlo
    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Header("Authorization") token: String,@Path("id") id: String): Response<Unit>

    @POST("api/export/sheets")
    suspend fun exportToSheets(
        @Header("Authorization") token: String,
        @Body body: ExportSheetRequest
    ): Response<ExportResponse>
}