package com.example.billlens.domain.expenses

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.material3.rememberTopAppBarState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.BuildConfig
import com.example.billlens.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.billlens.data.model.Expense
import com.example.billlens.ui.scan.ReceiptDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.first
import com.example.billlens.data.location.LocationDataSource
import com.example.billlens.data.network.GeocodingApiService
import com.google.android.gms.maps.model.LatLng

data class ExpenseValidationErrors(
    val totalError: String? = null,
    val dateError: String? = null,
    val notesError: String? = null
    // Aggiungi altri campi se necessario (es. locationError)
)


data class AddExpenseUiState(
    val store: String? = null,
    val location : String? = null,
    val date: String? = null,
    val total: String? = null,
    val notes: String? = null,
    val category: String = ExpenseCategory.OTHER.displayName,
    val availableCategories: List<String> = ExpenseCategory.entries.map { it.displayName },
    val imageBitmap: android.graphics.Bitmap? = null,
    val isSaving: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val isDateInFuture: Boolean = false,
    val validationErrors: ExpenseValidationErrors = ExpenseValidationErrors()
)


@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val locationDataSource: LocationDataSource,
    private val geocodingApiService: GeocodingApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun prefillFromExtracted(details: ReceiptDetails, bitmap: Bitmap?) {

        val suggestedCategory = ExpenseCategory.categorize(details.fullText).displayName
        val defaultNote = details.storeName ?: ""

        // --- MODIFICA 1: Usiamo la nostra funzione di parsing robusta ---
        val parsedDate = parseDateString(details.date) // Usiamo la funzione helper
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = formatter.format(parsedDate)

        // --- VALIDAZIONE ANCHE QUI ---
        val dateIsInFuture = isDateStringInFuture(details.date)

        _uiState.update { it.copy(
            store = details.storeName,
            location = details.storeLocation,
            date = dateString,
            total = details.total,
            notes = defaultNote,
            category = suggestedCategory,
            imageBitmap = bitmap,
            errorMessage = null,
            saved = false,
            isDateInFuture = dateIsInFuture
        ) }
    }

    fun updateCategory(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun updateFields(store: String?,location: String?, date: String?, total: String?, notes: String?) {
        _uiState.value = _uiState.value.copy(
            store = store,
            location = location,
            date = date,
            total = total,
            notes = notes,
            validationErrors = ExpenseValidationErrors()
            )
    }

    // --- NUOVA FUNZIONE PER LA DATA ---
    fun updateDate(newTimestamp: Long) {
        // Formattiamo il timestamp ricevuto dal DatePicker in una stringa "gg/MM/yyyy"
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = formatter.format(Date(newTimestamp))

        // --- VALIDAZIONE QUI ---
        val dateIsInFuture = isDateStringInFuture(dateString)
        _uiState.update { it.copy(date = dateString, isDateInFuture = dateIsInFuture) }
    }

    // --- NUOVA FUNZIONE DI VALIDAZIONE ---
    private fun validateCurrentState(): Boolean {
        val state = _uiState.value
        val errors = ExpenseValidationErrors(
            totalError = if (state.total.isNullOrBlank() || state.total.toBigDecimalOrNull() == null || state.total.toBigDecimal() <= BigDecimal.ZERO) "Amount cannot be empty or zero" else null,
            dateError = if (state.date.isNullOrBlank()) "Date cannot be empty" else null,
            notesError = if (state.notes.isNullOrBlank()) "Description cannot be empty" else null
        )

        _uiState.update { it.copy(validationErrors = errors) }

        // Restituisce true se non ci sono errori
        return errors.totalError == null && errors.dateError == null && errors.notesError == null
    }

    // --- CORREZIONE 1: La validazione avviene QUI, una sola volta. ---
    fun onSaveClicked() {
        // Esegui la validazione come primo passo.
        if (!validateCurrentState() || uiState.value.isDateInFuture) {
            // Se non è valido, la UI mostrerà gli errori grazie all'update
            // dello stato fatto dentro validateCurrentState(). La funzione si interrompe qui.
            Log.d("AddExpenseVM", "Validation failed. Save process stopped.")
            return
        }

        // Se la validazione ha successo, invia un evento alla UI per chiedere i permessi di localizzazione.
        viewModelScope.launch {
            _events.send(AddExpenseEvent.RequestLocationPermission)
        }
    }

    // Chiamato dalla UI dopo che l'utente ha risposto alla richiesta di permesso
    fun onLocationPermissionResult(isGranted: Boolean, isGpsEnabled: Boolean) {
        viewModelScope.launch {
            if (!isGranted) {
                // Se il permesso è negato, salva subito senza arricchire l'indirizzo.
                Log.w("AddExpenseVM", "Location permission denied. Saving without enriching address.")
                performSave(null)
                return@launch
            }

            // Se il permesso è concesso, ma il GPS è spento...
            if (!isGpsEnabled) {
                // ... invia un evento alla UI per chiedere all'utente di attivarlo.
                _events.send(AddExpenseEvent.RequestGpsEnable)
                return@launch
            }

            // Se permesso concesso E GPS attivo, procedi a ottenere la posizione.
            _uiState.update { it.copy(isFetchingLocation = true) } // <-- Attiva lo stato di attesa GPS
            // Usiamo 'withContext' per assicurarci di attendere il risultato
            // e per spostare questa operazione potenzialmente lunga su un thread di background.
            val location = withContext(Dispatchers.IO) {
                locationDataSource.fetchCurrentLocationWithTimeout()
            }


            if (location == null) {
                Log.w("AddExpenseVM", "Could not get location (timeout or other error).")
            }
            // Chiama la funzione di salvataggio finale, passando la posizione (o null se è andata in timeout).
            performSave(location)
        }
    }

    // Funzione chiamata se l'utente rifiuta di attivare il GPS nel dialogo
    fun onGpsEnableDeclined() {
        // L'utente non vuole attivare il GPS, procediamo a salvare senza arricchimento.
        performSave(null)
    }

    // --- CORREZIONE 2: Rinominata e semplificata la funzione di salvataggio ---
    // Questa funzione è ora privata e non esegue più la validazione.
    private fun performSave(currentLocation: LatLng?) {
        // Aggiorna la UI per mostrare che il salvataggio è iniziato
        _uiState.update { it.copy(isFetchingLocation = false, isSaving = true, errorMessage = null) }

        viewModelScope.launch { // Coroutine principale (Dispatcher.Main)
            try {
                val state = _uiState.value
                var enrichedLocation = state.location ?: ""

                // --- LOG 1: VERIFICA I DATI IN INGRESSO ---
                Log.d("AddExpenseVM_DEBUG", "Entering performSave. CurrentLocation: $currentLocation, State.Location: '${state.location}'")


                // --- MODIFICA 1: Esegui la chiamata di rete QUI, nel contesto giusto ---
                // Se abbiamo una posizione e un indirizzo parziale, tentiamo l'arricchimento.
                if (currentLocation != null && !(state.location.isNullOrBlank())) {

                    // --- LOG 2: CONFERMA CHE STIAMO PER FARE LA CHIAMATA DI RETE ---
                    Log.d("AddExpenseVM_DEBUG", "Attempting reverse geocoding for Lat: ${currentLocation.latitude}, Lng: ${currentLocation.longitude}")

                    try {
                        val response = geocodingApiService.reverseGeocode(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        BuildConfig.GEOKEO_API_KEY
                        )

                        // --- LOG 3: ANALIZZA LA RISPOSTA RICEVUTA ---
                        Log.d("AddExpenseVM_DEBUG", "Reverse geocoding response received. Code: ${response.code()}, isSuccessful: ${response.isSuccessful}")



                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody != null) {
                                Log.d("AddExpenseVM_DEBUG", "Response body is not null. Results count: ${responseBody.results.size}")

                                // --- SOLUZIONE CHIAVE QUI: LOGICA DI FALLBACK ---
                                val addressComponents = responseBody.results.firstOrNull()?.addressComponents

                                if (addressComponents != null) {
                                    // Tentiamo di estrarre la città con una logica di fallback,
                                    // controllando più campi in ordine di probabilità.
                                    val cityName = addressComponents.city
                                        ?: addressComponents.subdistrict // <-- Per Roma, GeoKeo usa "subdistrict"
                                        ?: addressComponents.district    // <-- A volte potrebbe essere il "district"
                                        ?: addressComponents.county      // <-- Come ultima spiaggia, la provincia

                                    if (!cityName.isNullOrBlank()) {
                                        Log.d("AddExpenseVM_DEBUG", "City/area found: '$cityName'. Current location: '$enrichedLocation'")
                                        if (!enrichedLocation.contains(cityName, ignoreCase = true)) {
                                            enrichedLocation = "$enrichedLocation, $cityName"
                                            Log.d("AddExpenseVM", "Address enriched successfully: $enrichedLocation")
                                        } else {
                                            Log.d("AddExpenseVM_DEBUG", "City '$cityName' is already in the address. No changes made.")
                                        }
                                    } else {
                                        Log.w("AddExpenseVM_DEBUG", "No usable city, subdistrict, district, or county found in the first result.")
                                    }
                                } else {
                                    Log.w("AddExpenseVM_DEBUG", "Address components are null in the first result.")
                                }
                            } else {
                                Log.w("AddExpenseVM_DEBUG", "Response body is null despite a successful call.")
                            }
                        }
                    } catch (e: Exception) {
                        // Questo log è fondamentale se la chiamata di rete stessa fallisce (es. no internet)
                        Log.e("AddExpenseVM_DEBUG", "A network exception occurred during reverse geocoding.", e)
                    }
                } else {
                    // --- LOG 6: CAPIAMO PERCHÉ L'ARRICCHIMENTO È STATO SALTATO ---
                    Log.w("AddExpenseVM_DEBUG", "Skipping address enrichment. Reason: currentLocation is null or state.location is blank.")
                }

                // --- MODIFICA 2: Usa withContext solo per la creazione dell'oggetto ---
                // Questo è un lavoro leggero, quindi Dispatchers.Default è appropriato.
                val expense = withContext(Dispatchers.Default) {
                    val totalAmount = state.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val receiptDate: Date = parseDateString(state.date)
                    val notes = state.notes ?: (state.store ?: "")
                    val now = Date()

                    Expense(
                        totalAmount = totalAmount,
                        receiptDate = receiptDate,
                        category = state.category,
                        // --- Usa la variabile 'enrichedLocation' ---
                        storeLocation = enrichedLocation,
                        notes = notes,
                        insertionDate = now,
                        lastUpdated = now,
                        isDeleted = false,
                        isSynced = false
                    )
                }

                // Il resto della logica rimane invariato
                expenseRepository.saveExpense(expense)
                _uiState.update { it.copy(isSaving = false, saved = true) }
                _events.send(AddExpenseEvent.Saved)

                launch {
                    try {
                        expenseRepository.syncWithServer()
                    } catch (syncError: Exception) {
                        Log.e("AddExpenseVM", "Background sync failed after save: ${syncError.message}")
                    }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, errorMessage = t.message) }
                _events.send(AddExpenseEvent.Error(t.message ?: "Error"))
            }
        }
    }

    // --- NUOVA FUNZIONE DI VALIDAZIONE, più semplice ---
    private fun isDateStringInFuture(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val parsedDate = LocalDate.parse(dateStr, formatter)
            parsedDate.isAfter(LocalDate.now())
        } catch (e: DateTimeParseException) {
            false
        }
    }

    private fun parseDateString(dateStr: String?): Date {
        if (dateStr.isNullOrBlank()) return Date()
        val fourDigitYearFormats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyyMMdd" // A volte si trovano anche senza separatori
        )

// 2. Formati con anno a 2 cifre (priorità più bassa, gestiti con attenzione)
        val twoDigitYearFormats = listOf(
            "dd/MM/yy",
            "dd-MM-yy",
            "MM/dd/yy"
        )

        var parsedLocalDate: LocalDate? = null

        for (fmt in fourDigitYearFormats) {
            try {
                // Usiamo java.time.LocalDate che è più moderno e robusto di SimpleDateFormat
                val formatter = DateTimeFormatter.ofPattern(fmt, Locale.getDefault())
                parsedLocalDate = LocalDate.parse(dateStr, formatter)
                // Convertiamo LocalDate in java.util.Date
                break
            } catch (e: DateTimeParseException) {
                // Continua con il prossimo formato
            }
        }

// 4. Se falliscono i 4 cifre, prova i formati a 2 cifre con gestione del secolo
        for (fmt in twoDigitYearFormats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(fmt, Locale.getDefault())
                // Il parsing dell'anno a due cifre da parte di DateTimeFormatter è più intelligente di SimpleDateFormat.
                // Per esempio, se è il 2024, "26" verrà interpretato come 2026.
                parsedLocalDate = LocalDate.parse(dateStr, formatter)
                break
            } catch (e: DateTimeParseException) {
                // Continua con il prossimo formato
            }
        }

        parsedLocalDate?.let { date ->
            val currentYear = LocalDate.now().year
            if (date.year > currentYear + 1) { // Tolleranza di 1 anno (per sicurezza, non si sa mai)
                Log.w("AddExpenseViewModel", "Data con anno futuro ('${date.year}') rilevata. Imposto l'anno corrente.")
                // Ritorna una nuova LocalDate con l'anno corrente, mantenendo mese e giorno
                return Date.from(date.withYear(currentYear).atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        }

// 3. Fallback: Se il parsing non ha avuto successo o l'anno era ok, converte la data trovata o usa la corrente
        return parsedLocalDate?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: run{
            Date()
        }
    }

    sealed class AddExpenseEvent {
        object Saved : AddExpenseEvent()
        object RequestLocationPermission : AddExpenseEvent()
        object RequestGpsEnable : AddExpenseEvent()
        data class Error(val message: String) : AddExpenseEvent()
    }
}
