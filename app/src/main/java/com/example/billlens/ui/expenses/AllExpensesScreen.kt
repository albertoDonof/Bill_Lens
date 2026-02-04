package com.example.billlens.ui.expenses


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.billlens.domain.expenses.AllExpensesViewModel
import com.example.billlens.domain.expenses.ExpenseCategory
import com.example.billlens.ui.components.ExpenseItem
import com.example.billlens.ui.navigation.AppBottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(
    navController: NavController,
    viewModel: AllExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("All Expenses") })
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 1. Barra di Ricerca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text("Search by description...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // 2. Filtri per Categoria (con LazyRow per scorrerli)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExpenseCategory.entries) { category ->
                    FilterChip(
                        selected = category.displayName == selectedCategory,
                        onClick = { viewModel.onCategoryFilterChanged(category.displayName) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Lista delle Spese
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses found.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onItemClick = { expenseId ->
                                navController.navigate("expense_detail/$expenseId")
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}