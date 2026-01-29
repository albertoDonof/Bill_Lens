package com.example.billlens.domain.expenses


import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.model.Expense
import com.example.billlens.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject


// Stato per la UI della schermata di dettaglio
data class ExpenseDetailUiState(
    val expense: Expense? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val navigateBack: Boolean = false, // Segnale per la navigazione
    val errorMessage: String? = null,
    val availableCategories: List<String> = ExpenseCategory.entries.map { it.displayName },
    val totalInput: String = "",
    val validationErrors: ExpenseValidationErrors = ExpenseValidationErrors()
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle // Per ricevere l'ID della spesa
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val expenseId: String = savedStateHandle.get<String>("expenseId")!!

    init {
        loadExpense()
    }

    private fun loadExpense() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Troviamo la spesa specifica nel repository
            val expense = expenseRepository.allExpenses.first().find { it.id == expenseId }
            if (expense != null) {
                _uiState.update { it.copy(expense = expense, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Expense not found") }
            }
        }
    }

    fun onFieldChange(notes: String, total: String, category: String, location: String) {
        val currentExpense = _uiState.value.expense ?: return

        _uiState.update {
            it.copy(
                expense = currentExpense.copy(
                    notes = notes,
                    category = category,
                    storeLocation = location
                ),
                totalInput = total,
                validationErrors = ExpenseValidationErrors()
            )
        }
    }

    // Aggiungiamo anche qui onDateChange per coerenza e pulizia
    fun onDateChange(newTimestamp: Long) {
        _uiState.value.expense?.let {
            _uiState.update {
                it.copy(
                    expense = it.expense?.copy(
                        receiptDate = Date(newTimestamp)
                    ),
                    validationErrors = it.validationErrors.copy(dateError = null) // Pulisce l'errore della data
                )
            }
        }
    }

    fun updateExpense() {
        // --- 5. ESEGUI LA VALIDAZIONE PRIMA DI SALVARE ---
        if (!validateCurrentState()) {
            // Se non è valido, interrompi l'operazione.
            // L'UI si aggiornerà per mostrare gli errori.
            _uiState.update { it.copy(isSaving = false) }
            return
        }

        viewModelScope.launch {
            _uiState.value.expense?.let { expense ->
                val updatedExpense = expense.copy(
                    totalAmount = _uiState.value.totalInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                )
                _uiState.update { it.copy(isSaving = true) }
                expenseRepository.saveExpense(expense) // saveExpense fa già un upsert
                // Lancia la sincronizzazione in background
                launch {
                    try {
                        expenseRepository.syncWithServer()
                    } catch (e: Exception) {
                        Log.e("DetailVM", "Background sync failed after update: ${e.message}")
                    }
                }
                _uiState.update { it.copy(isSaving = false, navigateBack = true) }
            }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            _uiState.value.expense?.let { expense ->
                _uiState.update { it.copy(isDeleting = true) }
                expenseRepository.deleteExpense(expense) // Fa un soft-delete
                _uiState.update { it.copy(isDeleting = false, navigateBack = true) }
            }
        }
    }

    // --- 4. AGGIUNGI LA FUNZIONE DI VALIDAZIONE ---
    private fun validateCurrentState(): Boolean {
        val expense = _uiState.value.expense
        val totalInput = _uiState.value.totalInput
        if (expense == null) return false

        val errors = ExpenseValidationErrors(
            totalError = if (totalInput.toBigDecimalOrNull() == null || totalInput.toBigDecimal() <= BigDecimal.ZERO) "Amount cannot be zero or less" else null,
            notesError = if (expense.notes.isNullOrBlank()) "Description cannot be empty" else null
            // La data viene gestita dal DatePicker, quindi è meno probabile che sia vuota, ma il controllo c'è.
        )

        _uiState.update { it.copy(validationErrors = errors) }

        // Restituisce true se non ci sono errori
        return errors.totalError == null && errors.notesError == null
    }

}