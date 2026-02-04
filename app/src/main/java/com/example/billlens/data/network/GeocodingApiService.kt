package com.example.billlens.data.network


import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Aggiungiamo un nuovo DTO per la risposta di reverse geocoding
data class ReverseGeokeoResponse(
    val results: List<ReverseGeokeoResult>
)

// Ogni risultato nella lista contiene "address_components"
data class ReverseGeokeoResult(
    @SerializedName("address_components") // Usiamo @SerializedName per mappare il nome JSON
    val addressComponents: GeokeoAddress
)

data class GeokeoAddress(
    val city: String?,
    val subdistrict: String?,
    val district: String?,
    val country: String?,
    val state: String?,
    val county: String?
)

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

    @GET("geocode/v1/reverse.php")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("api") apiKey: String
    ):Response<ReverseGeokeoResponse>
}