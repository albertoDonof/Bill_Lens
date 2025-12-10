package com.example.billlens.domain.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.result.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Stato della UI per la schermata di scansione.
 */
data class ScanUiState(
    val hasCameraPermission: Boolean = false,
    val isAnalyzing: Boolean = false,
    val recognizedText: String? = null,
    val frozenBitmap: Bitmap? = null
)

// Evento di navigazione con il testo come payload
sealed class ScanNavigationEvent {
    data object NavigateToTextResult : ScanNavigationEvent()
}

/**
 * ViewModel per la schermata di scansione.
 * Gestisce lo stato dei permessi e il testo riconosciuto da ML Kit.
 */
@HiltViewModel
class ScanReceiptViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<ScanNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var captureFunc: (() -> Unit)? = null

    /**
     * Chiamato quando il risultato della richiesta di permesso è disponibile.
     */
    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
        if (!isGranted) {
            Log.w("ScanViewModel", "Permesso fotocamera non concesso dall'utente.")
        }
    }

    /**
     * Chiamato dall'analizzatore di testo ogni volta che viene trovato del testo.
     */
    fun onTextRecognized(text: String) {
        // Ferma il loader
        _uiState.update { it.copy(isAnalyzing = false, recognizedText = text) }

        // Invia l'evento di navigazione con il testo riconosciuto
        viewModelScope.launch {
            _navigationEvent.send(ScanNavigationEvent.NavigateToTextResult)
        }
    }

    /**
     * Chiamato dalla UI quando l'utente preme il tasto di scatto.
     */
    fun onTakePhotoClick() {
        //_uiState.update { it.copy(isAnalyzing = true) }
        captureFunc?.invoke()
    }

    fun onAnalysisStarted(bitmap: Bitmap?) {
        _uiState.update { it.copy(isAnalyzing = true, frozenBitmap = bitmap) }
    }

    fun onNewScanRequested() {
        _uiState.update { it.copy(isAnalyzing = false, frozenBitmap = null) }
    }

    /**
     * Chiamato dalla UI per fornire la funzione di scatto effettiva.
     */
    fun setCaptureFunction(captureFunc: () -> Unit) {
        this.captureFunc = captureFunc
    }

    fun clearRecognizedText() {
        _uiState.update { it.copy(recognizedText = null) }
    }
}