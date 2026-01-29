package com.example.billlens.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.billlens.domain.expenses.ExpenseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    navController: NavController,
    viewModel: ExpenseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Gestisce la navigazione indietro dopo un salvataggio o cancellazione
    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            navController.popBackStack()
        }
    }

    // Dialogo di conferma per la cancellazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete this expense?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete Expense")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::updateExpense,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Expense")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.expense != null) {
            val expense = uiState.expense!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Campo Descrizione/Note
                OutlinedTextField(
                    value = expense.notes ?: "",
                    onValueChange = { newValue ->
                        viewModel.onFieldChange(
                            notes = newValue,
                            total = expense.totalAmount.toPlainString(),
                            category = expense.category,
                            location = expense.storeLocation ?: ""
                        )
                    },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.validationErrors.notesError != null,
                    supportingText = {
                        if (uiState.validationErrors.notesError != null) {
                            Text(text = uiState.validationErrors.notesError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // --- MODIFICA 2: Aggiungi il nuovo campo per l'indirizzo ---
                OutlinedTextField(
                    value = expense.storeLocation ?: "",
                    onValueChange = { newValue ->
                        viewModel.onFieldChange(
                            notes = expense.notes ?: "",
                            total = expense.totalAmount.toPlainString(),
                            category = expense.category,
                            location = newValue // Questo è il valore che sta cambiando
                        )
                    },
                    label = { Text("Address / Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoria (Menu a tendina)
                CategoryDropdown(
                    selectedCategory = expense.category,
                    categories = uiState.availableCategories,
                    onCategorySelected = { newCategory ->
                        viewModel.onFieldChange(
                            notes = expense.notes ?: "",
                            total = expense.totalAmount.toPlainString(),
                            category = newCategory,
                            location = expense.storeLocation ?: ""
                        )
                    }
                )

                // Campo Importo
                OutlinedTextField(
                    value = uiState.totalInput,
                    onValueChange = { newValue ->
                        viewModel.onFieldChange(
                            notes = expense.notes ?: "",
                            total = newValue,
                            category = expense.category,
                            location = expense.storeLocation ?: ""
                        )
                    },
                    label = { Text("Total Amount") },
                    prefix = { Text("€ ") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.validationErrors.totalError != null,
                    supportingText = {
                        if (uiState.validationErrors.totalError != null) {
                            Text(text = uiState.validationErrors.totalError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Data (non modificabile per semplicità, ma si potrebbe aggiungere un DatePickerDialog)
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(expense.receiptDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth()
                )


            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "An unknown error occurred.")
            }
        }
    }
}


// Estratto in un componente riutilizzabile per pulizia
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}