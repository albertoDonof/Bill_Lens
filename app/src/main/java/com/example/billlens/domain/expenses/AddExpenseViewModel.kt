package com.example.billlens.domain.expenses

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.material3.rememberTopAppBarState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.billlens.data.model.Expense
import com.example.billlens.ui.scan.ReceiptDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class AddExpenseUiState(
    val store: String? = null,
    val location : String? = null,
    val date: String? = null,
    val total: String? = null,
    val notes: String? = null,
    val category: String = ExpenseCategory.OTHER.displayName,
    val availableCategories: List<String> = ExpenseCategory.entries.map { it.displayName },
    val imageBitmap: android.graphics.Bitmap? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)


@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun prefillFromExtracted(details: ReceiptDetails, bitmap: Bitmap?) {

        val suggestedCategory = ExpenseCategory.categorize(details.fullText).displayName
        val defaultNote = details.storeName ?: ""

        _uiState.update { it.copy(
            store = details.storeName,
            location = details.storeLocation,
            date = details.date,
            total = details.total,
            notes = defaultNote,
            category = suggestedCategory,
            imageBitmap = bitmap,
            errorMessage = null,
            saved = false
        ) }
    }

    fun updateCategory(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun updateFields(store: String?,location: String?, date: String?, total: String?, notes: String?) {
        _uiState.value = _uiState.value.copy(store = store, location = location, date = date, total = total, notes = notes)
    }

    fun saveExpense() {

        // 1. Cambiamo lo stato IMMEDIATAMENTE
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {

                val expense = withContext(Dispatchers.Default) {
                    val state = _uiState.value
                    /*
                    val imageBytes = state.imageBitmap?.let { bitmap ->
                        // semplice conversione PNG, adattare se serve compressione diversa
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
                        stream.toByteArray()
                    } */

                    // Parse total in BigDecimal
                    val totalAmount = try {
                        if (!state.total.isNullOrBlank()) BigDecimal(state.total) else BigDecimal.ZERO
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }
                    val location = state.location ?: ""
                    val store = state.store ?: ""


                    // Parse date (support common formats); fallback a oggi
                    val receiptDate: Date = parseDateOrNow(state.date)

                    // Map: store -> notes (perché il modello Expense non ha store), category default
                    val category = state.category
                    val notes = state.notes ?: store

                    val now = Date()
                    Expense(
                        totalAmount = totalAmount,
                        receiptDate = receiptDate,
                        category = category,
                        storeLocation = location,
                        notes = notes,
                        insertionDate = now,
                        lastUpdated = now,
                        isDeleted = false,
                        isSynced = false
                    )
                }

                expenseRepository.saveExpense(expense)
                _uiState.update { it.copy(isSaving = false, saved = true) }
                _events.send(AddExpenseEvent.Saved)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, errorMessage = t.message)}
                _events.send(AddExpenseEvent.Error(t.message ?: "Error"))
            }
        }
    }

    private fun parseDateOrNow(dateStr: String?): Date {
        if (dateStr.isNullOrBlank()) return Date()
        val fourDigitYearFormats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyyMMdd" // A volte si trovano anche senza separatori
        )

// 2. Formati con anno a 2 cifre (priorità più bassa, gestiti con attenzione)
        val twoDigitYearFormats = listOf(
            "dd/MM/yy",
            "dd-MM-yy",
            "MM/dd/yy"
        )

        var parsedLocalDate: LocalDate? = null

        for (fmt in fourDigitYearFormats) {
            try {
                // Usiamo java.time.LocalDate che è più moderno e robusto di SimpleDateFormat
                val formatter = DateTimeFormatter.ofPattern(fmt, Locale.getDefault())
                parsedLocalDate = LocalDate.parse(dateStr, formatter)
                // Convertiamo LocalDate in java.util.Date
                break
            } catch (e: DateTimeParseException) {
                // Continua con il prossimo formato
            }
        }

// 4. Se falliscono i 4 cifre, prova i formati a 2 cifre con gestione del secolo
        for (fmt in twoDigitYearFormats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(fmt, Locale.getDefault())
                // Il parsing dell'anno a due cifre da parte di DateTimeFormatter è più intelligente di SimpleDateFormat.
                // Per esempio, se è il 2024, "26" verrà interpretato come 2026.
                parsedLocalDate = LocalDate.parse(dateStr, formatter)
                break
            } catch (e: DateTimeParseException) {
                // Continua con il prossimo formato
            }
        }

        parsedLocalDate?.let { date ->
            val currentYear = LocalDate.now().year
            if (date.year > currentYear + 1) { // Tolleranza di 1 anno (per sicurezza, non si sa mai)
                Log.w("AddExpenseViewModel", "Data con anno futuro ('${date.year}') rilevata. Imposto l'anno corrente.")
                // Ritorna una nuova LocalDate con l'anno corrente, mantenendo mese e giorno
                return Date.from(date.withYear(currentYear).atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        }

// 3. Fallback: Se il parsing non ha avuto successo o l'anno era ok, converte la data trovata o usa la corrente
        return parsedLocalDate?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: run{
            Date()
        }
    }

    sealed class AddExpenseEvent {
        object Saved : AddExpenseEvent()
        data class Error(val message: String) : AddExpenseEvent()
    }
}
