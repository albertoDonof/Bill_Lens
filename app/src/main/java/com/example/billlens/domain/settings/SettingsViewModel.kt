package com.example.billlens.domain.settings


import java.util.concurrent.TimeUnit
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.local.PreferenceDataSource
import com.example.billlens.data.network.NetworkDataSource
import com.example.billlens.data.repository.ExpenseRepository
import com.example.billlens.data.repository.UserRepository
import com.google.android.gms.auth.UserRecoverableAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

data class SettingsUiState(
    val userName: String? = null,
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val isExporting: Boolean = false, // Stato per il caricamento Google Sheets
    val isSyncing: Boolean = false,    // Stato per la sincronizzazione server
    val lastExportUrl: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val authorizationIntent: android.content.Intent? = null,
    val authorizationRequest: AuthorizationRequest? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val networkDataSource: NetworkDataSource,
    private val preferenceDataSource: PreferenceDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // Creiamo un StateFlow separato SOLO per il tempo relativo.
    // Questo si attiverà solo quando la UI lo osserva.
    val lastSyncRelativeTime: StateFlow<String> = flow {
        // Combiniamo il timestamp e un ticker
        combine(
            preferenceDataSource.lastSyncTimestamp,
            tickerFlow() // Usiamo una funzione helper per il ticker
        ) { timestamp, _ ->
            formatRelativeTime(timestamp)
        }.collect { relativeTime ->
            emit(relativeTime) // Emettiamo la stringa formattata
        }
    }.stateIn(
        scope = viewModelScope,
        // CRUCIALE: si avvia quando la UI osserva e si ferma dopo 5 secondi di inattività.
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Loading..."
    )

    init {
        // Avviamo l'osservazione dei dati utente al momento della creazione
        observeUserData()
    }

    // Funzione helper per creare il ticker in modo pulito
    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(60_000) // Emette ogni 60 secondi
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "Never"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Just now"
            minutes < 2 -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 2 -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    private fun observeUserData() {
        userRepository.userData
            .onEach { user ->
                _uiState.update { it.copy(
                    userName = user?.displayName,
                    userEmail = user?.email,
                    profilePictureUrl = user?.profilePictureUrl
                ) }
            }
            .launchIn(viewModelScope)
    }


    /**
     * Chiamato dalla UI con il risultato del launcher di autorizzazione.
     */
    fun onAuthorizationResult(result: AuthorizationResult?) {
        if (result?.accessToken != null) {
            // SUCCESSO! Abbiamo un Access Token.
            // Questo copre sia il caso in cui l'utente ha appena dato il consenso,
            // sia il caso in cui lo aveva già dato in passato.
            exportWithAccessToken(result.accessToken!!)
        } else {
            // FALLIMENTO: non abbiamo ottenuto un token.
            // Potrebbe essere perché l'utente ha annullato la schermata di consenso
            // o si è verificato un altro errore.
            _uiState.update { it.copy(
                isExporting = false, // Assicurati che il loader si fermi
                errorMessage = "Authorization was denied or failed. Please try again."
            ) }
        }
    }

    private fun exportWithAccessToken(accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, successMessage = null) }
            try {
                val idToken = userRepository.getIdToken() ?: throw Exception("Not authenticated")
                val response = networkDataSource.exportToSheets(idToken, accessToken)

                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.update { it.copy(
                        isExporting = false,
                        successMessage = body?.message ?: "Export completed!",
                        lastExportUrl = body?.sheetUrl
                    )}
                } else {
                    throw Exception("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, errorMessage = e.message) }
            }
        }
    }

    // --- NEW FUNCTION ---
    // This function will be called from the UI's onClick
    fun startAuthorizationFlow() {
        // The ViewModel can access the repository
        val request = userRepository.getAuthorizationRequest()
        _uiState.update { it.copy(authorizationRequest = request) }
    }

    // --- NEW FUNCTION ---
    // To reset the request after it has been launched
    fun authorizationRequestConsumed() {
        _uiState.update { it.copy(authorizationRequest = null) }
    }

    // Funzione per resettare lo stato dell'intent dopo che è stato usato
    fun authorizationIntentConsumed() {
        _uiState.update { it.copy(authorizationIntent = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            // 1. Cancella le spese di Room
            expenseRepository.clearLocalData()

            userRepository.clearLocalUser()

            // 3. Esegui il logout da Firebase
            userRepository.signOut()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun triggerSync() {
        // Prevent multiple syncs
        if (preferenceDataSource.isSyncing.value) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                expenseRepository.syncWithServer()
                _uiState.update { it.copy(isSyncing = false, successMessage = "Data synced correctly") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, errorMessage = "Sync failed! Retry Later.") }
            }
        }
    }
}