package com.example.billlens.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billlens.data.model.Expense
import com.example.billlens.domain.expenses.ExpensesListViewModel
import com.example.billlens.ui.components.ExpensesFeed
import java.math.BigDecimal
import java.util.Date

/**
 * Schermata che mostra unicamente la lista completa di tutte le spese.
 *
 * Utilizza ExpensesListViewModel per ottenere e gestire lo stato delle spese.
 */
@Composable
fun ExpensesListScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesListViewModel = hiltViewModel()
) {
    // Osserva lo stato dal ViewModel in modo sicuro rispetto al ciclo di vita
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Gestione degli stati di caricamento ed errore
    when {
        uiState.isInitialLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.initialLoadError != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${uiState.initialLoadError}")
            }
        }

        else -> {
            // Passa i dati reali dal uiState al contenuto della schermata
            ExpensesListScreenContent(
                modifier = modifier,
                expenses = uiState.expenses
            )
        }
    }
}

/**
 * Il contenuto effettivo della schermata, che mostra la lista di spese.
 */
@Composable
fun ExpensesListScreenContent(
    modifier: Modifier = Modifier,
    expenses: List<Expense>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // La lista ora occupa tutto lo spazio disponibile
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                contentAlignment = Alignment.Center
            ) {
                Text("No expense found.")
            }
        } else {
            ExpensesFeed(
                expenses = expenses,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpensesListScreenContentPreview() {
    val previewExpenses = listOf(
        Expense(
            id = "1",
            totalAmount = BigDecimal("12.50"),
            receiptDate = Date(),
            category = "Cibo",
            storeLocation = "Ristorante Da Mario",
            notes = "Pranzo",
            insertionDate = Date(),
            lastUpdated = Date()
        ),
        Expense(
            id = "2",
            totalAmount = BigDecimal("85.00"),
            receiptDate = Date(),
            category = "Shopping",
            storeLocation = "Centro Commerciale",
            notes = "Maglietta nuova",
            insertionDate = Date(),
            lastUpdated = Date()
        )
    )
    MaterialTheme {
        ExpensesListScreenContent(expenses = previewExpenses)
    }
}