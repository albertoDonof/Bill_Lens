package com.example.billlens.domain.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.model.Expense
import com.example.billlens.data.repository.ExpenseRepository
import com.example.billlens.utils.Async
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rappresenta lo stato completo della UI per la schermata della dashboard.
 * Essendo definita nello stesso file, è strettamente accoppiata al suo ViewModel.
 */
data class ExpensesListUiState(
    // La lista di spese da mostrare, proveniente da Async.Success.
    val expenses: List<Expense> = emptyList(),
    // Flag per indicare un'operazione in corso (es. sincronizzazione manuale).
    val isSyncing: Boolean = false,
    // Messaggio da mostrare all'utente (es. in una Snackbar).
    val userMessage: String? = null,
    // Flag che indica se il caricamento iniziale dei dati è ancora in corso.
    val isInitialLoading: Boolean = false,
    // Messaggio di errore se il caricamento iniziale fallisce.
    val initialLoadError: String? = null,
)

/**
 * ViewModel per la schermata principale che mostra la lista delle spese.
 * Segue il pattern di combinare diversi stream di dati in un unico UiState.
 */
@HiltViewModel
class ExpensesListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // Flussi privati per gestire stati specifici e interni.
    private val _userMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _isSyncing = MutableStateFlow(false)

    // Flusso che riceve le spese dal repository e le wrappa in una classe Async.
    private val _expensesAsync: Flow<Async<List<Expense>>> =
        expenseRepository.allExpenses
            .map<List<Expense>, Async<List<Expense>>> { expenses -> Async.Success(expenses) }
            .catch { emit(Async.Error("Errore nel caricamento delle spese.")) }

    /**
     * Lo StateFlow pubblico che la UI osserverà.
     * È il risultato della combinazione di tutti gli altri flussi.
     */
    val uiState: StateFlow<ExpensesListUiState> = combine(
        _isSyncing, _userMessage, _expensesAsync
    ) { isSyncing, userMessage, expensesAsync ->
        when (expensesAsync) {
            Async.Loading -> {
                ExpensesListUiState(isInitialLoading = true)
            }

            is Async.Error -> {
                ExpensesListUiState(initialLoadError = expensesAsync.errorMessage)
            }

            is Async.Success -> {
                ExpensesListUiState(
                    expenses = expensesAsync.data,
                    isSyncing = isSyncing,
                    userMessage = userMessage
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ExpensesListUiState(isInitialLoading = true)
        )

    init {
        // Avvia la prima sincronizzazione all'avvio del ViewModel.
        syncData(isManualRefresh = false)
    }

    /**
     * Avvia la sincronizzazione con il server.
     * @param isManualRefresh Indica se la sync è stata avviata dall'utente (es. pull-to-refresh).
     */
    fun syncData(isManualRefresh: Boolean = true) {
        // Mostra il loader solo se l'utente ha avviato l'azione manualmente.
        if (isManualRefresh) {
            _isSyncing.value = true
        }

        viewModelScope.launch {
            try {
                expenseRepository.syncWithServer()
                if (isManualRefresh) {
                    showSnackbarMessage("Dati sincronizzati con successo")
                }
            } catch (e: Exception) {
                showSnackbarMessage("Sincronizzazione fallita")
            } finally {
                // Nascondi sempre il loader al termine.
                if (isManualRefresh) {
                    _isSyncing.value = false
                }
            }
        }
    }

    /**
     * Gestisce la richiesta di eliminazione di una spesa.
     */
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            showSnackbarMessage("Spesa eliminata")
            // Optional: avviare una sync dopo la cancellazione
            syncData(isManualRefresh = false)
        }
    }

    /**
     * Metodo privato per aggiornare lo stream del messaggio utente.
     */
    private fun showSnackbarMessage(message: String) {
        _userMessage.value = message
    }

    /**
     * Chiamato dalla UI dopo che il messaggio è stato mostrato, per resettare lo stato.
     */
    fun snackbarMessageShown() {
        _userMessage.value = null
    }
}