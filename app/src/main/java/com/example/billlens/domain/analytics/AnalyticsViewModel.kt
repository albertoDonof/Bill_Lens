package com.example.billlens.domain.analytics

import android.util.Log
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billlens.data.model.Expense
import com.example.billlens.data.network.GeocodingApiService
import com.example.billlens.data.repository.ExpenseRepository
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.billlens.BuildConfig
import kotlinx.coroutines.flow.filter
import kotlin.collections.get
import java.text.SimpleDateFormat
import java.util.Locale


// Enum per i filtri temporali
enum class TimeFilter(val displayName: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

// Dati aggregati per una singola categoria, pronti per la UI
data class CategorySpending(
    val categoryName: String,
    val totalAmount: BigDecimal,
    val percentage: Float // Percentuale sul totale del periodo
)

data class ExpenseLocationMarker(
    val title: String,
    val location: LatLng,
    val snippet: String
)

// NUOVA data class per lo stato della mappa
data class MapUiState(
    val markers: List<ExpenseLocationMarker> = emptyList(),
    val isGeocoding: Boolean = false
)

data class MonthlySpending(
    val year: Int,
    val month: Int, // Manteniamo il mese come numero (0-11) per l'ordinamento
    val monthLabel: String, // Etichetta da mostrare (es. "Gen", "Feb")
    val totalAmount: BigDecimal
)

// Stato della UI per la schermata delle statistiche
data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categorySpendings: List<CategorySpending> = emptyList(),
    val monthlySpendings: List<MonthlySpending> = emptyList(),
    val isLoading: Boolean = true,
    val mapState: MapUiState = MapUiState()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val geocodingApiService: GeocodingApiService
) : ViewModel() {

    // Stato per il filtro temporale selezionato, di default "Questo Mese"
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter

    // --- MODIFICA 1: Dichiarazione di _uiState ---
    // Dichiariamo _uiState PRIMA di usarlo nel 'combine'. Lo useremo per aggiornare
    // lo stato della mappa in modo indipendente.
    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    // Questo StateFlow conterrà sempre la lista più aggiornata di TUTTE le spese da Room.
    private val allExpensesFlow: StateFlow<List<Expense>> = expenseRepository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Avviamo un osservatore che reagisce ai cambiamenti delle spese o dei filtri
        // e aggiorna la parte "statistiche" della UI.
        viewModelScope.launch {
            combine(
                allExpensesFlow,
                _selectedTimeFilter
            ) { allExpenses, filter ->
                val filteredExpenses = filterExpensesByTime(allExpenses, filter)
                val totalSpending = filteredExpenses.sumOf { it.totalAmount }

                val categorySpendings = if (totalSpending > BigDecimal.ZERO) {
                    filteredExpenses
                        .groupBy { it.category }
                        .map { (category, expensesInCategory) ->
                            val categoryTotal = expensesInCategory.sumOf { it.totalAmount }
                            CategorySpending(
                                categoryName = category,
                                totalAmount = categoryTotal,
                                percentage = (categoryTotal.toFloat() / totalSpending.toFloat()) * 100
                            )
                        }
                        .sortedByDescending { it.totalAmount }
                } else {
                    emptyList()
                }

                // --- NUOVA LOGICA PER IL GRAFICO A BARRE "SPENDING OVER TIME" ---
                val monthlySpendings = filteredExpenses
                    .groupBy { expense ->
                        // Raggruppiamo le spese per una coppia (Anno, Mese)
                        val cal = Calendar.getInstance().apply { time = expense.receiptDate }
                        cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                    }
                    .map { (yearMonthPair, expensesInMonth) ->
                        val (year, month) = yearMonthPair
                        MonthlySpending(
                            year = year,
                            month = month,
                            // Creiamo l'etichetta del mese (es. "Gen", "Feb")
                            monthLabel = SimpleDateFormat("MMM",
                                Locale.getDefault()).format(
                                Calendar.getInstance().apply { set(Calendar.MONTH, month) }.time
                            ),
                            totalAmount = expensesInMonth.sumOf { it.totalAmount }
                        )
                    }
                    // Ordiniamo i risultati cronologicamente
                    .sortedWith(compareBy({ it.year }, { it.month }))

                // Restituiamo un oggetto contenente i dati calcolati
                Triple(totalSpending, categorySpendings, monthlySpendings)

            }.collect { (total, categories, monthly) ->
                // --- MODIFICA 1: Usiamo 'update' per modificare lo stato in modo sicuro ---
                // In questo modo, modifichiamo solo le proprietà che ci interessano,
                // senza sovrascrivere lo stato della mappa (mapState).
                _uiState.update { currentState ->
                    currentState.copy(
                        totalSpending = total,
                        categorySpendings = categories,
                        monthlySpendings = monthly,
                        isLoading = false
                    )
                }
            }
        }
    }

    // Funzione per permettere alla UI di cambiare il filtro
    fun onTimeFilterChanged(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
    }


    // --- MODIFICA 2: Correzione della funzione 'loadHeatmapData' ---
    fun loadMarkersData() {
        if (_uiState.value.mapState.isGeocoding || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(mapState = it.mapState.copy(isGeocoding = true)) }

            // Aspetta finché la lista di spese non contiene elementi (evita di partire con lista vuota)
            val allExpenses = try {
                allExpensesFlow.first { it.isNotEmpty() }
            } catch (e: Exception) {
                Log.e("AnalyticsVM", "Timeout/wait error while waiting for expenses", e)
                _uiState.update { it.copy(mapState = it.mapState.copy(isGeocoding = false, markers = emptyList())) }
                return@launch
            }

            // Se la lista è vuota, non c'è nulla da geocodificare.
            if (allExpenses.isEmpty()) {
                Log.d("AnalyticsVM", "Expense list is empty. No markers to load.")
                _uiState.update { it.copy(mapState = it.mapState.copy(isGeocoding = false, markers = emptyList())) }
                return@launch
            }

            // val filteredExpenses = filterExpensesByTime(allExpenses, TimeFilter.ALL_TIME)


            // Filtra solo le spese che hanno un indirizzo valido
            val expensesWithLocation = allExpenses
                .filter { !it.storeLocation.isNullOrBlank() }

            val markers = mutableListOf<ExpenseLocationMarker>()

            // Esegui le chiamate di geocoding in parallelo per massima efficienza
            expensesWithLocation.map { expense ->
                async {
                    try {
                        val query = java.net.URLEncoder.encode(expense.storeLocation!!, "UTF-8")
                        Log.d("AnalyticsVM", "Geocoding -> '$query' (original: '${expense.storeLocation}')")
                        val response = geocodingApiService.geokeoAddress(query, BuildConfig.GEOKEO_API_KEY)
                        Log.d("AnalyticsVM", "Geocoding response for '${expense.storeLocation}': code=${response.code()}")
                        if (response.isSuccessful) {
                            val body = response.body()
                            Log.d("AnalyticsVM", "Response body: ${body?.results?.size ?: 0} results")
                            if (body?.results?.isNotEmpty() == true) {
                                val location = body.results[0].geometry.location
                                ExpenseLocationMarker(
                                    location = LatLng(location.latitude, location.longitude),
                                    title = expense.notes ?: "Expense",
                                    snippet = "Amount: €${expense.totalAmount}"
                                )
                            } else {
                                null
                            }
                        } else{
                            Log.e("AnalyticsVM", "Geocoding failed HTTP ${response.code()} for '${expense.storeLocation}'")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("AnalyticsVM", "Geocoding failed for '${expense.storeLocation}'", e)
                        null
                    }
                }
            }.forEach { deferred ->
                deferred.await()?.let { marker ->
                    Log.d("AnalyticsVM", "Marker added -> Title: ${marker.title}, Location: ${marker.location}")
                    markers.add(marker)
                }
            }

            // Aggiorna lo stato della UI con la nuova lista di marker
            _uiState.update { currentState ->

                Log.d("AnalyticsVM", "Finished geocoding. Total markers found: ${markers.size}")
                currentState.copy(mapState = currentState.mapState.copy(isGeocoding = false, markers = markers))
            }
        }
    }

    // Funzione helper per filtrare le spese in base al periodo selezionato
    private fun filterExpensesByTime(expenses: List<Expense>, filter: TimeFilter): List<Expense> {
        val now = Calendar.getInstance()
        return when (filter) {
            TimeFilter.THIS_MONTH -> {
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)
                expenses.filter {
                    val cal = Calendar.getInstance().apply { time = it.receiptDate }
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }

            TimeFilter.LAST_MONTH -> {
                val lastMonthCalendar = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                val lastMonth = lastMonthCalendar.get(Calendar.MONTH)
                val year = lastMonthCalendar.get(Calendar.YEAR)
                expenses.filter {
                    val cal = Calendar.getInstance().apply { time = it.receiptDate }
                    cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == year
                }
            }

            TimeFilter.THIS_YEAR -> {
                val currentYear = now.get(Calendar.YEAR)
                expenses.filter {
                    val cal = Calendar.getInstance().apply { time = it.receiptDate }
                    cal.get(Calendar.YEAR) == currentYear
                }
            }

            TimeFilter.ALL_TIME -> expenses
        }
    }

}