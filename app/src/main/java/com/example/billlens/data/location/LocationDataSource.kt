package com.example.billlens.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Ottiene un singolo aggiornamento della posizione corrente.
     * È efficiente perché richiede la posizione una sola volta e poi si ferma.
     * Usa un Flow per gestire la natura asincrona della richiesta.
     */
    @SuppressLint("MissingPermission") // Il permesso viene controllato nella UI prima di chiamare
    fun getCurrentLocation(): Flow<LatLng?> {
        return callbackFlow {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        // Appena riceviamo una posizione valida, la inviamo e chiudiamo il flow.
                        trySend(LatLng(location.latitude, location.longitude))
                        close() // Chiude il flow per evitare ulteriori aggiornamenti
                    }
                }
            }

            // Richiedi aggiornamenti della posizione
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Quando il flow viene cancellato, rimuovi il callback
            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // --- NUOVA FUNZIONE WRAPPER CON TIMEOUT ---
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocationWithTimeout(timeoutMillis: Long = 10000L): LatLng? {
        return withTimeoutOrNull(timeoutMillis) {
            // Se la posizione non arriva entro 10 secondi, questo blocco restituirà null.
            getCurrentLocation().first()
        }
    }
}