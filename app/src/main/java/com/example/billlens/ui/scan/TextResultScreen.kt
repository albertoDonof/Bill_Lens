package com.example.billlens.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                is AddExpenseViewModel.AddExpenseEvent.Error -> {
                    // Opzionale: gestire l'errore qui (es. mostrare Snackbar)
                }
            }
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
                Button(
                    onClick = { addExpenseViewModel.saveExpense() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = !addUiState.isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (addUiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Expense", fontSize = 18.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Weekly Groceries") }
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

            OutlinedTextField(
                value = addUiState.date ?: "",
                onValueChange = { addExpenseViewModel.updateFields(addUiState.store, addUiState.location ,it, addUiState.total,addUiState.notes) },
                label = { Text("Date (DD/MM/YYYY)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = addUiState.total ?: "",
                onValueChange = { addExpenseViewModel.updateFields(addUiState.store,addUiState.location, addUiState.date, it, addUiState.notes) },
                label = { Text("Total Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("€ ") }
            )

            if (addUiState.errorMessage != null) {
                Text(addUiState.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Sezione 2: Testo Completo (Riferimento)
            Text("Complete Text", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

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