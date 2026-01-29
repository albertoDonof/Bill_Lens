package com.example.billlens.domain.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.model.Expense
import com.example.billlens.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Stato della UI per questa schermata
data class AllExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AllExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // Stato per la query di ricerca
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Stato per la categoria selezionata come filtro
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    // Combiniamo tutti i dati e filtri in un unico StateFlow per la UI
    val uiState: StateFlow<AllExpensesUiState> = combine(
        expenseRepository.allExpenses, // Il Flow di tutte le spese da Room
        _searchQuery,                  // Il Flow della ricerca
        _selectedCategory              // Il Flow del filtro categoria
    ) { allExpenses, query, category ->

        // Applichiamo i filtri qui, dentro una coroutine!
        val filteredExpenses = allExpenses.filter { expense ->
            val matchesCategory = category == null || expense.category == category
            val matchesSearch = query.isBlank() ||
                    expense.notes?.contains(query, ignoreCase = true) == true ||
                    expense.storeLocation?.contains(query, ignoreCase = true) == true
            matchesCategory && matchesSearch
        }

        AllExpensesUiState(expenses = filteredExpenses, isLoading = false)

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllExpensesUiState(isLoading = true)
    )

    // Funzioni per aggiornare i filtri dalla UI
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterChanged(category: String?) {
        // Se si clicca di nuovo sulla stessa categoria, si deseleziona
        if (_selectedCategory.value == category) {
            _selectedCategory.value = null
        } else {
            _selectedCategory.value = category
        }
    }
}