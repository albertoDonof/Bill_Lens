package com.example.billlens.data.network


import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// DTO per la risposta di GeoKeo
data class GeokeoResponse(val results: List<GeokeoResult>)
data class GeokeoResult(val geometry: GeokeoGeometry)
data class GeokeoGeometry(val location: GeokeoLocation)
data class GeokeoLocation(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lng") val longitude: Double
)

// Interfaccia Retrofit
interface GeocodingApiService {
    @GET("geocode/v1/search.php")
    suspend fun geokeoAddress(
        @Query("q") address: String,
        @Query("api") apiKey: String
    ): Response<GeokeoResponse>
}