package com.example.billlens.domain.home

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.model.Expense
import com.example.billlens.data.repository.ExpenseRepository
import com.example.billlens.data.repository.UserRepository
import com.example.billlens.utils.Async
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.math.exp

data class HomeUiState(
    // Dati dell'utente
    val userName: String? = null,
    val userProfilePictureUrl: String? = null,
    val monthlyTotal: BigDecimal = BigDecimal.ZERO,
    val recentExpenses: List<Expense> = emptyList(),
    // campi gestione UI
    // Flag per indicare un'operazione in corso (es. sincronizzazione manuale).
    val isSyncing: Boolean = false,
    // Messaggio da mostrare all'utente (es. in una Snackbar).
    val userMessage: String? = null,
    // Flag che indica se il caricamento iniziale dei dati è ancora in corso.
    val isInitialLoading: Boolean = false,
    // Messaggio di errore se il caricamento iniziale fallisce.
    val initialLoadError: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    // Flussi privati per gestire stati specifici e interni.
    private val _userMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _isSyncing = MutableStateFlow(false)

    // Flusso che riceve le spese dal repository e le wrappa in una classe Async.
    private val _expensesAsync: Flow<Async<List<Expense>>> =
        expenseRepository.allExpenses
            .map<List<Expense>, Async<List<Expense>>> { expenses -> Async.Success(expenses) }
            .catch { emit(Async.Error("Errore nel caricamento delle spese.")) }


    private val _userDataStream = userRepository.userData
    /**
     * Lo StateFlow pubblico che la UI osserverà.
     * È il risultato della combinazione di tutti gli altri flussi.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        _isSyncing, _userMessage, _expensesAsync, _userDataStream
    ) { isSyncing, userMessage, expensesAsync , userData->
        when (expensesAsync) {
            Async.Loading -> {
                HomeUiState(isInitialLoading = true)
            }

            is Async.Error -> {
                HomeUiState(initialLoadError = expensesAsync.errorMessage)
            }

            is Async.Success -> {
                val expensesData = expensesAsync.data
                val monthlyTotal = calculateMonthlyTotal(expensesData)
                HomeUiState(
                    userName = userData?.displayName,
                    userProfilePictureUrl = userData?.profilePictureUrl,
                    recentExpenses = expensesData,
                    monthlyTotal = monthlyTotal,
                    isSyncing = isSyncing,
                    userMessage = userMessage
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeUiState(isInitialLoading = true)
        )

    init {
        // Avvia la prima sincronizzazione all'avvio del ViewModel.
        syncData(isManualRefresh = false)
    }


    private fun calculateMonthlyTotal(expenses: List<Expense>): BigDecimal {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return expenses.filter { expense ->
            calendar.time = expense.receiptDate
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }.fold(BigDecimal.ZERO){acc, expense -> acc.add(expense.totalAmount)}
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