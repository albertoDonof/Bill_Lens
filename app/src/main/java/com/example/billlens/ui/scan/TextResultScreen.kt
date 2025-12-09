package com.example.billlens.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billlens.domain.scan.ScanReceiptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextResultScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanReceiptViewModel // Usa il suo ViewModel dedicato
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Questo è importante: quando l'utente esce da questa schermata,
    // puliamo il testo riconosciuto per preparare la prossima scansione.
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearRecognizedText()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Risultato Scansione") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.recognizedText != null) {
            Text(
                text = uiState.recognizedText!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        } else {
            // Stato di fallback nel caso in cui l'utente arrivi qui per errore
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun testo da mostrare.")
            }
        }
    }
}