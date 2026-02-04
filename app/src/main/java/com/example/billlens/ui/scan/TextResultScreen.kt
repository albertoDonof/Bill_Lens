package com.example.billlens.ui.scan

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import android.content.pm.PackageManager
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billlens.domain.expenses.AddExpenseViewModel
import com.example.billlens.domain.scan.ScanReceiptViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.billlens.data.location.LocationDataSource
import kotlinx.coroutines.flow.first
import android.location.LocationManager
import android.provider.Settings // <-- SOLUZIONE: Aggiungi questo import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextResultScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    scanViewModel: ScanReceiptViewModel, // ViewModel del grafo di scansione
    addExpenseViewModel: AddExpenseViewModel = hiltViewModel()
) {
    val scanUiState by scanViewModel.uiState.collectAsStateWithLifecycle()
    val addUiState by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showGpsEnableDialog by remember { mutableStateOf(false) }

    // --- MODIFICA 1: CREA I FOCUS REQUESTER E LO SCROLL STATE ---
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState() // Stato per controllare lo scroll
    val notesFocusRequester = remember { FocusRequester() } // Requester per il campo 'notes'
    val totalFocusRequester = remember { FocusRequester() } // Requester per il campo 'total'


    // Funzione helper per controllare se il GPS è attivo
    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            addExpenseViewModel.onLocationPermissionResult(isGranted, isGpsEnabled())
        }
    )

    // --- MODIFICA 2: EFFETTO PER GESTIRE IL FOCUS E LO SCROLL ---
    // Questo LaunchedEffect si attiva ogni volta che validationErrors cambia.
    LaunchedEffect(addUiState.validationErrors) {
        val errors = addUiState.validationErrors
        // Se ci sono errori, trova il primo e sposta il focus e la vista.
        if (errors.notesError != null) {
            // Lancia una coroutine per eseguire lo scroll e richiedere il focus.
            coroutineScope.launch {
                notesFocusRequester.requestFocus()
                // Scorre fino alla cima della pagina per assicurare che il campo sia visibile.
                scrollState.animateScrollTo(0)
            }
        } else if (errors.dateError != null) {
            // Per la data, possiamo solo scrollare perché il campo è disabilitato.
            coroutineScope.launch {
                scrollState.animateScrollTo(200) // Un valore approssimativo per scorrere verso il basso
            }
        } else if (errors.totalError != null) {
            coroutineScope.launch {
                totalFocusRequester.requestFocus()
                // Scorre fino alla fine della pagina per mostrare il campo 'total'.
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    // Sincronizza i dati tra i due ViewModel al caricamento
    LaunchedEffect(scanUiState.extractedDetails) {
        scanUiState.extractedDetails?.let { details ->
            addExpenseViewModel.prefillFromExtracted(details, scanUiState.frozenBitmap)
        }
    }

    // 2. Ascoltiamo l'evento di salvataggio avvenuto con successo
    LaunchedEffect(Unit) {
        addExpenseViewModel.events.collectLatest { event ->
            when (event) {
                is AddExpenseViewModel.AddExpenseEvent.Saved -> {
                    onSaveSuccess()
                }
                is AddExpenseViewModel.AddExpenseEvent.RequestLocationPermission -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Se il permesso c'è già, controlla subito il GPS
                        addExpenseViewModel.onLocationPermissionResult(true, isGpsEnabled())
                    } else {
                        // Altrimenti, lancia la richiesta di permesso
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
                is AddExpenseViewModel.AddExpenseEvent.RequestGpsEnable -> {
                    showGpsEnableDialog = true // Il ViewModel chiede di mostrare il dialogo
                }
                is AddExpenseViewModel.AddExpenseEvent.Error -> {
                    // Opzionale: gestire l'errore qui (es. mostrare Snackbar)
                }
            }
        }
    }

    // Dialogo per chiedere di attivare il GPS
    if (showGpsEnableDialog) {
        AlertDialog(
            onDismissRequest = {
                showGpsEnableDialog = false
                addExpenseViewModel.onGpsEnableDeclined() // L'utente chiude il dialogo, salva senza GPS
            },
            title = { Text("Enable GPS") },
            text = { Text("To improve address accuracy, please enable your device's GPS. Would you like to save without it?") },
            confirmButton = {
                TextButton(onClick = {
                    showGpsEnableDialog = false
                    // Apri le impostazioni di localizzazione del telefono
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Enable GPS") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGpsEnableDialog = false
                    addExpenseViewModel.onGpsEnableDeclined() // Salva senza GPS
                }) { Text("Save Without") }
            }
        )
    }

    // --- 2. IMPLEMENTAZIONE DEL DATE PICKER DIALOG ---
    if (showDatePicker) {
        Log.d("TextResultScreen", "DatePickerDialog should be showing now.")
        // Tentiamo di pre-selezionare la data attuale nel picker
        val initialDateMillis = remember(addUiState.date) {
            try {
                // Tenta di fare il parsing della data attuale per inizializzare il calendario
                addUiState.date?.let { SimpleDateFormat("dd/MM/yyyy",
                    Locale.getDefault()).parse(it)?.time }
            } catch (e: Exception) {
                null // Se fallisce, il picker si aprirà sulla data odierna
            } ?: System.currentTimeMillis() // Fallback alla data/ora corrente
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        // Passa il timestamp (in millisecondi UTC) selezionato al ViewModel
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            Log.d("TextResultScreen", "DatePickerDialog: New date selected with timestamp: $timestamp")
                            addExpenseViewModel.updateDate(timestamp)
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Tasto Salva fisso in basso per comodità
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // --- MOSTRA L'ERRORE DELLA DATA QUI ---
                    if (addUiState.isDateInFuture) {
                        Text(
                            text = "Cannot save an expense with a future date.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = { addExpenseViewModel.onSaveClicked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = !addUiState.isSaving && !addUiState.isDateInFuture && !addUiState.isFetchingLocation,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // Usiamo un 'when' per cambiare il contenuto del bottone in base allo stato
                        when {
                            // Stato 1: Sta ottenendo la posizione GPS
                            addUiState.isFetchingLocation -> {
                                Text("Fetching location...")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary, // Colore che contrasta con lo sfondo del bottone
                                    strokeWidth = 2.dp
                                )
                            }
                            // Stato 2: Sta salvando (dopo aver ottenuto la posizione)
                            addUiState.isSaving -> {
                                Text("Saving...")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                            // Stato 3: Stato normale, pronto per il click
                            else -> {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save Expense", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sezione 1: Campi Modificabili
            Text("Expense Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = addUiState.notes ?: "",
                onValueChange = {
                    addExpenseViewModel.updateFields(
                        addUiState.store, addUiState.location, addUiState.date, addUiState.total, it
                    )
                },
                label = { Text("Description / Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(notesFocusRequester),
                singleLine = true,
                placeholder = { Text("e.g. Weekly Groceries") },
                isError = addUiState.validationErrors.notesError != null,
                supportingText = {
                    if (addUiState.validationErrors.notesError != null) {
                        Text(text = addUiState.validationErrors.notesError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            OutlinedTextField(
                value = addUiState.location ?: "",
                onValueChange = { addExpenseViewModel.updateFields(addUiState.store,it, addUiState.date, addUiState.total, addUiState.notes) },
                label = { Text("Location / Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // --- NEW: Category Dropdown ---
            ExposedDropdownMenuBox(
                expanded = expandedCategoryDropdown,
                onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = addUiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedCategoryDropdown,
                    onDismissRequest = { expandedCategoryDropdown = false }
                ) {
                    addUiState.availableCategories.forEach { categoryName ->
                        DropdownMenuItem(
                            text = { Text(categoryName) },
                            onClick = {
                                addExpenseViewModel.updateCategory(categoryName)
                                expandedCategoryDropdown = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // --- 3. RENDI IL CAMPO DATA CLICCABILE E NON SCRIVIBILE ---
            // Incapsuliamo il campo della data in un Box per isolare il click.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = {
                            Log.d(
                                "TextResultScreen",
                                "Date field (Box) clicked! Setting showDatePicker to true."
                            )
                            showDatePicker = true
                        },
                        // Aggiungiamo enabled = true per chiarezza, anche se è il default
                        enabled = true
                    )
            ) {
                OutlinedTextField(
                    value = addUiState.date ?: "",
                    onValueChange = { /* Non facciamo nulla, è read-only */ },
                    label = { Text("Date (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    // IMPORTANTE: Disabilitiamo gli eventi di input sul campo di testo stesso
                    // per evitare conflitti con il Box.
                    enabled = false,
                    isError = addUiState.validationErrors.dateError != null,
                    // Cambiamo i colori per farlo apparire "normale" e non disabilitato
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
            // Mostra l'errore della data sotto il Box per visibilità
            if (addUiState.validationErrors.dateError != null) {
                Text(
                    text = addUiState.validationErrors.dateError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            OutlinedTextField(
                value = addUiState.total ?: "",
                onValueChange = { addExpenseViewModel.updateFields(addUiState.store,addUiState.location, addUiState.date, it, addUiState.notes) },
                label = { Text("Total Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(totalFocusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("€ ") },
                isError = addUiState.validationErrors.totalError != null,
                supportingText = {
                    if (addUiState.validationErrors.totalError != null) {
                        Text(text = addUiState.validationErrors.totalError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            if (addUiState.errorMessage != null) {
                Text(addUiState.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Sezione 2: Testo Completo (Riferimento)
            Text("Complete Text Scanned", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = scanUiState.extractedDetails?.fullText ?: "No text found.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }

            // Spazio extra in fondo per non coprire il testo con il tasto fisso
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}