package com.example.billlens.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.billlens.data.model.Expense
import com.example.billlens.domain.home.HomeViewModel
import com.example.billlens.ui.components.ExpensesFeed
import com.example.billlens.ui.components.MonthlySummary
import com.example.billlens.ui.components.UserHeader
import com.example.billlens.ui.navigation.AppBottomNavigationBar
import com.example.billlens.ui.navigation.NavigationScreens
import com.example.billlens.ui.navigation.SCAN_GRAPH_ROUTE
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // L'UserHeader ora può far parte della TopAppBar
                    UserHeader(
                        userName = uiState.userName ?: "User",
                        profilePictureUrl = uiState.userProfilePictureUrl

                    )
                }
            )

        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(SCAN_GRAPH_ROUTE) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Scan receipt")
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        // Gestione degli stati di caricamento ed errore
        when {
            uiState.isInitialLoading -> {
                Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.initialLoadError != null -> {
                Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${uiState.initialLoadError}")
                }
            }
            else -> {
                // Passiamo il modifier con il padding al contenuto
                HomeScreenContent(
                    navController = navController,
                    monthlyTotal = uiState.monthlyTotal,
                    recentExpenses = uiState.recentExpenses,
                    modifier = contentModifier
                )
            }
        }
    }

}


@Composable
fun HomeScreenContent(
    navController: NavController,
    monthlyTotal: BigDecimal,
    recentExpenses: List<Expense>,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // 1. Riepilogo spese del mese
        MonthlySummary(
            total = monthlyTotal,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Titolo della lista
        Text(
            text = "Recent Expenses",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Feed delle spese
        if (recentExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent expenses. Add one!")
            }
        } else {
            ExpensesFeed(
                expenses = recentExpenses,
                onExpenseClick = { expenseId ->// Naviga al dettaglio passando l'ID
                    navController.navigate("expense_detail/$expenseId")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun HomeScreenContentPreview() {
    // Dati di esempio per la preview
    val previewExpenses = listOf(
        Expense(
            id = "1",
            totalAmount = BigDecimal("12.50"),
            receiptDate = Date(),
            category = "Cibo",
            notes = "Pranzo veloce",
            storeLocation = "Ristorante Da Mario",
            insertionDate = Date(),
            lastUpdated = Date()
        ),
        Expense(
            id = "2",
            totalAmount = BigDecimal("85.00"),
            receiptDate = Date(System.currentTimeMillis() - 86400000 * 2), // Due giorni fa
            category = "Shopping",
            notes = "Maglietta nuova",
            storeLocation = "Negozio Centro",
            insertionDate = Date(),
            lastUpdated = Date()
        ),
        Expense(
            id = "3",
            totalAmount = BigDecimal("5.20"),
            receiptDate = Date(System.currentTimeMillis() - 86400000 * 3), // Tre giorni fa
            category = "Trasporti",
            notes = "Biglietto autobus",
            storeLocation = "Edicola Stazione",
            insertionDate = Date(),
            lastUpdated = Date()
        )
    )

    // È buona norma wrappare la preview nel tema dell'app
    // per vedere colori e font corretti.
    // Sostituisci con il nome del tuo tema se è diverso.
    MaterialTheme {
        HomeScreenContent(
            navController = rememberNavController(),
            monthlyTotal = BigDecimal("255.75"),
            recentExpenses = previewExpenses
        )
    }
}
